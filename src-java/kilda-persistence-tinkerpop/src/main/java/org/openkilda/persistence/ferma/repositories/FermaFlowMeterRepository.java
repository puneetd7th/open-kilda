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

import org.openkilda.model.FlowMeter;
import org.openkilda.model.FlowMeter.FlowMeterData;
import org.openkilda.model.MeterId;
import org.openkilda.model.PathId;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.PersistenceException;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowMeterFrame;
import org.openkilda.persistence.ferma.frames.converters.MeterIdConverter;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;
import org.openkilda.persistence.repositories.FlowMeterRepository;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ferma (Tinkerpop) implementation of {@link FlowMeterRepository}.
 */
class FermaFlowMeterRepository extends FermaGenericRepository<FlowMeter, FlowMeterData>
        implements FlowMeterRepository {
    FermaFlowMeterRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Collection<FlowMeter> findAll() {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowMeterFrame.FRAME_LABEL))
                .toListExplicit(FlowMeterFrame.class).stream().map(FlowMeter::new).collect(Collectors.toList());
    }

    @Override
    public Optional<FlowMeter> findLldpMeterByMeterIdSwitchIdAndFlowId(
            MeterId meterId, SwitchId switchId, String flowId) {
        return Optional.ofNullable(framedGraph().traverse(g -> g.V().hasLabel(FlowMeterFrame.FRAME_LABEL)
                .has(FlowMeterFrame.METER_ID_PROPERTY, MeterIdConverter.INSTANCE.map(meterId))
                .has(FlowMeterFrame.SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId))
                .has(FlowMeterFrame.FLOW_ID_PROPERTY, flowId))
                .nextOrDefaultExplicit(FlowMeterFrame.class, null)).map(FlowMeter::new);
    }

    @Override
    public Collection<FlowMeter> findByPathId(PathId pathId) {
        Collection<FlowMeter> meters =
                framedGraph().traverse(g -> g.V().hasLabel(FlowMeterFrame.FRAME_LABEL)
                        .has(FlowMeterFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId)))
                        .toListExplicit(FlowMeterFrame.class).stream()
                        .map(FlowMeter::new).collect(Collectors.toList());
        if (meters.size() > 2) {
            throw new PersistenceException(format("Found more that 2 Meter entity by path (%s). "
                    + " One path must have up to 2 meters: ingress meter and LLDP meter.", pathId));
        }
        return meters;
    }

    @Override
    public Optional<MeterId> findMaximumAssignedMeter(SwitchId switchId) {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowMeterFrame.FRAME_LABEL)
                .has(FlowMeterFrame.SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId))
                .values(FlowMeterFrame.METER_ID_PROPERTY).max())
                .getRawTraversal().tryNext()
                .filter(n -> !(n instanceof Double && ((Double) n).isNaN()))
                .map(l -> MeterIdConverter.INSTANCE.map((Long) l));
    }

    @Override
    public MeterId findFirstUnassignedMeter(SwitchId switchId, MeterId startMeterId) {
        String switchIdAsStr = SwitchIdConverter.INSTANCE.map(switchId);
        return framedGraph().traverse(g -> g.V().hasLabel(FlowMeterFrame.FRAME_LABEL)
                .has(FlowMeterFrame.METER_ID_PROPERTY, P.gte(MeterIdConverter.INSTANCE.map(startMeterId)))
                .has(FlowMeterFrame.SWITCH_PROPERTY, switchIdAsStr)
                .values(FlowMeterFrame.METER_ID_PROPERTY)
                .order().math("_ + 1").as("a")
                .where(__.V().hasLabel(FlowMeterFrame.FRAME_LABEL)
                        .has(FlowMeterFrame.SWITCH_PROPERTY, switchIdAsStr)
                        .values(FlowMeterFrame.METER_ID_PROPERTY)
                        .where(P.eq("a")).count().is(0))
                .select("a"))
                .getRawTraversal().tryNext()
                .map(l -> ((Double) l).longValue())
                .map(l -> MeterIdConverter.INSTANCE.map(l)).orElse(startMeterId);
    }

    @Override
    public FlowMeter add(FlowMeter entity) {
        FlowMeterData data = entity.getData();
        if (data instanceof FlowMeterFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() -> new FlowMeter(FlowMeterFrame.create(framedGraph(), data)));
    }
}
