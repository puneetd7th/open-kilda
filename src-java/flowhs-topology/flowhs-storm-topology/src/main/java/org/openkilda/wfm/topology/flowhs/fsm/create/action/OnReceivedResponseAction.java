/* Copyright 2019 Telstra Open Source
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

package org.openkilda.wfm.topology.flowhs.fsm.create.action;

import org.openkilda.floodlight.api.request.factory.FlowSegmentRequestFactory;
import org.openkilda.floodlight.api.response.SpeakerFlowSegmentResponse;
import org.openkilda.floodlight.flow.response.FlowErrorResponse;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.FlowProcessingAction;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.State;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
abstract class OnReceivedResponseAction extends FlowProcessingAction<FlowCreateFsm, State, Event, FlowCreateContext> {
    private final int speakerCommandRetriesLimit;

    OnReceivedResponseAction(PersistenceManager persistenceManager, int speakerCommandRetriesLimit) {
        super(persistenceManager);
        this.speakerCommandRetriesLimit = speakerCommandRetriesLimit;
    }

    @Override
    protected void perform(State from, State to, Event event, FlowCreateContext context, FlowCreateFsm stateMachine) {
        SpeakerFlowSegmentResponse response = context.getSpeakerFlowResponse();

        if (response.getRequestCreateTime() > 0) {
            Duration abs = Duration.between(Instant.ofEpochMilli(response.getRequestCreateTime()),
                    Instant.now()).abs();
            stateMachine.getMeterRegistry().timer("fsm.command.roundtrip",
                    "flow_id", stateMachine.getFlowId())
                    .record(abs.toNanos(), TimeUnit.NANOSECONDS);
            log.error("Execution time {}", abs);
        }

        if (response.getResponseCreateTime() > 0) {
            Duration abs = Duration.between(Instant.ofEpochMilli(response.getResponseCreateTime()),
                    Instant.now()).abs();
            stateMachine.getMeterRegistry().timer("floodlight.command.in_transfer",
                    "flow_id", stateMachine.getFlowId())
                    .record(abs.toNanos(), TimeUnit.NANOSECONDS);
            log.error("In transfer time {}", abs);
        }

        if (response.getRouterPassTime() > 0) {
            Duration abs = Duration.between(Instant.ofEpochMilli(response.getRouterPassTime()),
                    Instant.now()).abs();
            log.error("Router-Hub transfer time {}", abs);
        }

        if (response.getWorkerPassTime() > 0) {
            Duration abs = Duration.between(Instant.ofEpochMilli(response.getWorkerPassTime()),
                    Instant.now()).abs();
            log.error("SpeakerWorker-Hub transfer time {}", abs);
        }

        if (response.getTransferTime() > 0) {
            stateMachine.getMeterRegistry().timer("floodlight.command.out_transfer",
                    "flow_id", stateMachine.getFlowId())
                    .record(response.getTransferTime(), TimeUnit.NANOSECONDS);
            log.error("Out transfer time {}", Duration.ofNanos(response.getTransferTime()));
        }

        if (response.getWaitTime() > 0) {
            stateMachine.getMeterRegistry().timer("floodlight.command.wait",
                    "flow_id", stateMachine.getFlowId())
                    .record(response.getWaitTime(), TimeUnit.NANOSECONDS);
        }

        if (response.getExecutionTime() > 0) {
            stateMachine.getMeterRegistry().timer("floodlight.command.execution",
                    "flow_id", stateMachine.getFlowId())
                    .record(response.getExecutionTime(), TimeUnit.NANOSECONDS);
        }

        UUID commandId = response.getCommandId();
        FlowSegmentRequestFactory command = stateMachine.getPendingCommands().get(commandId);
        if (command == null) {
            log.warn("Received a response for unexpected command: {}", response);
            return;
        }

        if (response.isSuccess()) {
            stateMachine.getPendingCommands().remove(commandId);

            handleSuccessResponse(stateMachine, response);
        } else {
            FlowErrorResponse errorResponse = (FlowErrorResponse) response;

            int retries = stateMachine.getRetriedCommands().getOrDefault(commandId, 0);
            if (retries < speakerCommandRetriesLimit && isRetryableError(errorResponse)) {
                stateMachine.getRetriedCommands().put(commandId, ++retries);

                handleErrorAndRetry(stateMachine, command, commandId, errorResponse, retries);
            } else {
                stateMachine.getPendingCommands().remove(commandId);
                stateMachine.getFailedCommands().add(commandId);

                handleErrorResponse(stateMachine, errorResponse);
            }
        }

        if (stateMachine.getPendingCommands().isEmpty()) {
            if (stateMachine.getFailedCommands().isEmpty()) {
                onComplete(stateMachine, context);
            } else {
                onCompleteWithErrors(stateMachine, context);
            }
        }
    }

    protected abstract void handleSuccessResponse(FlowCreateFsm stateMachine, SpeakerFlowSegmentResponse response);

    protected boolean isRetryableError(FlowErrorResponse errorResponse) {
        return true;
    }

    protected abstract void handleErrorAndRetry(FlowCreateFsm stateMachine, FlowSegmentRequestFactory command,
                                                UUID commandId, FlowErrorResponse errorResponse, int retries);

    protected abstract void handleErrorResponse(FlowCreateFsm stateMachine, FlowErrorResponse response);

    protected abstract void onComplete(FlowCreateFsm stateMachine, FlowCreateContext context);

    protected abstract void onCompleteWithErrors(FlowCreateFsm stateMachine, FlowCreateContext context);
}
