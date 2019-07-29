/* Copyright 2019 Telstra Open Source
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

import org.openkilda.model.SwitchId;
import org.openkilda.model.SwitchProperties;
import org.openkilda.model.SwitchProperties.SwitchPropertiesData;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.SwitchPropertiesFrame;
import org.openkilda.persistence.repositories.SwitchPropertiesRepository;

import com.syncleus.ferma.ElementFrame;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ferma (Tinkerpop) implementation of {@link SwitchPropertiesRepository}.
 */
class FermaSwitchPropertiesRepository
        extends FermaGenericRepository<SwitchProperties, SwitchPropertiesData>
        implements SwitchPropertiesRepository {
    FermaSwitchPropertiesRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public Collection<SwitchProperties> findAll() {
        return framedGraph().traverse(g -> g.V().hasLabel(SwitchPropertiesFrame.FRAME_LABEL))
                .toListExplicit(SwitchPropertiesFrame.class).stream()
                .map(SwitchProperties::new).collect(Collectors.toList());
    }

    @Override
    public Optional<SwitchProperties> findBySwitchId(SwitchId switchId) {
        return Optional.ofNullable(
                framedGraph().traverse(g -> FermaSwitchRepository.getTraverseForSwitch(g, switchId)
                        .out(SwitchPropertiesFrame.HAS_BY_EDGE)
                        .hasLabel(SwitchPropertiesFrame.FRAME_LABEL))
                        .nextOrDefaultExplicit(SwitchPropertiesFrame.class, null)).map(SwitchProperties::new);
    }

    @Override
    public SwitchProperties add(SwitchProperties entity) {
        SwitchPropertiesData data = entity.getData();
        if (data instanceof ElementFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() ->
                new SwitchProperties(SwitchPropertiesFrame.create(framedGraph(), data)));
    }
}
