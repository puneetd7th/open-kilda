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

import org.openkilda.floodlight.api.request.TransitFlowSegmentRequest;
import org.openkilda.floodlight.flow.response.FlowRuleResponse;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.wfm.topology.flowhs.fsm.common.action.FlowProcessingAction;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.State;
import org.openkilda.wfm.topology.flowhs.validation.rules.NonIngressRulesValidator;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ValidateNonIngressRuleAction extends FlowProcessingAction<FlowCreateFsm, State, Event, FlowCreateContext> {

    public ValidateNonIngressRuleAction(PersistenceManager persistenceManager) {
        super(persistenceManager);
    }

    @Override
    protected void perform(State from, State to, Event event, FlowCreateContext context, FlowCreateFsm stateMachine) {
        UUID commandId = context.getActModResponse().getCommandId();

        TransitFlowSegmentRequest expected = stateMachine.getNonIngressCommands().get(commandId);
        FlowRuleResponse actual = (FlowRuleResponse) context.getActModResponse();
        if (!new NonIngressRulesValidator(expected, actual).validate()) {
            stateMachine.getFailedCommands().add(commandId);
        } else {
            saveHistory(stateMachine, expected);
            stateMachine.getPendingCommands().remove(commandId);
            if (stateMachine.getPendingCommands().isEmpty()) {
                log.debug("Non ingress rules have been validated for flow {}", stateMachine.getFlowId());
                stateMachine.fire(Event.NEXT);
            }
        }
    }

    private void saveHistory(FlowCreateFsm stateMachine, TransitFlowSegmentRequest expected) {
        String action = format("Rule is valid: switch %s, cookie %s",
                expected.getSwitchId().toString(), expected.getCookie());
        String description = format("Non ingress rule has been validated successfully: switch %s, cookie %s",
                expected.getSwitchId().toString(), expected.getCookie());
        saveHistory(stateMachine, stateMachine.getCarrier(), stateMachine.getFlowId(), action, description);
    }
}
