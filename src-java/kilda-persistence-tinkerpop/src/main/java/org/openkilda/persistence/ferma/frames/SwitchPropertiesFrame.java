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

import org.openkilda.model.FlowEncapsulationType;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchId;
import org.openkilda.model.SwitchProperties;
import org.openkilda.model.SwitchProperties.SwitchPropertiesData;
import org.openkilda.persistence.ferma.frames.converters.FlowEncapsulationTypeConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Property;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class SwitchPropertiesFrame extends KildaBaseVertexFrame implements SwitchPropertiesData {
    public static final String FRAME_LABEL = "switch_properties";
    public static final String HAS_BY_EDGE = "has";
    public static final String SUPPORTED_TRANSIT_ENCAPSULATION_PROPERTY = "supported_transit_encapsulation";

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
    public Set<FlowEncapsulationType> getSupportedTransitEncapsulation() {
        Set<FlowEncapsulationType> results = new HashSet<>();
        getElement().properties(SUPPORTED_TRANSIT_ENCAPSULATION_PROPERTY).forEachRemaining(property -> {
            if (property.isPresent()) {
                Object propertyValue = property.value();
                if (propertyValue instanceof Collection) {
                    ((Collection<String>) propertyValue).forEach(entry ->
                            results.add(FlowEncapsulationTypeConverter.INSTANCE.map(entry)));
                } else {
                    results.add(FlowEncapsulationTypeConverter.INSTANCE.map((String) propertyValue));
                }
            }
        });
        return results;
    }

    @Override
    public void setSupportedTransitEncapsulation(Set<FlowEncapsulationType> supportedTransitEncapsulation) {
        //TODO: need to fix the support Cardinality.set in traversals (see FermaIslRepository)
        //getElement().properties(SUPPORTED_TRANSIT_ENCAPSULATION_PROPERTY)
        //        .forEachRemaining(property -> property.remove());

        getElement().property(SUPPORTED_TRANSIT_ENCAPSULATION_PROPERTY,
                FlowEncapsulationTypeConverter.INSTANCE.map(supportedTransitEncapsulation.iterator().next()));
        //supportedTransitEncapsulation.forEach(value ->
        //        getElement().property(VertexProperty.Cardinality.set, SUPPORTED_TRANSIT_ENCAPSULATION_PROPERTY,
        //                FlowEncapsulationTypeConverter.INSTANCE.map(value)));
    }

    @Override
    @Property("multitable")
    public abstract boolean isMultiTable();

    @Override
    @Property("multitable")
    public abstract void setMultiTable(boolean multiTable);

    @Override
    @Property("switch_lldp")
    public abstract boolean isSwitchLldp();

    @Override
    @Property("switch_lldp")
    public abstract void setSwitchLldp(boolean switchLldp);

    @Override
    @Property("switch_arp")
    public abstract boolean isSwitchArp();

    @Override
    @Property("switch_arp")
    public abstract void setSwitchArp(boolean switchArp);

    public static SwitchPropertiesFrame create(FramedGraph framedGraph, SwitchPropertiesData data) {
        SwitchPropertiesFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                SwitchPropertiesFrame.class);
        SwitchProperties.SwitchPropertiesCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
