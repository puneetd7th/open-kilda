/* Copyright 2018 Telstra Open Source
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

package org.openkilda.testing.service.northbound;

import org.openkilda.messaging.Utils;
import org.openkilda.messaging.payload.flow.FlowIdStatusPayload;
import org.openkilda.model.SwitchId;
import org.openkilda.northbound.dto.v2.flows.FlowPatchV2;
import org.openkilda.northbound.dto.v2.flows.FlowRequestV2;
import org.openkilda.northbound.dto.v2.flows.FlowRerouteResponseV2;
import org.openkilda.northbound.dto.v2.flows.FlowResponseV2;
import org.openkilda.northbound.dto.v2.switches.PortHistoryResponse;
import org.openkilda.northbound.dto.v2.switches.PortPropertiesDto;
import org.openkilda.northbound.dto.v2.switches.PortPropertiesResponse;
import org.openkilda.northbound.dto.v2.switches.SwitchConnectedDevicesResponse;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class NorthboundServiceV2Impl implements NorthboundServiceV2 {

    @Autowired
    @Qualifier("northboundRestTemplate")
    private RestTemplate restTemplate;

    private DateFormat dateFormat = new SimpleDateFormat(StdDateFormat.DATE_FORMAT_STR_ISO8601);

    @Override
    public FlowResponseV2 getFlow(String flowId) {
        return restTemplate.exchange("/api/v2/flows/{flow_id}", HttpMethod.GET,
                new HttpEntity(buildHeadersWithCorrelationId()), FlowResponseV2.class, flowId).getBody();
    }

    @Override
    public List<FlowResponseV2> getAllFlows() {
        FlowResponseV2[] flows = restTemplate.exchange("/api/v2/flows", HttpMethod.GET,
                new HttpEntity(buildHeadersWithCorrelationId()), FlowResponseV2[].class).getBody();
        return Arrays.asList(flows);
    }

    @Override
    public FlowIdStatusPayload getFlowStatus(String flowId) {
        try {
            return restTemplate.exchange("/api/v2/flows/status/{flow_id}", HttpMethod.GET, new HttpEntity(
                    buildHeadersWithCorrelationId()), FlowIdStatusPayload.class, flowId).getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw ex;
            }
            return null;
        }
    }

    @Override
    public FlowResponseV2 addFlow(FlowRequestV2 request) {
        HttpEntity<FlowRequestV2> httpEntity = new HttpEntity<>(request, buildHeadersWithCorrelationId());
        return restTemplate.exchange("/api/v2/flows", HttpMethod.POST, httpEntity, FlowResponseV2.class).getBody();
    }

    @Override
    public FlowResponseV2 updateFlow(String flowId, FlowRequestV2 request) {
        HttpEntity<FlowRequestV2> httpEntity = new HttpEntity<>(request, buildHeadersWithCorrelationId());
        return restTemplate.exchange("/api/v2/flows/{flow_id}", HttpMethod.PUT, httpEntity, FlowResponseV2.class,
                flowId).getBody();
    }

    @Override
    public FlowResponseV2 deleteFlow(String flowId) {
        return restTemplate.exchange("/api/v2/flows/{flow_id}", HttpMethod.DELETE,
                new HttpEntity(buildHeadersWithCorrelationId()), FlowResponseV2.class, flowId).getBody();
    }

    @Override
    public FlowRerouteResponseV2 rerouteFlow(String flowId) {
        return restTemplate.exchange("/api/v2/flows/{flow_id}/reroute", HttpMethod.POST,
                new HttpEntity<>(buildHeadersWithCorrelationId()), FlowRerouteResponseV2.class, flowId).getBody();
    }

    @Override
    public FlowResponseV2 partialUpdate(String flowId, FlowPatchV2 patch) {
        return restTemplate.exchange("/api/v2/flows/{flow_id}", HttpMethod.PATCH,
                new HttpEntity<>(patch, buildHeadersWithCorrelationId()), FlowResponseV2.class, flowId)
                .getBody();
    }

    @Override
    public SwitchConnectedDevicesResponse getConnectedDevices(SwitchId switchId) {
        return getConnectedDevices(switchId, null);
    }

    @Override
    public SwitchConnectedDevicesResponse getConnectedDevices(SwitchId switchId, Date since) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("/api/v2/switches/{switch_id}/devices");
        if (since != null) {
            uriBuilder.queryParam("since", dateFormat.format(since));
        }
        return restTemplate.exchange(uriBuilder.build().toString(), HttpMethod.GET,
                new HttpEntity(buildHeadersWithCorrelationId()), SwitchConnectedDevicesResponse.class,
                switchId).getBody();
    }

    @Override
    public List<PortHistoryResponse> getPortHistory(SwitchId switchId, Integer port) {
        return getPortHistory(switchId, port, null, null);
    }

    @Override
    public List<PortHistoryResponse> getPortHistory(SwitchId switchId, Integer port, Long timeFrom, Long timeTo) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
                "/api/v2/switches/{switch_id}/ports/{port}/history");
        if (timeFrom != null) {
            uriBuilder.queryParam("timeFrom", dateFormat.format(new Date(timeFrom)));
        }
        if (timeTo != null) {
            uriBuilder.queryParam("timeTo", dateFormat.format(new Date(timeTo)));
        }

        PortHistoryResponse[] portHistory = restTemplate.exchange(uriBuilder.build().toString(), HttpMethod.GET,
                new HttpEntity(buildHeadersWithCorrelationId()), PortHistoryResponse[].class, switchId, port).getBody();
        return Arrays.asList(portHistory);
    }

    @Override
    public PortPropertiesResponse getPortProperties(SwitchId switchId, Integer port) {
        return restTemplate.exchange("/api/v2/switches/{switch_id}/ports/{port}/properties", HttpMethod.GET,
                new HttpEntity(buildHeadersWithCorrelationId()), PortPropertiesResponse.class,
                switchId, port).getBody();
    }

    @Override
    public PortPropertiesResponse updatePortProperties(SwitchId switchId, Integer port, PortPropertiesDto payload) {
        log.debug("Update port property. Switch: {}, port: {}, enableDiscovery: {}.", switchId, port,
                payload.isDiscoveryEnabled());
        return restTemplate.exchange("/api/v2/switches/{switch_id}/ports/{port}/properties", HttpMethod.PUT,
                new HttpEntity<>(payload, buildHeadersWithCorrelationId()), PortPropertiesResponse.class,
                switchId, port).getBody();
    }

    private HttpHeaders buildHeadersWithCorrelationId() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Utils.CORRELATION_ID, String.valueOf(System.currentTimeMillis()));
        return headers;
    }
}
