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

package org.openkilda.persistence.repositories.impl;

import static org.junit.Assert.assertEquals;

import org.openkilda.model.PathId;
import org.openkilda.model.Vxlan;
import org.openkilda.persistence.Neo4jBasedTest;
import org.openkilda.persistence.repositories.VxlanRepository;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

public class Neo4jVxlanRepositoryTest extends Neo4jBasedTest {
    static final String TEST_FLOW_ID = "test_flow";
    static final int MIN_VNI = 5;
    static final int MAX_VNI = 25;

    static VxlanRepository vxlanRepository;

    @BeforeClass
    public static void setUp() {
        vxlanRepository = new Neo4jVxlanRepository(neo4jSessionFactory, txManager);
    }

    @Test
    public void shouldCreateVxlan() {
        Vxlan vxlan = Vxlan.builder()
                .vni(1)
                .pathId(new PathId(TEST_FLOW_ID + "_path"))
                .flowId(TEST_FLOW_ID)
                .build();
        vxlanRepository.createOrUpdate(vxlan);

        Collection<Vxlan> allVxlans = vxlanRepository.findAll();
        Vxlan foundVxlan = allVxlans.iterator().next();

        assertEquals(vxlan.getVni(), foundVxlan.getVni());
        assertEquals(TEST_FLOW_ID, foundVxlan.getFlowId());
    }

    @Test
    public void shouldDeleteVxlan() {
        Vxlan vxlan = Vxlan.builder()
                .vni(1)
                .pathId(new PathId(TEST_FLOW_ID + "_path"))
                .flowId(TEST_FLOW_ID)
                .build();
        vxlanRepository.createOrUpdate(vxlan);

        vxlanRepository.delete(vxlan);

        assertEquals(0, vxlanRepository.findAll().size());
    }

    @Test
    public void shouldDeleteFoundVxlan() {
        Vxlan vxlan = Vxlan.builder()
                .vni(1)
                .pathId(new PathId(TEST_FLOW_ID + "_path"))
                .flowId(TEST_FLOW_ID)
                .build();
        vxlanRepository.createOrUpdate(vxlan);

        Collection<Vxlan> allVxlans = vxlanRepository.findAll();
        Vxlan foundVxlan = allVxlans.iterator().next();
        vxlanRepository.delete(foundVxlan);

        assertEquals(0, vxlanRepository.findAll().size());
    }

    @Test
    public void shouldSelectNextInOrderResourceWhenFindUnassignedVxlan() {
        int first = findUnassignedVxlanAndCreate("flow_1");
        assertEquals(5, first);

        int second = findUnassignedVxlanAndCreate("flow_2");
        assertEquals(6, second);

        int third = findUnassignedVxlanAndCreate("flow_3");
        assertEquals(7, third);

        vxlanRepository.findAll().stream()
                .filter(vxlan -> vxlan.getVni() == second)
                .forEach(vxlanRepository::delete);
        int fourth = findUnassignedVxlanAndCreate("flow_4");
        assertEquals(6, fourth);

        int fifth = findUnassignedVxlanAndCreate("flow_5");
        assertEquals(8, fifth);
    }

    private int findUnassignedVxlanAndCreate(String flowId) {
        int availableVni = vxlanRepository.findUnassignedVxlan(MIN_VNI, MAX_VNI).get();
        Vxlan vxlan = Vxlan.builder()
                .vni(availableVni)
                .pathId(new PathId(TEST_FLOW_ID + "_path"))
                .flowId(flowId)
                .build();
        vxlanRepository.createOrUpdate(vxlan);
        return availableVni;
    }
}
