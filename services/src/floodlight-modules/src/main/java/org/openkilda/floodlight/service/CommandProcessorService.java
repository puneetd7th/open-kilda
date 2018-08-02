/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.floodlight.service;

import org.openkilda.floodlight.command.Command;
import org.openkilda.floodlight.command.CommandContext;
import org.openkilda.floodlight.command.PendingCommandSubmitter;
import org.openkilda.floodlight.kafka.KafkaConsumerConfig;
import org.openkilda.floodlight.utils.CommandContextFactory;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CommandProcessorService implements IFloodlightService {
    private static final Logger log = LoggerFactory.getLogger(CommandProcessorService.class);

    private static final int FUTURE_COMPLETE_CHECK_INTERVAL = 200;
    private static final long REJECTED_REPORT_INTERVAL = 1000;

    private final CommandContextFactory commandContextFactory;

    private ThreadPoolExecutor executor;

    private LinkedList<ProcessorTask> tasks = new LinkedList<>();
    private final LinkedList<Runnable> rejectedQueue = new LinkedList<>();
    private long lastRejectCountReportedAt = 0;

    public CommandProcessorService(CommandContextFactory commandContextFactory) {
        this.commandContextFactory = commandContextFactory;
    }

    /**
     * Service initialize(late) method.
     */
    public void init(FloodlightModuleContext moduleContext) {
        KafkaConsumerConfig config = moduleContext.getServiceImpl(ConfigService.class).getConsumerConfig();

        String name = getClass().getCanonicalName();
        log.info("{} config - persistent workers - {}", name, config.getCommandPersistentWorkersCount());
        log.info("{} config - workers limit - {}", name, config.getCommandWorkersLimit());
        log.info("{} config - idle workers keep alive seconds - {}",
                name, config.getCommandIdleWorkersKeepAliveSeconds());
        log.info("{} config - deferred requests limit - {}", name, config.getCommandDeferredRequestsLimit());

        executor = new ThreadPoolExecutor(
                config.getCommandPersistentWorkersCount(), config.getCommandWorkersLimit(),
                config.getCommandIdleWorkersKeepAliveSeconds(), TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(config.getCommandDeferredRequestsLimit()),
                new RejectedExecutor(this));
        executor.prestartAllCoreThreads();

        scheduleFutureCheckTrigger(moduleContext.getServiceImpl(IThreadPoolService.class).getScheduledExecutor());
    }

    public void process(Command command) {
        processLazy(command);
        verifyPendingStatus();
    }

    /**
     * Execute command and initiate completion check.
     */
    public void process(List<Command> commands) {
        for (Command entry : commands) {
            this.processLazy(entry);
        }
        verifyPendingStatus();
    }

    /**
     * Execute command without intermediate completion check.
     */
    public void processLazy(Command command) {
        Future<Command> successor = executor.submit(command);
        synchronized (this) {
            tasks.addLast(new ProcessorTask(command, successor));
        }
    }

    /**
     * Submit pending command.
     *
     * <p>Initiator will receive exception returned by future object (if it will raise one). I.e. this interface
     * allow to wait for some background task to complete, without occupy any working thread.
     */
    public synchronized void submitPending(Command initiator, Future<Command> successor) {
        tasks.add(new ProcessorTask(initiator, successor));
    }

    public synchronized void markCompleted(ProcessorTask task) { }

    private synchronized void reSubmitPending(List<ProcessorTask> pending) {
        tasks.addAll(pending);
    }

    private void handleExecutorReject(Runnable command) {
        synchronized (rejectedQueue) {
            rejectedQueue.addLast(command);
        }
    }

    private void timerTrigger() {
        pushRejected();
        verifyPendingStatus();
    }

    private void pushRejected() {
        if (executor.isShutdown()) {
            return;
        }

        BlockingQueue<Runnable> queue = executor.getQueue();
        int count;
        synchronized (rejectedQueue) {
            while (!rejectedQueue.isEmpty()) {
                Runnable entry = rejectedQueue.getFirst();
                if (queue.offer(entry)) {
                    rejectedQueue.removeFirst();
                    continue;
                }

                break;
            }
            count = rejectedQueue.size();
        }

        if (0 < count) {
            long now = System.currentTimeMillis();
            if (lastRejectCountReportedAt + REJECTED_REPORT_INTERVAL < now) {
                lastRejectCountReportedAt = now;
                log.warn("Rejected commands queue size: {}", count);
            }
        } else if (0 < lastRejectCountReportedAt) {
            log.warn("All rejected command have been submitted into executor");
            lastRejectCountReportedAt = 0;
        }
    }

    private void verifyPendingStatus() {
        LinkedList<ProcessorTask> checkList = rotatePendingCommands();
        if (checkList.size() == 0) {
            return;
        }

        try {
            CommandContext context = commandContextFactory.produce();
            VerifyBatch verifyBatch = new VerifyBatch(this, checkList);
            PendingCommandSubmitter checkCommands = new PendingCommandSubmitter(context, verifyBatch);
            processLazy(checkCommands);
        } catch (Throwable e) {
            synchronized (this) {
                tasks.addAll(checkList);
            }
            throw e;
        }
    }

    private synchronized LinkedList<ProcessorTask> rotatePendingCommands() {
        LinkedList<ProcessorTask> current = tasks;
        tasks = new LinkedList<>();
        return current;
    }

    private void scheduleFutureCheckTrigger(ScheduledExecutorService scheduler) {
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                timerTrigger();
            }
        }, FUTURE_COMPLETE_CHECK_INTERVAL, FUTURE_COMPLETE_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public static class VerifyBatch implements AutoCloseable {
        private CommandProcessorService commandProcessor;
        private final LinkedList<ProcessorTask> tasksBatch;

        VerifyBatch(CommandProcessorService commandProcessor, LinkedList<ProcessorTask> tasksBatch) {
            this.commandProcessor = commandProcessor;
            this.tasksBatch = tasksBatch;
        }

        @Override
        public void close() throws Exception {
            commandProcessor.reSubmitPending(tasksBatch);
        }

        public CommandProcessorService getCommandProcessor() {
            return commandProcessor;
        }

        public List<ProcessorTask> getTasksBatch() {
            return tasksBatch;
        }
    }

    public static class ProcessorTask {
        public final Command initiator;
        public final Future<Command> pendingSuccessor;

        public ProcessorTask(Command initiator, Future<Command> pendingSuccessor) {
            this.initiator = initiator;
            this.pendingSuccessor = pendingSuccessor;
        }
    }

    private static class RejectedExecutor implements RejectedExecutionHandler {
        private final CommandProcessorService commandProcessor;

        public RejectedExecutor(CommandProcessorService commandProcessor) {
            this.commandProcessor = commandProcessor;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            commandProcessor.handleExecutorReject(r);
        }
    }
}
