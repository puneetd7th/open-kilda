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

import org.openkilda.model.PathId;
import org.openkilda.model.Vxlan;
import org.openkilda.persistence.InMemoryGraphBasedTest;
import org.openkilda.persistence.repositories.VxlanRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class FermaVxlanRepositoryTest extends InMemoryGraphBasedTest {
    static final String TEST_FLOW_ID = "test_flow";

    VxlanRepository vxlanRepository;

    @Before
    public void setUp() {
        cleanTinkerGraph();

        vxlanRepository = repositoryFactory.getVxlanRepository();
    }

    @Test
    public void shouldCreateVxlan() {
        Vxlan vxlan = createVxlan();

        Collection<Vxlan> allVxlans = vxlanRepository.findAll();
        Vxlan foundVxlan = allVxlans.iterator().next();

        assertEquals(vxlan.getVni(), foundVxlan.getVni());
        assertEquals(TEST_FLOW_ID, foundVxlan.getFlowId());
    }

    @Test
    public void shouldDeleteVxlan() {
        Vxlan vxlan = createVxlan();

        vxlanRepository.remove(vxlan);

        assertEquals(0, vxlanRepository.findAll().size());
    }

    @Test
    public void shouldDeleteFoundVxlan() {
        createVxlan();

        Collection<Vxlan> allVxlans = vxlanRepository.findAll();
        Vxlan foundVxlan = allVxlans.iterator().next();
        vxlanRepository.remove(foundVxlan);

        assertEquals(0, vxlanRepository.findAll().size());
    }

    private Vxlan createVxlan() {
        Vxlan vxlan = Vxlan.builder()
                .vni(1).pathId(new PathId(TEST_FLOW_ID + "_path")).flowId(TEST_FLOW_ID).build();
        return vxlanRepository.add(vxlan);
    }
}
