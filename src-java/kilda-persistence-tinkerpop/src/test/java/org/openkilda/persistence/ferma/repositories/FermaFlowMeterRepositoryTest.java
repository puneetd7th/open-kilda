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

import org.openkilda.model.FlowMeter;
import org.openkilda.model.MeterId;
import org.openkilda.model.PathId;
import org.openkilda.model.Switch;
import org.openkilda.persistence.InMemoryGraphBasedTest;
import org.openkilda.persistence.PersistenceException;
import org.openkilda.persistence.repositories.FlowMeterRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class FermaFlowMeterRepositoryTest extends InMemoryGraphBasedTest {
    static final String TEST_FLOW_ID = "test_flow";
    static final String TEST_PATH_ID = "test_path";

    FlowMeterRepository flowMeterRepository;

    Switch theSwitch;

    @Before
    public void setUp() {
        cleanTinkerGraph();

        flowMeterRepository = repositoryFactory.getFlowMeterRepository();

        theSwitch = createTestSwitch(1);
    }

    @Test
    public void shouldCreateFlowMeter() {
        createFlowMeter();

        Collection<FlowMeter> allMeters = flowMeterRepository.findAll();
        FlowMeter foundMeter = allMeters.iterator().next();

        assertEquals(theSwitch.getSwitchId(), foundMeter.getSwitchId());
        assertEquals(TEST_FLOW_ID, foundMeter.getFlowId());
    }

    @Test(expected = PersistenceException.class)
    public void shouldNotGetMoreThanTwoMetersForPath() {
        createFlowMeter(1, new PathId(TEST_PATH_ID));
        createFlowMeter(2, new PathId(TEST_PATH_ID));
        createFlowMeter(3, new PathId(TEST_PATH_ID));
        flowMeterRepository.findByPathId(new PathId(TEST_PATH_ID));
    }

    @Test
    public void shouldGetZeroMetersForPath() {
        Collection<FlowMeter> meters = flowMeterRepository.findByPathId(new PathId(TEST_PATH_ID));
        assertEquals(0, meters.size());
    }

    @Test
    public void shouldDeleteFlowMeter() {
        FlowMeter meter = createFlowMeter();

        flowMeterRepository.remove(meter);

        assertEquals(0, flowMeterRepository.findAll().size());
    }

    @Test
    public void shouldDeleteFoundFlowMeter() {
        createFlowMeter();

        Collection<FlowMeter> allMeters = flowMeterRepository.findAll();
        FlowMeter foundMeter = allMeters.iterator().next();
        flowMeterRepository.remove(foundMeter);

        assertEquals(0, flowMeterRepository.findAll().size());
    }

    private FlowMeter createFlowMeter(int meterId, PathId pathId) {
        FlowMeter flowMeter = FlowMeter.builder()
                .switchId(theSwitch.getSwitchId())
                .meterId(new MeterId(meterId))
                .pathId(pathId)
                .flowId(TEST_FLOW_ID)
                .build();
        return flowMeterRepository.add(flowMeter);
    }

    private FlowMeter createFlowMeter() {
        return createFlowMeter(1, new PathId(TEST_PATH_ID));
    }
}
