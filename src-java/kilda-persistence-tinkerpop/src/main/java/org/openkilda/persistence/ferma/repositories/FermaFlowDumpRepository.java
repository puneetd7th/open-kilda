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

import org.openkilda.model.history.FlowDump;
import org.openkilda.model.history.FlowDump.FlowDumpData;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowDumpFrame;
import org.openkilda.persistence.repositories.history.FlowDumpRepository;

/**
 * Ferma (Tinkerpop) implementation of {@link FlowDumpRepository}.
 */
class FermaFlowDumpRepository extends FermaGenericRepository<FlowDump, FlowDumpData>
        implements FlowDumpRepository {
    FermaFlowDumpRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public FlowDump add(FlowDump entity) {
        FlowDumpData data = entity.getData();
        if (data instanceof FlowDumpFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() -> new FlowDump(FlowDumpFrame.create(framedGraph(), data)));
    }
}
