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

package org.openkilda.persistence.repositories;

import org.openkilda.persistence.repositories.history.FlowDumpRepository;
import org.openkilda.persistence.repositories.history.FlowEventRepository;
import org.openkilda.persistence.repositories.history.FlowHistoryRepository;
import org.openkilda.persistence.repositories.history.PortHistoryRepository;

/**
 * Factory to obtain {@link Repository} instances.
 */
public interface RepositoryFactory {
    FlowCookieRepository getFlowCookieRepository();

    FlowMeterRepository getFlowMeterRepository();

    FlowPathRepository getFlowPathRepository();

    FlowRepository getFlowRepository();

    IslRepository getIslRepository();

    LinkPropsRepository getLinkPropsRepository();

    SwitchRepository getSwitchRepository();

    TransitVlanRepository getTransitVlanRepository();

    VxlanRepository getVxlanRepository();

    FeatureTogglesRepository getFeatureTogglesRepository();

    FlowEventRepository getFlowEventRepository();

    FlowHistoryRepository getFlowHistoryRepository();

    FlowDumpRepository getFlowDumpRepository();

    BfdSessionRepository getBfdSessionRepository();

    KildaConfigurationRepository getKildaConfigurationRepository();

    SwitchPropertiesRepository getSwitchPropertiesRepository();

    SwitchConnectedDeviceRepository getSwitchConnectedDeviceRepository();

    PortHistoryRepository getPortHistoryRepository();

    PortPropertiesRepository getPortPropertiesRepository();

    PathSegmentRepository getPathSegmentRepository();
}
