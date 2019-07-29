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

import org.openkilda.model.LinkProps;
import org.openkilda.model.LinkProps.LinkPropsData;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;

public abstract class LinkPropsFrame extends KildaBaseVertexFrame implements LinkPropsData {
    public static final String FRAME_LABEL = "link_props";
    public static final String SRC_SWITCH_PROPERTY = "src_switch";
    public static final String SRC_PORT_PROPERTY = "src_port";
    public static final String DST_SWITCH_PROPERTY = "dst_switch";
    public static final String DST_PORT_PROPERTY = "dst_port";
    public static final String COST_PROP_NAME = "cost";
    public static final String MAX_BANDWIDTH_PROP_NAME = "max_bandwidth";

    @Override
    @Property(SRC_PORT_PROPERTY)
    public abstract int getSrcPort();

    @Override
    @Property(SRC_PORT_PROPERTY)
    public abstract void setSrcPort(int srcPort);

    @Override
    @Property(DST_PORT_PROPERTY)
    public abstract int getDstPort();

    @Override
    @Property(DST_PORT_PROPERTY)
    public abstract void setDstPort(int dstPort);

    @Override
    public SwitchId getSrcSwitchId() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty(SRC_SWITCH_PROPERTY));
    }

    @Override
    public void setSrcSwitchId(SwitchId srcSwitchId) {
        setProperty(SRC_SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(srcSwitchId));
    }

    @Override
    public SwitchId getDstSwitchId() {
        return SwitchIdConverter.INSTANCE.map((String) getProperty(DST_SWITCH_PROPERTY));
    }

    @Override
    public void setDstSwitchId(SwitchId dstSwitchId) {
        setProperty(DST_SWITCH_PROPERTY, SwitchIdConverter.INSTANCE.map(dstSwitchId));
    }

    @Override
    @Property(COST_PROP_NAME)
    public abstract Integer getCost();

    @Override
    @Property(COST_PROP_NAME)
    public abstract void setCost(Integer cost);

    @Override
    @Property(MAX_BANDWIDTH_PROP_NAME)
    public abstract Long getMaxBandwidth();

    @Override
    @Property(MAX_BANDWIDTH_PROP_NAME)
    public abstract void setMaxBandwidth(Long maxBandwidth);

    public static LinkPropsFrame create(FramedGraph framedGraph, LinkPropsData data) {
        LinkPropsFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL, LinkPropsFrame.class);
        LinkProps.LinkPropsCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
