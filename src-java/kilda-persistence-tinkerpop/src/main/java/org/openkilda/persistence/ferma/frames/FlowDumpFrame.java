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

import org.openkilda.model.Cookie;
import org.openkilda.model.FlowPathStatus;
import org.openkilda.model.MeterId;
import org.openkilda.model.SwitchId;
import org.openkilda.model.history.FlowDump;
import org.openkilda.model.history.FlowDump.FlowDumpData;
import org.openkilda.model.history.FlowEvent;
import org.openkilda.persistence.ferma.frames.converters.CookieConverter;
import org.openkilda.persistence.ferma.frames.converters.FlowPathStatusConverter;
import org.openkilda.persistence.ferma.frames.converters.MeterIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Property;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;

import java.util.Optional;

public abstract class FlowDumpFrame extends KildaBaseVertexFrame implements FlowDumpData {
    public static final String FRAME_LABEL = "flow_dump";
    public static final String TASK_ID_PROPERTY = "task_id";
    public static final String STATE_LOG_EDGE = "state_log";

    @Override
    @Property(TASK_ID_PROPERTY)
    public abstract String getTaskId();

    @Override
    @Property(TASK_ID_PROPERTY)
    public abstract void setTaskId(String taskId);

    @Override
    @Property("flow_id")
    public abstract String getFlowId();

    @Override
    @Property("flow_id")
    public abstract void setFlowId(String flowId);

    @Override
    @Property("type")
    public abstract String getType();

    @Override
    @Property("type")
    public abstract void setType(String type);

    @Override
    @Property("bandwidth")
    public abstract long getBandwidth();

    @Override
    @Property("bandwidth")
    public abstract void setBandwidth(long bandwidth);

    @Override
    @Property("ignore_bandwidth")
    public abstract boolean isIgnoreBandwidth();

    @Override
    @Property("ignore_bandwidth")
    public abstract void setIgnoreBandwidth(boolean ignoreBandwidth);

    @Override
    public Cookie getForwardCookie() {
        return CookieConverter.INSTANCE.map((Long) getProperty("forward_cookie"));
    }

    @Override
    public void setForwardCookie(Cookie forwardCookie) {
        setProperty("forward_cookie", CookieConverter.INSTANCE.map(forwardCookie));
    }

    @Override
    public Cookie getReverseCookie() {
        return CookieConverter.INSTANCE.map((Long) getProperty("reverse_cookie"));
    }

    @Override
    public void setReverseCookie(Cookie reverseCookie) {
        setProperty("reverse_cookie", CookieConverter.INSTANCE.map(reverseCookie));
    }

    @Override
    public SwitchId getSourceSwitch() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty("source_switch"));
    }

    @Override
    public void setSourceSwitch(SwitchId sourceSwitch) {
        setProperty("source_switch", SwitchIdConverter.INSTANCE.map(sourceSwitch));
    }

    @Override
    public SwitchId getDestinationSwitch() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty("destination_switch"));
    }

    @Override
    public void setDestinationSwitch(SwitchId destinationSwitch) {
        setProperty("destination_switch", SwitchIdConverter.INSTANCE.map(destinationSwitch));
    }

    @Override
    @Property("source_port")
    public abstract int getSourcePort();

    @Override
    @Property("source_port")
    public abstract void setSourcePort(int sourcePort);

    @Override
    @Property("destination_port")
    public abstract int getDestinationPort();

    @Override
    @Property("destination_port")
    public abstract void setDestinationPort(int destinationPort);

    @Override
    @Property("source_vlan")
    public abstract int getSourceVlan();

    @Override
    @Property("source_vlan")
    public abstract void setSourceVlan(int sourceVlan);

    @Override
    @Property("destination_vlan")
    public abstract int getDestinationVlan();

    @Override
    @Property("destination_vlan")
    public abstract void setDestinationVlan(int destinationVlan);

    @Override
    public MeterId getForwardMeterId() {
        return MeterIdConverter.INSTANCE.map((Long) getProperty("forward_meter_id"));
    }

    @Override
    public void setForwardMeterId(MeterId forwardMeterId) {
        setProperty("forward_meter_id", MeterIdConverter.INSTANCE.map(forwardMeterId));
    }

    @Override
    public MeterId getReverseMeterId() {
        return MeterIdConverter.INSTANCE.map((Long) getProperty("reverse_meter_id"));
    }

    @Override
    public void setReverseMeterId(MeterId reverseMeterId) {
        setProperty("reverse_meter_id", MeterIdConverter.INSTANCE.map(reverseMeterId));
    }

    @Override
    @Property("forward_path")
    public abstract String getForwardPath();

    @Override
    @Property("forward_path")
    public abstract void setForwardPath(String forwardPath);

    @Override
    @Property("reverse_path")
    public abstract String getReversePath();

    @Override
    @Property("reverse_path")
    public abstract void setReversePath(String reversePath);

    @Override
    public FlowPathStatus getForwardStatus() {
        return FlowPathStatusConverter.INSTANCE.map((String) getProperty("forward_status"));
    }

    @Override
    public void setForwardStatus(FlowPathStatus forwardStatus) {
        setProperty("forward_status", FlowPathStatusConverter.INSTANCE.map(forwardStatus));
    }

    @Override
    public FlowPathStatus getReverseStatus() {
        return FlowPathStatusConverter.INSTANCE.map((String) getProperty("reverse_status"));
    }

    @Override
    public void setReverseStatus(FlowPathStatus reverseStatus) {
        setProperty("reverse_status", FlowPathStatusConverter.INSTANCE.map(reverseStatus));
    }

    @Override
    public FlowEvent getFlowEvent() {
        return Optional.ofNullable(traverse(v -> v.in(STATE_LOG_EDGE)
                .hasLabel(FlowEventFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(FlowEventFrame.class, null))
                .map(FlowEvent::new)
                .orElse(null);
    }

    @Override
    public void setFlowEvent(FlowEvent flowEvent) {
        getElement().edges(Direction.IN, STATE_LOG_EDGE).forEachRemaining(Element::remove);

        FlowEvent.FlowEventData data = flowEvent.getData();
        if (data instanceof FlowEventFrame) {
            linkIn((VertexFrame) data, STATE_LOG_EDGE);
        } else {
            throw new IllegalArgumentException("Unable to link to transient flow event " + flowEvent);
        }
    }

    public static FlowDumpFrame create(FramedGraph framedGraph, FlowDumpData data) {
        FlowDumpFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                FlowDumpFrame.class);
        FlowDump.FlowDumpCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
