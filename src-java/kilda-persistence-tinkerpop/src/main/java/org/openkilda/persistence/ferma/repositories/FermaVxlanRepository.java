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

import org.openkilda.model.PathId;
import org.openkilda.model.Vxlan;
import org.openkilda.model.Vxlan.VxlanData;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowCookieFrame;
import org.openkilda.persistence.ferma.frames.VxlanFrame;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.repositories.VxlanRepository;

import com.syncleus.ferma.ElementFrame;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ferma (Tinkerpop) implementation of {@link VxlanRepository}.
 */
class FermaVxlanRepository extends FermaGenericRepository<Vxlan, VxlanData> implements VxlanRepository {
    FermaVxlanRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Collection<Vxlan> findAll() {
        return framedGraph().traverse(g -> g.V().hasLabel(VxlanFrame.FRAME_LABEL))
                .toListExplicit(VxlanFrame.class).stream().map(Vxlan::new).collect(Collectors.toList());
    }

    @Override
    public Collection<Vxlan> findByPathId(PathId pathId, PathId oppositePathId) {
        List<? extends VxlanFrame> frames =
                framedGraph().traverse(g -> g.V().hasLabel(VxlanFrame.FRAME_LABEL)
                        .has(VxlanFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId)))
                        .toListExplicit(VxlanFrame.class);
        if (frames.isEmpty() && oppositePathId != null) {
            frames = framedGraph().traverse(g -> g.V().hasLabel(VxlanFrame.FRAME_LABEL)
                    .has(VxlanFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(oppositePathId)))
                    .toListExplicit(VxlanFrame.class);
        }
        return frames.stream().map(Vxlan::new).collect(Collectors.toList());
    }

    @Override
    public Optional<Integer> findMaximumAssignedVxlan() {
        return framedGraph().traverse(g -> g.V().hasLabel(FlowCookieFrame.FRAME_LABEL)
                .values(VxlanFrame.VNI_PROPERTY).max())
                .getRawTraversal().tryNext()
                .filter(n -> !(n instanceof Double && ((Double) n).isNaN()))
                .map(l -> l instanceof Integer ? (Integer) l : ((Long) l).intValue());
    }

    @Override
    public int findFirstUnassignedVxlan(int startVxlan) {
        return framedGraph().traverse(g -> g.V().hasLabel(VxlanFrame.FRAME_LABEL)
                .has(VxlanFrame.VNI_PROPERTY, P.gte(startVxlan))
                .values(VxlanFrame.VNI_PROPERTY)
                .order().math("_ + 1").as("a")
                .where(__.V().hasLabel(VxlanFrame.FRAME_LABEL)
                        .values(VxlanFrame.VNI_PROPERTY)
                        .where(P.eq("a")).count().is(0))
                .select("a"))
                .getRawTraversal().tryNext()
                .map(l -> ((Double) l).intValue()).orElse(startVxlan);
    }

    @Override
    public Vxlan add(Vxlan entity) {
        VxlanData data = entity.getData();
        if (data instanceof ElementFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() -> new Vxlan(VxlanFrame.create(framedGraph(), data)));
    }
}
