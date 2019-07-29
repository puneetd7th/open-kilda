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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.openkilda.model.PathId;
import org.openkilda.model.TransitVlan;
import org.openkilda.persistence.InMemoryGraphBasedTest;
import org.openkilda.persistence.repositories.TransitVlanRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;

public class FermaTransitVlanRepositoryTest extends InMemoryGraphBasedTest {
    static final String TEST_FLOW_ID = "test_flow";
    static final int VLAN = 1;

    TransitVlanRepository transitVlanRepository;

    @Before
    public void setUp() {
        cleanTinkerGraph();

        transitVlanRepository = repositoryFactory.getTransitVlanRepository();
    }

    @Test
    public void shouldCreateTransitVlan() {
        TransitVlan vlan = createTransitVlan();

        Collection<TransitVlan> allVlans = transitVlanRepository.findAll();
        TransitVlan foundVlan = allVlans.iterator().next();

        assertEquals(vlan.getVlan(), foundVlan.getVlan());
        assertEquals(TEST_FLOW_ID, foundVlan.getFlowId());
    }

    @Test
    public void shouldFindTransitVlan() {
        TransitVlan vlan = createTransitVlan();
        vlan.setVlan(VLAN);

        Optional<TransitVlan> foundVlan = transitVlanRepository.findByVlan(VLAN);

        assertTrue(foundVlan.isPresent());
        assertEquals(vlan.getVlan(), foundVlan.get().getVlan());
        assertEquals(vlan.getFlowId(), foundVlan.get().getFlowId());
        assertEquals(vlan.getPathId(), foundVlan.get().getPathId());
    }

    @Test
    public void shouldDeleteFlowMeter() {
        TransitVlan vlan = createTransitVlan();

        transitVlanRepository.remove(vlan);

        assertEquals(0, transitVlanRepository.findAll().size());
    }

    @Test
    public void shouldDeleteFoundFlowMeter() {
        createTransitVlan();

        Collection<TransitVlan> allVlans = transitVlanRepository.findAll();
        TransitVlan foundVlan = allVlans.iterator().next();
        transitVlanRepository.remove(foundVlan);

        assertEquals(0, transitVlanRepository.findAll().size());
    }

    private TransitVlan createTransitVlan() {
        TransitVlan transitVlan = TransitVlan.builder()
                .vlan(1).pathId(new PathId(TEST_FLOW_ID + "_path")).flowId(TEST_FLOW_ID).build();
        return transitVlanRepository.add(transitVlan);
    }
}
