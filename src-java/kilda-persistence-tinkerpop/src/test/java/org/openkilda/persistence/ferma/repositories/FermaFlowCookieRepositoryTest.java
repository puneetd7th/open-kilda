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

import org.openkilda.model.FlowCookie;
import org.openkilda.persistence.InMemoryGraphBasedTest;
import org.openkilda.persistence.repositories.FlowCookieRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class FermaFlowCookieRepositoryTest extends InMemoryGraphBasedTest {
    static final String TEST_FLOW_ID = "test_flow";
    static final long TEST_COOKIE = 1L;

    FlowCookieRepository flowCookieRepository;

    @Before
    public void setUp() {
        cleanTinkerGraph();

        flowCookieRepository = repositoryFactory.getFlowCookieRepository();
    }

    @Test
    public void shouldCreateFlowCookie() {
        createFlowCookie();

        Collection<FlowCookie> allCookies = flowCookieRepository.findAll();
        FlowCookie foundCookie = allCookies.iterator().next();

        assertEquals(TEST_COOKIE, foundCookie.getUnmaskedCookie());
        assertEquals(TEST_FLOW_ID, foundCookie.getFlowId());
    }

    @Test
    public void shouldDeleteFlowCookie() {
        FlowCookie cookie = createFlowCookie();

        flowCookieRepository.remove(cookie);

        assertEquals(0, flowCookieRepository.findAll().size());
    }

    @Test
    public void shouldDeleteFoundFlowCookie() {
        createFlowCookie();

        Collection<FlowCookie> allCookies = flowCookieRepository.findAll();
        FlowCookie foundCookie = allCookies.iterator().next();
        flowCookieRepository.remove(foundCookie);

        assertEquals(0, flowCookieRepository.findAll().size());
    }

    private FlowCookie createFlowCookie() {
        FlowCookie cookie = FlowCookie.builder()
                .unmaskedCookie(TEST_COOKIE)
                .flowId(TEST_FLOW_ID)
                .build();
        return flowCookieRepository.add(cookie);
    }
}
