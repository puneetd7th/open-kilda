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

import org.openkilda.model.history.FlowDump;
import org.openkilda.model.history.FlowEvent;
import org.openkilda.model.history.FlowEvent.FlowEventData;
import org.openkilda.model.history.FlowHistory;
import org.openkilda.persistence.ConstraintViolationException;
import org.openkilda.persistence.ferma.frames.converters.InstantStringConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class FlowEventFrame extends KildaBaseVertexFrame implements FlowEventData {
    public static final String FRAME_LABEL = "flow_event";
    public static final String TASK_ID_PROPERTY = "task_id";
    public static final String FLOW_ID_PROPERTY = "flow_id";
    public static final String TIMESTAMP_PROPERTY = "timestamp";

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract String getFlowId();

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract void setFlowId(String flowId);

    @Override
    public Instant getTimestamp() {
        return InstantStringConverter.INSTANCE.map((String) getProperty(TIMESTAMP_PROPERTY));
    }

    @Override
    public void setTimestamp(Instant timestamp) {
        setProperty(TIMESTAMP_PROPERTY, InstantStringConverter.INSTANCE.map(timestamp));
    }

    @Override
    @Property("actor")
    public abstract String getActor();

    @Override
    @Property("actor")
    public abstract void setActor(String actor);

    @Override
    @Property("action")
    public abstract String getAction();

    @Override
    @Property("action")
    public abstract void setAction(String action);

    @Override
    @Property(TASK_ID_PROPERTY)
    public abstract String getTaskId();

    @Override
    @Property(TASK_ID_PROPERTY)
    public abstract void setTaskId(String taskId);

    @Override
    @Property("details")
    public abstract String getDetails();


    @Override
    @Property("details")
    public abstract void setDetails(String details);

    @Override
    public Collection<FlowHistory> getHistoryRecords() {
        return traverse(v -> v.out(FlowHistoryFrame.HISTORY_LOG_EDGE)
                .hasLabel(FlowHistoryFrame.FRAME_LABEL))
                .toListExplicit(FlowHistoryFrame.class).stream()
                .map(FlowHistory::new).collect(Collectors.toList());
    }

    @Override
    public Collection<FlowDump> getFlowDumps() {
        return traverse(v -> v.out(FlowDumpFrame.STATE_LOG_EDGE)
                .hasLabel(FlowDumpFrame.FRAME_LABEL))
                .toListExplicit(FlowDumpFrame.class).stream()
                .map(FlowDump::new).collect(Collectors.toList());
    }

    public static FlowEventFrame create(FramedGraph framedGraph, FlowEventData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL)
                .has(TASK_ID_PROPERTY, data.getTaskId()).count()).getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a vertex with duplicated " + TASK_ID_PROPERTY);
        }

        FlowEventFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                FlowEventFrame.class);
        FlowEvent.FlowEventCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
