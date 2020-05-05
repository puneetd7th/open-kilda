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

package org.openkilda.persistence.ferma.repositories;

import org.openkilda.model.history.FlowHistory;
import org.openkilda.model.history.FlowHistory.FlowHistoryData;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowHistoryFrame;
import org.openkilda.persistence.ferma.frames.KildaBaseVertexFrame;
import org.openkilda.persistence.repositories.history.FlowHistoryRepository;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

/**
 * Ferma (Tinkerpop) implementation of {@link FlowHistoryRepository}.
 */
class FermaFlowHistoryRepository extends FermaGenericRepository<FlowHistory, FlowHistoryData, FlowHistoryFrame>
        implements FlowHistoryRepository {
    FermaFlowHistoryRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    protected FlowHistoryFrame doAdd(FlowHistoryData data) {
        FlowHistoryFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph(),
                FlowHistoryFrame.FRAME_LABEL, FlowHistoryFrame.class);
        FlowHistory.FlowHistoryCloner.INSTANCE.copy(data, frame);
        return frame;
    }

    @Override
    protected FlowHistoryData doRemove(FlowHistory entity, FlowHistoryFrame frame) {
        FlowHistoryData data = FlowHistory.FlowHistoryCloner.INSTANCE.copy(frame);
        frame.getElement().edges(Direction.BOTH).forEachRemaining(Edge::remove);
        frame.remove();
        return data;
    }
}
