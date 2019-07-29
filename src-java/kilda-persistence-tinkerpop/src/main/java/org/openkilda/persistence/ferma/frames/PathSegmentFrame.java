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

import org.openkilda.model.FlowPath;
import org.openkilda.model.PathId;
import org.openkilda.model.PathSegment;
import org.openkilda.model.PathSegment.PathSegmentData;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Property;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;

import java.util.Optional;

public abstract class PathSegmentFrame extends KildaBaseVertexFrame implements PathSegmentData {
    public static final String FRAME_LABEL = "path_segment";
    public static final String SOURCE_EDGE = "source";
    public static final String DESTINATION_EDGE = "destination";
    public static final String SRC_PORT_PROPERTY = "src_port";
    public static final String DST_PORT_PROPERTY = "dst_port";
    public static final String SRC_W_MULTI_TABLE_PROPERTY = "src_with_multi_table";
    public static final String DST_W_MULTI_TABLE_PROPERTY = "dst_with_multi_table";

    @Override
    @Property(SRC_PORT_PROPERTY)
    public abstract int getSrcPort();

    @Override
    @Property(SRC_PORT_PROPERTY)
    public abstract void setSrcPort(int srcPort);

    @Override
    @Property(DST_PORT_PROPERTY)
    public abstract int getDestPort();

    @Override
    @Property(DST_PORT_PROPERTY)
    public abstract void setDestPort(int destPort);

    @Override
    @Property("seq_id")
    public abstract int getSeqId();

    @Override
    @Property("seq_id")
    public abstract void setSeqId(int seqId);

    @Override
    @Property("latency")
    public abstract Long getLatency();

    @Override
    @Property("latency")
    public abstract void setLatency(Long latency);

    @Override
    @Property("failed")
    public abstract boolean isFailed();

    @Override
    @Property("failed")
    public abstract void setFailed(boolean failed);

    @Override
    public Switch getSrcSwitch() {
        return Optional.ofNullable(traverse(v -> v.out(SOURCE_EDGE)
                .hasLabel(SwitchFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(SwitchFrame.class, null)).map(Switch::new).orElse(null);
    }

    @Override
    public SwitchId getSrcSwitchId() {
        return traverse(v -> v.out(SOURCE_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                .values(SwitchFrame.SWITCH_ID_PROPERTY)).getRawTraversal().tryNext()
                .map(s -> (String) s).map(SwitchIdConverter.INSTANCE::map).orElse(null);
    }

    @Override
    public void setSrcSwitch(Switch srcSwitch) {
        getElement().edges(Direction.OUT, SOURCE_EDGE).forEachRemaining(Element::remove);

        Switch.SwitchData data = srcSwitch.getData();
        if (data instanceof SwitchFrame) {
            linkOut((VertexFrame) data, SOURCE_EDGE);
        } else {
            SwitchFrame frame = SwitchFrame.load(getGraph(), data.getSwitchId()).orElseThrow(() ->
                    new IllegalArgumentException("Unable to link to non-existent switch " + srcSwitch));
            linkOut(frame, SOURCE_EDGE);
        }
    }

    @Override
    public Switch getDestSwitch() {
        return Optional.ofNullable(traverse(v -> v.out(DESTINATION_EDGE)
                .hasLabel(SwitchFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(SwitchFrame.class, null)).map(Switch::new).orElse(null);
    }

    @Override
    public SwitchId getDestSwitchId() {
        return traverse(v -> v.out(DESTINATION_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                .values(SwitchFrame.SWITCH_ID_PROPERTY)).getRawTraversal().tryNext()
                .map(s -> (String) s).map(SwitchIdConverter.INSTANCE::map).orElse(null);
    }

    @Override
    public void setDestSwitch(Switch destSwitch) {
        getElement().edges(Direction.OUT, DESTINATION_EDGE).forEachRemaining(Element::remove);

        Switch.SwitchData data = destSwitch.getData();
        if (data instanceof SwitchFrame) {
            linkOut((VertexFrame) data, DESTINATION_EDGE);
        } else {
            SwitchFrame frame = SwitchFrame.load(getGraph(), data.getSwitchId()).orElseThrow(() ->
                    new IllegalArgumentException("Unable to link to non-existent switch " + destSwitch));
            linkOut(frame, DESTINATION_EDGE);
        }
    }

    @Override
    public PathId getPathId() {
        return traverse(v -> v.in(FlowPathFrame.OWNS_SEGMENTS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL)
                .values(FlowPathFrame.PATH_ID_PROPERTY)).getRawTraversal().tryNext()
                .map(s -> (String) s).map(PathIdConverter.INSTANCE::map).orElse(null);
    }

    @Override
    public FlowPath getPath() {
        return Optional.ofNullable(traverse(v -> v.in(FlowPathFrame.OWNS_SEGMENTS_EDGE)
                .hasLabel(FlowPathFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(FlowPathFrame.class, null)).map(FlowPath::new).orElse(null);
    }

    @Override
    @Property(SRC_W_MULTI_TABLE_PROPERTY)
    public abstract boolean isSrcWithMultiTable();

    @Override
    @Property(SRC_W_MULTI_TABLE_PROPERTY)
    public abstract void setSrcWithMultiTable(boolean srcWithMultiTable);

    @Override
    @Property(DST_W_MULTI_TABLE_PROPERTY)
    public abstract boolean isDestWithMultiTable();

    @Override
    @Property(DST_W_MULTI_TABLE_PROPERTY)
    public abstract void setDestWithMultiTable(boolean destWithMultiTable);

    public static PathSegmentFrame create(FramedGraph framedGraph, PathSegmentData data) {
        PathSegmentFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                PathSegmentFrame.class);
        PathSegment.PathSegmentCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
