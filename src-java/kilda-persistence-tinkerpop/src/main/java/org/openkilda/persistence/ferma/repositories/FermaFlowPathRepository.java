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

package org.openkilda.persistence.ferma.repositories;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import org.openkilda.model.Cookie;
import org.openkilda.model.FlowPath;
import org.openkilda.model.FlowPath.FlowPathData;
import org.openkilda.model.FlowPathStatus;
import org.openkilda.model.FlowStatus;
import org.openkilda.model.PathId;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.PersistenceException;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowFrame;
import org.openkilda.persistence.ferma.frames.FlowPathFrame;
import org.openkilda.persistence.ferma.frames.PathSegmentFrame;
import org.openkilda.persistence.ferma.frames.SwitchFrame;
import org.openkilda.persistence.ferma.frames.converters.CookieConverter;
import org.openkilda.persistence.ferma.frames.converters.FlowStatusConverter;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;
import org.openkilda.persistence.repositories.FlowPathRepository;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ferma (Tinkerpop) implementation of {@link FlowPathRepository}.
 */
class FermaFlowPathRepository extends FermaGenericRepository<FlowPath, FlowPathData>
        implements FlowPathRepository {
    FermaFlowPathRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Collection<FlowPath> findAll() {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class).stream().map(FlowPath::new).collect(Collectors.toList());
    }

    @Override
    public Optional<FlowPath> findById(PathId pathId) {
        return Optional.ofNullable(framedGraph().traverse(g -> g.V().hasLabel(FlowPathFrame.FRAME_LABEL)
                .has(FlowPathFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId)))
                .nextOrDefaultExplicit(FlowPathFrame.class, null)).map(FlowPath::new);
    }

    @Override
    public Optional<FlowPath> findByFlowIdAndCookie(String flowId, Cookie cookie) {
        List<FlowPathFrame> flowPaths = unmodifiableList(
                framedGraph().traverse(g -> g.V().hasLabel(FlowFrame.FRAME_LABEL)
                        .has(FlowFrame.FLOW_ID_PROPERTY, flowId)
                        .out(FlowFrame.OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL)
                        .has(FlowPathFrame.COOKIE_PROPERTY, CookieConverter.INSTANCE.map(cookie)))
                        .toListExplicit(FlowPathFrame.class));
        if (flowPaths.size() > 1) {
            throw new PersistenceException(format("Found more that 1 FlowPath entity by (%s, %s)", flowId, cookie));
        } else if (flowPaths.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(flowPaths.iterator().next()).map(FlowPath::new);
    }

    @Override
    public Collection<FlowPath> findByFlowId(String flowId) {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowFrame.FRAME_LABEL)
                .has(FlowFrame.FLOW_ID_PROPERTY, flowId)
                .out(FlowFrame.OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class).stream().map(FlowPath::new).collect(Collectors.toList());
    }

    @Override
    public Collection<FlowPath> findByFlowGroupId(String flowGroupId) {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowFrame.FRAME_LABEL)
                .has(FlowFrame.GROUP_ID_PROPERTY, flowGroupId)
                .out(FlowFrame.OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class).stream().map(FlowPath::new).collect(Collectors.toList());
    }

    @Override
    public Collection<PathId> findPathIdsByFlowGroupId(String flowGroupId) {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowFrame.FRAME_LABEL)
                .has(FlowFrame.GROUP_ID_PROPERTY, flowGroupId)
                .out(FlowFrame.OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL)
                .values(FlowPathFrame.PATH_ID_PROPERTY)).getRawTraversal().toStream()
                .map(pathId -> PathIdConverter.INSTANCE.map((String) pathId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FlowPath> findBySrcSwitch(SwitchId switchId, boolean includeProtected) {
        List<FlowPath> result = new ArrayList<>();
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(FlowPathFrame.SOURCE_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.add(new FlowPath(frame)));
        if (includeProtected) {
            return result;
        } else {
            return result.stream()
                    .filter(path -> !(path.isProtected() && switchId.equals(path.getSrcSwitchId())))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Collection<FlowPath> findByEndpointSwitch(SwitchId switchId, boolean includeProtected) {
        Map<PathId, FlowPath> result = new HashMap<>();
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(FlowPathFrame.SOURCE_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(FlowPathFrame.DESTINATION_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));

        if (includeProtected) {
            return result.values();
        } else {
            return result.values().stream()
                    .filter(path -> !(path.isProtected() && switchId.equals(path.getSrcSwitchId())))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Collection<FlowPath> findBySegmentSwitch(SwitchId switchId) {
        Map<PathId, FlowPath> result = new HashMap<>();
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.SOURCE_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.DESTINATION_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        return result.values();
    }

    @Override
    public Collection<FlowPath> findInactiveBySegmentSwitch(SwitchId switchId) {
        String downFlowStatus = FlowStatusConverter.INSTANCE.map(FlowStatus.DOWN);
        String degragedFlowStatus = FlowStatusConverter.INSTANCE.map(FlowStatus.DEGRADED);

        Map<PathId, FlowPath> result = new HashMap<>();
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.SOURCE_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL).as("p")
                .in(FlowFrame.OWNS_PATHS_EDGE)
                .hasLabel(FlowFrame.FRAME_LABEL)
                .or(__.has(FlowFrame.STATUS_PROPERTY, downFlowStatus),
                        __.has(FlowFrame.STATUS_PROPERTY, degragedFlowStatus))
                .select("p"))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.DESTINATION_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL).as("p")
                .in(FlowFrame.OWNS_PATHS_EDGE)
                .hasLabel(FlowFrame.FRAME_LABEL)
                .or(__.has(FlowFrame.STATUS_PROPERTY, downFlowStatus),
                        __.has(FlowFrame.STATUS_PROPERTY, degragedFlowStatus))
                .select("p"))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        return result.values();
    }

    @Override
    public Collection<FlowPath> findBySegmentSwitchWithMultiTable(SwitchId switchId, boolean multiTable) {
        Map<PathId, FlowPath> result = new HashMap<>();
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.SOURCE_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .has(PathSegmentFrame.SRC_W_MULTI_TABLE_PROPERTY, multiTable)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.DESTINATION_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .has(PathSegmentFrame.DST_W_MULTI_TABLE_PROPERTY, multiTable)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        return result.values();
    }

    @Override
    public Collection<FlowPath> findWithPathSegment(SwitchId srcSwitchId, int srcPort,
                                                    SwitchId dstSwitchId, int dstPort) {
        return framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, srcSwitchId)
                .in(PathSegmentFrame.SOURCE_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .has(PathSegmentFrame.SRC_PORT_PROPERTY, srcPort)
                .has(PathSegmentFrame.DST_PORT_PROPERTY, dstPort)
                .where(__.out(PathSegmentFrame.DESTINATION_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                        .has(SwitchFrame.SWITCH_ID_PROPERTY, SwitchIdConverter.INSTANCE.map(dstSwitchId)))
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class).stream().map(FlowPath::new).collect(Collectors.toList());
    }

    @Override
    public Collection<FlowPath> findBySegmentDestSwitch(SwitchId switchId) {
        return framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.DESTINATION_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class).stream().map(FlowPath::new).collect(Collectors.toList());
    }

    @Override
    public Collection<FlowPath> findBySegmentEndpoint(SwitchId switchId, int port) {
        Map<PathId, FlowPath> result = new HashMap<>();
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.SOURCE_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .has(PathSegmentFrame.SRC_PORT_PROPERTY, port)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                .in(PathSegmentFrame.DESTINATION_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .has(PathSegmentFrame.DST_PORT_PROPERTY, port)
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .frameExplicit(FlowPathFrame.class)
                .forEachRemaining(frame -> result.put(frame.getPathId(), new FlowPath(frame)));
        return result.values();
    }

    @Override
    public void updateStatus(PathId pathId, FlowPathStatus pathStatus) {
        transactionManager.doInTransaction(() ->
                Optional.ofNullable(framedGraph().traverse(g -> g.V().hasLabel(FlowPathFrame.FRAME_LABEL)
                        .has(FlowPathFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId)))
                        .nextOrDefaultExplicit(FlowPathFrame.class, null))
                        .ifPresent(pathFrame -> {
                            pathFrame.setStatus(pathStatus);
                        }));
    }

    @Override
    public long getUsedBandwidthBetweenEndpoints(SwitchId srcSwitchId, int srcPort, SwitchId dstSwitchId, int dstPort) {
        return framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, srcSwitchId)
                .in(PathSegmentFrame.SOURCE_EDGE)
                .hasLabel(PathSegmentFrame.FRAME_LABEL)
                .has(PathSegmentFrame.SRC_PORT_PROPERTY, srcPort)
                .has(PathSegmentFrame.DST_PORT_PROPERTY, dstPort)
                .where(__.out(PathSegmentFrame.DESTINATION_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                        .has(SwitchFrame.SWITCH_ID_PROPERTY, SwitchIdConverter.INSTANCE.map(dstSwitchId)))
                .in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL)
                .has(FlowPathFrame.IGNORE_BANDWIDTH_PROPERTY, false)
                .values(FlowPathFrame.BANDWIDTH_PROPERTY)
                .sum()).getRawTraversal().tryNext().map(l -> ((Number) l).longValue()).orElse(0L);
    }

    @Override
    @Deprecated
    public void lockInvolvedSwitches(FlowPath... flowPaths) {
        // TODO: remove it
    }

    @Override
    public FlowPath add(FlowPath entity) {
        FlowPathData data = entity.getData();
        if (data instanceof FlowPathFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() -> new FlowPath(FlowPathFrame.create(framedGraph(), data)));
    }
}
