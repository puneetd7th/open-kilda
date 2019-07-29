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

import org.openkilda.model.FeatureToggles;
import org.openkilda.model.FeatureToggles.FeatureTogglesData;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FeatureTogglesFrame;
import org.openkilda.persistence.repositories.FeatureTogglesRepository;

import java.util.Optional;

/**
 * Ferma (Tinkerpop) implementation of {@link FeatureTogglesRepository}.
 */
class FermaFeatureTogglesRepository extends FermaGenericRepository<FeatureToggles, FeatureTogglesData>
        implements FeatureTogglesRepository {
    FermaFeatureTogglesRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Optional<FeatureToggles> find() {
        return Optional.ofNullable(framedGraph().traverse(g -> g.V().hasLabel(FeatureTogglesFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(FeatureTogglesFrame.class, null))
                .map(FeatureToggles::new);
    }

    @Override
    public FeatureToggles getOrDefault() {
        return find().orElse(FeatureToggles.DEFAULTS);
    }

    @Override
    public FeatureToggles add(FeatureToggles entity) {
        FeatureTogglesData data = entity.getData();
        if (data instanceof FeatureTogglesFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() ->
                new FeatureToggles(FeatureTogglesFrame.create(framedGraph(), data)));
    }
}
