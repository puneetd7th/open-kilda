/* Copyright 2020 Telstra Open Source
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

package org.openkilda.wfm.share.service;

import static java.lang.String.format;

import org.openkilda.adapter.FlowSideAdapter;
import org.openkilda.floodlight.api.request.factory.EgressFlowSegmentRequestFactory;
import org.openkilda.floodlight.api.request.factory.FlowSegmentRequestFactory;
import org.openkilda.floodlight.api.request.factory.IngressFlowSegmentRequestFactory;
import org.openkilda.floodlight.api.request.factory.OneSwitchFlowRequestFactory;
import org.openkilda.floodlight.api.request.factory.TransitFlowSegmentRequestFactory;
import org.openkilda.floodlight.model.FlowSegmentMetadata;
import org.openkilda.floodlight.model.RulesContext;
import org.openkilda.messaging.MessageContext;
import org.openkilda.model.Flow;
import org.openkilda.model.FlowEncapsulationType;
import org.openkilda.model.FlowPath;
import org.openkilda.model.FlowTransitEncapsulation;
import org.openkilda.model.IslEndpoint;
import org.openkilda.model.MeterConfig;
import org.openkilda.model.PathId;
import org.openkilda.model.PathSegment;
import org.openkilda.wfm.CommandContext;
import org.openkilda.wfm.share.flow.resources.EncapsulationResources;
import org.openkilda.wfm.share.flow.resources.FlowResourcesManager;
import org.openkilda.wfm.share.model.SpeakerRequestBuildContext;
import org.openkilda.wfm.share.model.SpeakerRequestBuildContext.PathContext;
import org.openkilda.wfm.topology.flowhs.service.FlowCommandBuilder;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpeakerFlowSegmentRequestBuilder implements FlowCommandBuilder {
    // In the case of a path deletion operation, it is possible that there is no encapsulation resource for that path
    // in the database. In this case, we use this stub so as not to interrupt the delete operation. After that, excess
    // rules will remain on the switches, which the operator will have to synchronize.
    private static final FlowTransitEncapsulation DELETE_ENCAPSULATION_STUB =
            new FlowTransitEncapsulation(2, FlowEncapsulationType.TRANSIT_VLAN);

    private final NoArgGenerator commandIdGenerator = Generators.timeBasedGenerator();
    private final FlowResourcesManager resourcesManager;

    public SpeakerFlowSegmentRequestBuilder(FlowResourcesManager resourcesManager) {
        this.resourcesManager = resourcesManager;
    }

    @Override
    public List<FlowSegmentRequestFactory> buildAll(CommandContext context, Flow flow, FlowPath path,
                                                    SpeakerRequestBuildContext speakerRequestBuildContext) {
        return makeRequests(context, flow, path, null, true, true, true, speakerRequestBuildContext);
    }

    @Override
    public List<FlowSegmentRequestFactory> buildAll(CommandContext context, Flow flow, FlowPath forwardPath,
                                                    FlowPath reversePath,
                                                    SpeakerRequestBuildContext speakerRequestBuildContext) {
        return makeRequests(context, flow, forwardPath, reversePath, true, true, true, speakerRequestBuildContext);
    }

    @Override
    public List<FlowSegmentRequestFactory> buildAllExceptIngress(CommandContext context, @NonNull Flow flow) {
        return buildAllExceptIngress(context, flow, flow.getForwardPath(), flow.getReversePath());
    }

    @Override
    public List<FlowSegmentRequestFactory> buildAllExceptIngress(CommandContext context, Flow flow, FlowPath path) {
        return buildAllExceptIngress(context, flow, path, null);
    }

    @Override
    public List<FlowSegmentRequestFactory> buildAllExceptIngress(
            CommandContext context, Flow flow, FlowPath path, FlowPath oppositePath) {
        return makeRequests(context, flow, path, oppositePath, false, true, true,
                SpeakerRequestBuildContext.EMPTY);
    }

    @Override
    public List<FlowSegmentRequestFactory> buildIngressOnly(
            CommandContext context, @NonNull Flow flow, SpeakerRequestBuildContext speakerRequestBuildContext) {
        return buildIngressOnly(context, flow, flow.getForwardPath(), flow.getReversePath(),
                speakerRequestBuildContext);
    }

    @Override
    public List<FlowSegmentRequestFactory> buildIngressOnly(
            CommandContext context, Flow flow, FlowPath path, FlowPath oppositePath,
            SpeakerRequestBuildContext speakerRequestBuildContext) {
        return makeRequests(context, flow, path, oppositePath, true, false, false,
                speakerRequestBuildContext);
    }

    private List<FlowSegmentRequestFactory> makeRequests(
            CommandContext context, Flow flow, FlowPath path, FlowPath oppositePath,
            boolean doIngress, boolean doTransit, boolean doEgress,
            SpeakerRequestBuildContext speakerRequestBuildContext) {
        if (path == null) {
            path = oppositePath;
            oppositePath = null;
            speakerRequestBuildContext = SpeakerRequestBuildContext.builder()
                    .forward(speakerRequestBuildContext.getReverse())
                    .reverse(speakerRequestBuildContext.getForward())
                    .build();
        }
        if (path == null) {
            throw new IllegalArgumentException("At least one flow path must be not null");
        }

        boolean isDeleteOperation = speakerRequestBuildContext.isDeleteOperation();
        FlowTransitEncapsulation encapsulation = null;
        if (!flow.isOneSwitchFlow()) {
            try {
                encapsulation = getEncapsulation(
                        flow.getEncapsulationType(), path.getPathId(),
                        oppositePath != null ? oppositePath.getPathId() : null);
            } catch (IllegalStateException e) {
                if (!isDeleteOperation) {
                    throw e;
                }
                encapsulation = DELETE_ENCAPSULATION_STUB;
            }
        }

        List<FlowSegmentRequestFactory> requests = new ArrayList<>(makePathRequests(flow, path, context, encapsulation,
                doIngress, doTransit, doEgress, createRulesContext(speakerRequestBuildContext.getForward())));
        if (oppositePath != null) {
            if (!flow.isOneSwitchFlow()) {
                try {
                    encapsulation = getEncapsulation(
                            flow.getEncapsulationType(), oppositePath.getPathId(), path.getPathId());
                } catch (IllegalStateException e) {
                    if (!isDeleteOperation) {
                        throw e;
                    }
                    encapsulation = DELETE_ENCAPSULATION_STUB;
                }
            }
            requests.addAll(makePathRequests(flow, oppositePath, context, encapsulation, doIngress, doTransit, doEgress,
                    createRulesContext(speakerRequestBuildContext.getReverse())));
        }
        return requests;
    }

    private RulesContext createRulesContext(PathContext pathContext) {
        return new RulesContext(
                pathContext.isRemoveCustomerPortRule(),
                pathContext.isRemoveCustomerPortLldpRule(),
                pathContext.isRemoveCustomerPortArpRule(),
                pathContext.isRemoveOuterVlanMatchSharedRule(),
                pathContext.isRemoveServer42InputRule(),
                pathContext.isRemoveServer42IngressRule(),
                pathContext.isRemoveServer42OuterVlanMatchSharedRule(),
                pathContext.isInstallServer42InputRule(),
                pathContext.isInstallServer42IngressRule(),
                pathContext.isInstallServer42OuterVlanMatchSharedRule(),
                pathContext.getServer42Port(),
                pathContext.getServer42MacAddress());
    }

    @SuppressWarnings("squid:S00107")
    private List<FlowSegmentRequestFactory> makePathRequests(
            @NonNull Flow flow, @NonNull FlowPath path, CommandContext context, FlowTransitEncapsulation encapsulation,
            boolean doIngress, boolean doTransit, boolean doEgress, RulesContext rulesContext) {
        final FlowSideAdapter ingressSide = FlowSideAdapter.makeIngressAdapter(flow, path);
        final FlowSideAdapter egressSide = FlowSideAdapter.makeEgressAdapter(flow, path);

        final List<FlowSegmentRequestFactory> requests = new ArrayList<>();

        PathSegment lastSegment = null;
        for (PathSegment segment : path.getSegments()) {
            if (lastSegment == null) {
                if (doIngress) {
                    requests.add(makeIngressRequest(context, path, encapsulation, ingressSide, segment, egressSide,
                            rulesContext));
                }
            } else {
                if (doTransit) {
                    requests.add(makeTransitRequest(context, path, encapsulation, lastSegment, segment));
                }
            }
            lastSegment = segment;
        }

        if (lastSegment != null) {
            if (doEgress) {
                requests.add(makeEgressRequest(context, path, encapsulation, lastSegment, egressSide, ingressSide));
            }
        } else if (doIngress) {
            // one switch flow (path without path segments)
            requests.add(makeOneSwitchRequest(context, path, ingressSide, egressSide, rulesContext));
        }

        return requests;
    }

    private FlowSegmentRequestFactory makeIngressRequest(
            CommandContext context, FlowPath path, FlowTransitEncapsulation encapsulation,
            FlowSideAdapter flowSide, PathSegment segment, FlowSideAdapter egressFlowSide,
            RulesContext rulesContext) {
        PathSegmentSide segmentSide = makePathSegmentSourceSide(segment);

        UUID commandId = commandIdGenerator.generate();
        MessageContext messageContext = new MessageContext(commandId.toString(), context.getCorrelationId());
        return IngressFlowSegmentRequestFactory.builder()
                .messageContext(messageContext)
                .metadata(makeMetadata(path, ensureEqualMultiTableFlag(
                        flowSide.isMultiTableSegment(), segmentSide.isMultiTable(),
                        String.format("First flow(id:%s, path:%s) segment and flow level multi-table flag values are "
                                              + "incompatible to each other - flow(%s) != segment(%s)",
                                      path.getFlow().getFlowId(), path.getPathId(),
                                      flowSide.isMultiTableSegment(), segmentSide.isMultiTable()))))
                .endpoint(flowSide.getEndpoint())
                .meterConfig(getMeterConfig(path))
                .egressSwitchId(egressFlowSide.getEndpoint().getSwitchId())
                .islPort(segmentSide.getEndpoint().getPortNumber())
                .encapsulation(encapsulation)
                .rulesContext(rulesContext)
                .build();
    }

    private FlowSegmentRequestFactory makeTransitRequest(
            CommandContext context, FlowPath path, FlowTransitEncapsulation encapsulation,
            PathSegment ingress, PathSegment egress) {
        final PathSegmentSide inboundSide = makePathSegmentDestSide(ingress);
        final PathSegmentSide outboundSide = makePathSegmentSourceSide(egress);

        final IslEndpoint ingressEndpoint = inboundSide.getEndpoint();
        final IslEndpoint egressEndpoint = outboundSide.getEndpoint();

        assert ingressEndpoint.getSwitchId().equals(egressEndpoint.getSwitchId())
                : "Only neighbor segments can be used for for transit segment request creation";

        UUID commandId = commandIdGenerator.generate();
        MessageContext messageContext = new MessageContext(commandId.toString(), context.getCorrelationId());
        return TransitFlowSegmentRequestFactory.builder()
                .messageContext(messageContext)
                .switchId(ingressEndpoint.getSwitchId())
                .metadata(makeMetadata(path, ensureEqualMultiTableFlag(
                        inboundSide.isMultiTable(), outboundSide.isMultiTable(),
                        String.format(
                                "Flow(id:%s, path:%s) have incompatible multi-table flags between segments %s "
                                        + "and %s", path.getFlow().getFlowId(), path.getPathId(), ingress,
                                egress))))
                .ingressIslPort(ingressEndpoint.getPortNumber())
                .egressIslPort(egressEndpoint.getPortNumber())
                .encapsulation(encapsulation)
                .build();
    }

    private FlowSegmentRequestFactory makeEgressRequest(
            CommandContext context, FlowPath path, FlowTransitEncapsulation encapsulation,
            PathSegment segment, FlowSideAdapter flowSide, FlowSideAdapter ingressFlowSide) {
        Flow flow = flowSide.getFlow();
        PathSegmentSide segmentSide = makePathSegmentDestSide(segment);

        UUID commandId = commandIdGenerator.generate();
        MessageContext messageContext = new MessageContext(commandId.toString(), context.getCorrelationId());

        return EgressFlowSegmentRequestFactory.builder()
                .messageContext(messageContext)
                .metadata(makeMetadata(path, ensureEqualMultiTableFlag(
                        segmentSide.isMultiTable(), flowSide.isMultiTableSegment(),
                        String.format("Last flow(id:%s, path:%s) segment and flow level multi-table flags value are "
                                              + "incompatible to each other - segment(%s) != flow(%s)",
                                      flow.getFlowId(), path.getPathId(), segmentSide.isMultiTable(),
                                      flowSide.isMultiTableSegment()))))
                .endpoint(flowSide.getEndpoint())
                .ingressEndpoint(ingressFlowSide.getEndpoint())
                .islPort(segmentSide.getEndpoint().getPortNumber())
                .encapsulation(encapsulation)
                .build();
    }

    private FlowSegmentRequestFactory makeOneSwitchRequest(
            CommandContext context, FlowPath path, FlowSideAdapter ingressSide, FlowSideAdapter egressSide,
            RulesContext rulesContext) {
        Flow flow = ingressSide.getFlow();

        UUID commandId = commandIdGenerator.generate();
        MessageContext messageContext = new MessageContext(commandId.toString(), context.getCorrelationId());
        return OneSwitchFlowRequestFactory.builder()
                .messageContext(messageContext)
                .metadata(makeMetadata(path, ensureEqualMultiTableFlag(
                        ingressSide.isMultiTableSegment(), egressSide.isMultiTableSegment(),
                        String.format("Flow(id:%s) have incompatible for one-switch flow per-side multi-table flags - "
                                              + "src(%s) != dst(%s)",
                                      flow.getFlowId(), flow.isSrcWithMultiTable(), flow.isDestWithMultiTable()))))
                .endpoint(ingressSide.getEndpoint())
                .meterConfig(getMeterConfig(path))
                .egressEndpoint(egressSide.getEndpoint())
                .rulesContext(rulesContext)
                .build();
    }

    private boolean ensureEqualMultiTableFlag(boolean ingress, boolean egress, String errorMessage) {
        if (ingress != egress) {
            throw new IllegalArgumentException(errorMessage);
        }
        return ingress;
    }

    private PathSegmentSide makePathSegmentSourceSide(PathSegment segment) {
        return new PathSegmentSide(
                new IslEndpoint(segment.getSrcSwitch().getSwitchId(), segment.getSrcPort()),
                segment.isSrcWithMultiTable());
    }

    private PathSegmentSide makePathSegmentDestSide(PathSegment segment) {
        return new PathSegmentSide(
                new IslEndpoint(segment.getDestSwitch().getSwitchId(), segment.getDestPort()),
                segment.isDestWithMultiTable());
    }

    private FlowSegmentMetadata makeMetadata(FlowPath path, boolean isMultitable) {
        Flow flow = path.getFlow();
        return new FlowSegmentMetadata(flow.getFlowId(), path.getCookie(), isMultitable);
    }

    @Value
    private static class PathSegmentSide {
        private final IslEndpoint endpoint;

        private boolean multiTable;
    }

    private MeterConfig getMeterConfig(FlowPath path) {
        if (path.getMeterId() == null) {
            return null;
        }
        return new MeterConfig(path.getMeterId(), path.getBandwidth());
    }

    private FlowTransitEncapsulation getEncapsulation(
            FlowEncapsulationType encapsulation, PathId pathId, PathId oppositePathId) {
        EncapsulationResources resources = resourcesManager
                .getEncapsulationResources(pathId, oppositePathId, encapsulation)
                .orElseThrow(() -> new IllegalStateException(format(
                        "No encapsulation resources found for flow path %s (opposite: %s)", pathId, oppositePathId)));
        return new FlowTransitEncapsulation(resources.getTransitEncapsulationId(), resources.getEncapsulationType());
    }
}
