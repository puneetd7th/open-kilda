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

import static java.lang.String.format;

import org.openkilda.floodlight.api.response.SpeakerFlowSegmentResponse;
import org.openkilda.floodlight.flow.response.FlowErrorResponse;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnReceivedInstallResponseAction extends OnReceivedResponseAction {
    private final MeterRegistry meterRegistry;

    public OnReceivedInstallResponseAction(PersistenceManager persistenceManager, MeterRegistry meterRegistry) {
        super(persistenceManager);
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void handleResponse(FlowCreateFsm stateMachine, SpeakerFlowSegmentResponse response) {
        if (response.isSuccess()) {
            stateMachine.saveActionToHistory("Rule was installed",
                    format("The rule was installed: switch %s, cookie %s",
                            response.getSwitchId(), response.getCookie()));
            meterRegistry.counter("fsm.install_rule.success", "flow_id",
                    stateMachine.getFlowId()).increment();
        } else {
            handleError(stateMachine, response);
        }
    }

    @Override
    protected void onComplete(FlowCreateFsm stateMachine, FlowCreateContext context) {
        if (stateMachine.getFailedCommands().isEmpty()) {
            log.debug("Received responses for all pending commands of the flow {} ({})",
                    stateMachine.getFlowId(), stateMachine.getCurrentState());
            stateMachine.fireNext(context);
        } else {
            String errorMessage = format("Received error response(s) for %d commands (%s)",
                    stateMachine.getFailedCommands().size(), stateMachine.getCurrentState());
            stateMachine.getFailedCommands().clear();
            stateMachine.saveErrorToHistory(errorMessage);
            stateMachine.fireError(errorMessage);
        }
    }

    private void handleError(FlowCreateFsm stateMachine, SpeakerFlowSegmentResponse response) {
        FlowErrorResponse errorResponse = (FlowErrorResponse) response;
        stateMachine.getFailedCommands().add(response.getCommandId());
        stateMachine.saveErrorToHistory("Failed to install rule",
                format("Failed to install the rule: switch %s, cookie %s. Error: %s",
                        errorResponse.getSwitchId(), response.getCookie(), errorResponse));
        meterRegistry.counter("fsm.install_rule.failed", "flow_id",
                stateMachine.getFlowId()).increment();
    }
}
