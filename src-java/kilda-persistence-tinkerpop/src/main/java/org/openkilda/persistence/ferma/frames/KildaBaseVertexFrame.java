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

import org.openkilda.persistence.ferma.frames.converters.InstantStringConverter;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.time.Instant;

/**
 * A base for Kilda data model entity that is mapped to a graph vertex.
 */
public abstract class KildaBaseVertexFrame extends AbstractVertexFrame {
    public static final String TIME_CREATE_PROPERTY = "time_create";
    public static final String TIME_MODIFY_PROPERTY = "time_modify";

    @Override
    public void setProperty(String name, Object value) {
        if (!name.equals(TIME_CREATE_PROPERTY) && !name.equals(TIME_MODIFY_PROPERTY)) {
            setProperty(TIME_MODIFY_PROPERTY, InstantStringConverter.INSTANCE.toGraphProperty(Instant.now()));
        }
        super.setProperty(name, value);
    }

    public Instant getTimeCreate() {
        return InstantStringConverter.INSTANCE.toEntityAttribute((String) getProperty(TIME_CREATE_PROPERTY));
    }

    public void setTimeCreate(Instant timeCreate) {
        // Do nothing as timestamps are not set or updated directly
    }

    public Instant getTimeModify() {
        return InstantStringConverter.INSTANCE.toEntityAttribute((String) getProperty(TIME_MODIFY_PROPERTY));
    }

    public void setTimeModify(Instant timeModify) {
        // Do nothing as timestamps are not set or updated directly
    }

    public static <U extends KildaBaseVertexFrame> U addNewFramedVertex(FramedGraph graph, String label,
                                                                        Class<U> frameClass) {
        Vertex element = ((DelegatingFramedGraph) graph).getBaseGraph().addVertex(T.label, label);
        U frame = graph.frameElementExplicit(element, frameClass);
        String now = InstantStringConverter.INSTANCE.toGraphProperty(Instant.now());
        frame.setProperty(TIME_CREATE_PROPERTY, now);
        frame.setProperty(TIME_MODIFY_PROPERTY, now);
        return frame;
    }
}
