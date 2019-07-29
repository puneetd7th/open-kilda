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

import org.openkilda.model.ConnectedDeviceType;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchConnectedDevice;
import org.openkilda.model.SwitchConnectedDevice.SwitchConnectedDeviceData;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ferma.frames.converters.ConnectedDeviceTypeConverter;
import org.openkilda.persistence.ferma.frames.converters.InstantStringConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Property;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;

import java.time.Instant;
import java.util.Optional;

public abstract class SwitchConnectedDeviceFrame extends KildaBaseVertexFrame implements SwitchConnectedDeviceData {
    public static final String FRAME_LABEL = "switch_connected_device";
    public static final String HAS_BY_EDGE = "has";
    public static final String FLOW_ID_PROPERTY = "flow_id";
    public static final String PORT_NUMBER_PROPERTY = "port_number";
    public static final String VLAN_PROPERTY = "vlan";
    public static final String TYPE_PROPERTY = "type";
    public static final String MAC_ADDRESS_PROPERTY = "mac_address";
    public static final String CHASSIS_ID_PROPERTY = "chassis_id";
    public static final String PORT_ID_PROPERTY = "port_id";
    public static final String IP_ADDRESS_PROPERTY = "ip_address";

    @Override
    public SwitchId getSwitchId() {
        return traverse(v -> v.in(HAS_BY_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                .values(SwitchFrame.SWITCH_ID_PROPERTY)).getRawTraversal().tryNext()
                .map(s -> (String) s).map(SwitchIdConverter.INSTANCE::map).orElse(null);
    }

    @Override
    public Switch getSwitchObj() {
        return Optional.ofNullable(traverse(v -> v.in(HAS_BY_EDGE)
                .hasLabel(SwitchFrame.FRAME_LABEL))
                .nextExplicit(SwitchFrame.class)).map(Switch::new).orElse(null);
    }

    @Override
    public void setSwitchObj(Switch switchObj) {
        getElement().edges(Direction.IN, HAS_BY_EDGE).forEachRemaining(Element::remove);

        Switch.SwitchData data = switchObj.getData();
        if (data instanceof SwitchFrame) {
            linkIn((VertexFrame) data, HAS_BY_EDGE);
        } else {
            SwitchFrame frame = SwitchFrame.load(getGraph(), data.getSwitchId()).orElseThrow(() ->
                    new IllegalArgumentException("Unable to link to non-existent switch " + switchObj));
            linkIn(frame, HAS_BY_EDGE);
        }
    }

    @Override
    @Property(PORT_NUMBER_PROPERTY)
    public abstract int getPortNumber();

    @Override
    @Property(PORT_NUMBER_PROPERTY)
    public abstract void setPortNumber(int portNumber);

    @Override
    @Property(VLAN_PROPERTY)
    public abstract int getVlan();

    @Override
    @Property(VLAN_PROPERTY)
    public abstract void setVlan(int vlan);

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract String getFlowId();

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract void setFlowId(String flowId);

    @Override
    @Property("source")
    public abstract Boolean getSource();

    @Override
    @Property("source")
    public abstract void setSource(Boolean source);

    @Override
    @Property(MAC_ADDRESS_PROPERTY)
    public abstract String getMacAddress();

    @Override
    @Property(MAC_ADDRESS_PROPERTY)
    public abstract void setMacAddress(String macAddress);

    @Override
    public ConnectedDeviceType getType() {
        return ConnectedDeviceTypeConverter.INSTANCE.map((String) getProperty(TYPE_PROPERTY));
    }

    @Override
    public void setType(ConnectedDeviceType connectedDeviceType) {
        setProperty(TYPE_PROPERTY, ConnectedDeviceTypeConverter.INSTANCE.map(connectedDeviceType));
    }

    @Override
    @Property("ip_address")
    public abstract String getIpAddress();

    @Override
    @Property("ip_address")
    public abstract void setIpAddress(String ipAddress);

    @Override
    @Property(CHASSIS_ID_PROPERTY)
    public abstract String getChassisId();

    @Override
    @Property(CHASSIS_ID_PROPERTY)
    public abstract void setChassisId(String chassisId);

    @Override
    @Property(PORT_ID_PROPERTY)
    public abstract String getPortId();

    @Override
    @Property(PORT_ID_PROPERTY)
    public abstract void setPortId(String portId);

    @Override
    @Property("ttl")
    public abstract Integer getTtl();

    @Override
    @Property("ttl")
    public abstract void setTtl(Integer ttl);

    @Override
    @Property("port_description")
    public abstract String getPortDescription();

    @Override
    @Property("port_description")
    public abstract void setPortDescription(String portDescription);

    @Override
    @Property("system_name")
    public abstract String getSystemName();

    @Override
    @Property("system_name")
    public abstract void setSystemName(String systemName);

    @Override
    @Property("system_description")
    public abstract String getSystemDescription();

    @Override
    @Property("system_description")
    public abstract void setSystemDescription(String systemDescription);

    @Override
    @Property("system_capabilities")
    public abstract String getSystemCapabilities();

    @Override
    @Property("system_capabilities")
    public abstract void setSystemCapabilities(String systemCapabilities);

    @Override
    @Property("management_address")
    public abstract String getManagementAddress();

    @Override
    @Property("management_address")
    public abstract void setManagementAddress(String managementAddress);

    @Override
    public Instant getTimeFirstSeen() {
        return InstantStringConverter.INSTANCE.map((String) getProperty("time_first_seen"));
    }

    @Override
    public void setTimeFirstSeen(Instant timeFirstSeen) {
        setProperty("time_first_seen", InstantStringConverter.INSTANCE.map(timeFirstSeen));
    }

    @Override
    public Instant getTimeLastSeen() {
        return InstantStringConverter.INSTANCE.map((String) getProperty("time_last_seen"));
    }

    @Override
    public void setTimeLastSeen(Instant timeLastSeen) {
        setProperty("time_last_seen", InstantStringConverter.INSTANCE.map(timeLastSeen));
    }

    public static SwitchConnectedDeviceFrame create(FramedGraph framedGraph, SwitchConnectedDeviceData data) {
        SwitchConnectedDeviceFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                SwitchConnectedDeviceFrame.class);
        SwitchConnectedDevice.SwitchConnectedDeviceCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
