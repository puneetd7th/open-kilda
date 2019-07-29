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

import static java.util.Collections.emptyList;

import org.openkilda.model.Switch;
import org.openkilda.model.Switch.SwitchData;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowFrame;
import org.openkilda.persistence.ferma.frames.SwitchFrame;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;
import org.openkilda.persistence.repositories.SwitchRepository;

import com.syncleus.ferma.ElementFrame;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ferma (Tinkerpop) implementation of {@link SwitchRepository}.
 */
class FermaSwitchRepository extends FermaGenericRepository<Switch, SwitchData> implements SwitchRepository {
    FermaSwitchRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Collection<Switch> findAll() {
        return framedGraph().traverse(g -> g.V().hasLabel(SwitchFrame.FRAME_LABEL))
                .toListExplicit(SwitchFrame.class).stream()
                .map(Switch::new).collect(Collectors.toList());
    }

    @Override
    public boolean exists(SwitchId switchId) {
        return (Long) framedGraph().traverse(g -> g.V().hasLabel(SwitchFrame.FRAME_LABEL)
                .has(SwitchFrame.SWITCH_ID_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId)).count())
                .getRawTraversal().next() > 0;
    }

    @Override
    public Optional<Switch> findById(SwitchId switchId) {
        return SwitchFrame.load(framedGraph(), switchId).map(Switch::new);
    }

    @Override
    public Collection<Switch> findSwitchesInFlowPathByFlowId(String flowId) {
        FlowFrame flowFrame = framedGraph().traverse(g -> g.V().hasLabel(FlowFrame.FRAME_LABEL)
                .has(FlowFrame.FLOW_ID_PROPERTY, flowId))
                .nextOrDefaultExplicit(FlowFrame.class, null);
        if (flowFrame == null) {
            return emptyList();
        }
        Map<SwitchId, Switch> result = new HashMap<>();
        Stream.of(flowFrame.getSrcSwitch(), flowFrame.getDestSwitch())
                .forEach(sw -> result.put(sw.getSwitchId(), sw));
        flowFrame.getPaths().forEach(flowPath -> {
            flowPath.getSegments().forEach(pathSegment -> {
                Stream.of(pathSegment.getSrcSwitch(), pathSegment.getDestSwitch())
                        .forEach(sw -> result.put(sw.getSwitchId(), sw));
            });
        });
        return result.values();
    }

    @Override
    @Deprecated
    public Switch reload(Switch entity) {
        return entity;
        /* TODO: remove it
        if (entity.getData() instanceof SwitchFrame) {
            return entity;
        }
        return findById(entity.getSwitchId())
                .orElseThrow(() -> new PersistenceException(format("Switch not found: %s", entity.getSwitchId())));
         */
    }

    @Override
    @Deprecated
    public void lockSwitches(Switch... switches) {
        // TODO: remove it
    }

    @Override
    public boolean removeIfNoDependant(Switch entity) {
        SwitchData data = entity.getData();
        if (data instanceof SwitchFrame) {
            return transactionManager.doInTransaction(() -> {
                if (!((SwitchFrame) data).getElement().edges(Direction.BOTH).hasNext()) {
                    ((SwitchFrame) data).remove();
                    return true;
                }
                return false;
            });
        } else {
            throw new IllegalArgumentException("Can't delete object " + entity + " which is not framed graph element");
        }
    }

    @Override
    public Switch add(Switch entity) {
        SwitchData data = entity.getData();
        if (data instanceof ElementFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() -> new Switch(SwitchFrame.create(framedGraph(), data)));
    }

    static GraphTraversal<Vertex, Vertex> getTraverseForSwitch(GraphTraversalSource input, SwitchId switchId) {
        return input.V().hasLabel(SwitchFrame.FRAME_LABEL)
                .has(SwitchFrame.SWITCH_ID_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId));
    }
}
