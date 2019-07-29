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

import org.openkilda.model.PathId;
import org.openkilda.model.TransitVlan;
import org.openkilda.model.TransitVlan.TransitVlanData;
import org.openkilda.persistence.ConstraintViolationException;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;
import lombok.NonNull;

public abstract class TransitVlanFrame extends KildaBaseVertexFrame implements TransitVlanData {
    public static final String FRAME_LABEL = "transit_vlan";
    public static final String PATH_ID_PROPERTY = "path_id";
    public static final String VLAN_PROPERTY = "vlan";

    @Override
    @Property("flow_id")
    public abstract String getFlowId();

    @Override
    @Property("flow_id")
    public abstract void setFlowId(String flowId);

    @Override
    public PathId getPathId() {
        return PathIdConverter.INSTANCE.map((String) getProperty(PATH_ID_PROPERTY));
    }

    @Override
    public void setPathId(@NonNull PathId pathId) {
        setProperty(PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId));
    }

    @Override
    @Property(VLAN_PROPERTY)
    public abstract int getVlan();

    @Override
    @Property(VLAN_PROPERTY)
    public abstract void setVlan(int vlan);

    public static TransitVlanFrame create(FramedGraph framedGraph, TransitVlanData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL)
                .has(VLAN_PROPERTY, data.getVlan()).count()).getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a vertex with duplicated "
                    + VLAN_PROPERTY);
        }

        TransitVlanFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                TransitVlanFrame.class);
        TransitVlan.TransitVlanCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
