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

import org.openkilda.model.KildaConfiguration;
import org.openkilda.model.KildaConfiguration.KildaConfigurationData;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.KildaConfigurationFrame;
import org.openkilda.persistence.repositories.KildaConfigurationRepository;

import java.util.Optional;

/**
 * Ferma (Tinkerpop) implementation of {@link KildaConfigurationRepository}.
 */
class FermaKildaConfigurationRepository extends FermaGenericRepository<KildaConfiguration, KildaConfigurationData>
        implements KildaConfigurationRepository {
    FermaKildaConfigurationRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Optional<KildaConfiguration> find() {
        return Optional.ofNullable(framedGraph().traverse(g -> g.V()
                .hasLabel(KildaConfigurationFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(KildaConfigurationFrame.class, null))
                .map(KildaConfiguration::new);
    }

    @Override
    public KildaConfiguration getOrDefault() {
        return find().orElse(KildaConfiguration.DEFAULTS);
    }

    @Override
    public KildaConfiguration add(KildaConfiguration entity) {
        KildaConfigurationData data = entity.getData();
        if (data instanceof KildaConfigurationFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() ->
                new KildaConfiguration(KildaConfigurationFrame.create(framedGraph(), data)));
    }
}
