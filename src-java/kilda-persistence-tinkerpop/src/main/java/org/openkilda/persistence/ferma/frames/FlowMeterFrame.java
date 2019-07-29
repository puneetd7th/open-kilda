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

package org.openkilda.persistence.ferma.frames;

import org.openkilda.model.FlowMeter;
import org.openkilda.model.FlowMeter.FlowMeterData;
import org.openkilda.model.MeterId;
import org.openkilda.model.PathId;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ConstraintViolationException;
import org.openkilda.persistence.ferma.frames.converters.MeterIdConverter;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;

public abstract class FlowMeterFrame extends KildaBaseVertexFrame implements FlowMeterData {
    public static final String FRAME_LABEL = "flow_meter";
    public static final String PATH_ID_PROPERTY = "path_id";
    public static final String METER_ID_PROPERTY = "meter_id";
    public static final String FLOW_ID_PROPERTY = "flow_id";
    public static final String SWITCH_PROPERTY = "switch_id";

    @Override
    public SwitchId getSwitchId() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty(SWITCH_PROPERTY));
    }

    @Override
    public void setSwitchId(SwitchId switchId) {
        setProperty(SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId));
    }

    @Override
    public MeterId getMeterId() {
        return MeterIdConverter.INSTANCE.map((Long) getProperty(METER_ID_PROPERTY));
    }

    @Override
    public void setMeterId(MeterId meterId) {
        setProperty(METER_ID_PROPERTY, MeterIdConverter.INSTANCE.map(meterId));
    }

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract String getFlowId();

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract void setFlowId(String flowId);

    @Override
    public PathId getPathId() {
        return PathIdConverter.INSTANCE.map((String) getProperty(PATH_ID_PROPERTY));
    }

    @Override
    public void setPathId(PathId pathId) {
        setProperty(PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId));
    }

    public static FlowMeterFrame create(FramedGraph framedGraph, FlowMeterData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL)
                .has(SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(data.getSwitchId()))
                .has(METER_ID_PROPERTY, data.getMeterId()).count()).getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a vertex with duplicated "
                    + METER_ID_PROPERTY);
        }

        FlowMeterFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                FlowMeterFrame.class);
        FlowMeter.FlowMeterCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
