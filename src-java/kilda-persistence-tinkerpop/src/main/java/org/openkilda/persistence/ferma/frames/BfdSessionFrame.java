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

import org.openkilda.model.BfdSession;
import org.openkilda.model.BfdSession.BfdSessionData;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ConstraintViolationException;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;

public abstract class BfdSessionFrame extends KildaBaseVertexFrame implements BfdSessionData {
    public static final String FRAME_LABEL = "bfd_session";
    public static final String SWITCH_PROPERTY = "switch";
    public static final String IP_ADDRESS_PROPERTY = "ip_address";
    public static final String REMOTE_SWITCH_PROPERTY = "remote_switch";
    public static final String REMOVE_IP_ADDRESS_PROPERTY = "remote_ip_address";
    public static final String PORT_PROPERTY = "port";
    public static final String DISCRIMINATOR_PROPERTY = "discriminator";

    @Override
    public SwitchId getSwitchId() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty(SWITCH_PROPERTY));
    }

    @Override
    public void setSwitchId(SwitchId switchId) {
        setProperty(SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId));
    }

    @Override
    @Property(IP_ADDRESS_PROPERTY)
    public abstract String getIpAddress();

    @Override
    @Property(IP_ADDRESS_PROPERTY)
    public abstract void setIpAddress(String ipAddress);

    @Override
    public SwitchId getRemoteSwitchId() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty(REMOTE_SWITCH_PROPERTY));
    }

    @Override
    public void setRemoteSwitchId(SwitchId switchId) {
        setProperty(REMOTE_SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(switchId));
    }

    @Override
    @Property(REMOVE_IP_ADDRESS_PROPERTY)
    public abstract String getRemoteIpAddress();

    @Override
    @Property(REMOVE_IP_ADDRESS_PROPERTY)
    public abstract void setRemoteIpAddress(String remoteIpAddress);

    @Override
    @Property(PORT_PROPERTY)
    public abstract Integer getPort();

    @Override
    @Property(PORT_PROPERTY)
    public abstract void setPort(Integer port);

    @Override
    @Property(DISCRIMINATOR_PROPERTY)
    public abstract Integer getDiscriminator();

    @Override
    @Property(DISCRIMINATOR_PROPERTY)
    public abstract void setDiscriminator(Integer discriminator);

    public static BfdSessionFrame create(FramedGraph framedGraph, BfdSessionData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL)
                .has(DISCRIMINATOR_PROPERTY, data.getDiscriminator()).count()).getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a vertex with duplicated "
                    + DISCRIMINATOR_PROPERTY);
        }

        BfdSessionFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                BfdSessionFrame.class);
        BfdSession.BfdSessionCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
