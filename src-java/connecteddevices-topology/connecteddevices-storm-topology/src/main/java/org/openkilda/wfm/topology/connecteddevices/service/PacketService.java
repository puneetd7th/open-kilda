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

package org.openkilda.wfm.topology.connecteddevices.service;

import static org.openkilda.model.ConnectedDeviceType.ARP;
import static org.openkilda.model.ConnectedDeviceType.LLDP;
import static org.openkilda.model.FlowEndpoint.makeVlanStack;
import static org.openkilda.model.cookie.Cookie.ARP_INGRESS_COOKIE;
import static org.openkilda.model.cookie.Cookie.ARP_INPUT_PRE_DROP_COOKIE;
import static org.openkilda.model.cookie.Cookie.ARP_POST_INGRESS_COOKIE;
import static org.openkilda.model.cookie.Cookie.ARP_POST_INGRESS_ONE_SWITCH_COOKIE;
import static org.openkilda.model.cookie.Cookie.ARP_POST_INGRESS_VXLAN_COOKIE;
import static org.openkilda.model.cookie.Cookie.ARP_TRANSIT_COOKIE;
import static org.openkilda.model.cookie.Cookie.LLDP_INGRESS_COOKIE;
import static org.openkilda.model.cookie.Cookie.LLDP_INPUT_PRE_DROP_COOKIE;
import static org.openkilda.model.cookie.Cookie.LLDP_POST_INGRESS_COOKIE;
import static org.openkilda.model.cookie.Cookie.LLDP_POST_INGRESS_ONE_SWITCH_COOKIE;
import static org.openkilda.model.cookie.Cookie.LLDP_POST_INGRESS_VXLAN_COOKIE;
import static org.openkilda.model.cookie.Cookie.LLDP_TRANSIT_COOKIE;

import org.openkilda.messaging.info.event.ArpInfoData;
import org.openkilda.messaging.info.event.ConnectedDevicePacketBase;
import org.openkilda.messaging.info.event.LldpInfoData;
import org.openkilda.model.Flow;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchConnectedDevice;
import org.openkilda.model.SwitchId;
import org.openkilda.model.TransitVlan;
import org.openkilda.model.Vxlan;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.repositories.FlowRepository;
import org.openkilda.persistence.repositories.SwitchConnectedDeviceRepository;
import org.openkilda.persistence.repositories.SwitchRepository;
import org.openkilda.persistence.repositories.TransitVlanRepository;
import org.openkilda.persistence.repositories.VxlanRepository;

import com.google.common.annotations.VisibleForTesting;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PacketService {
    public static final int FULL_PORT_VLAN = 0;

    private final TransactionManager transactionManager;
    private final SwitchRepository switchRepository;
    private final SwitchConnectedDeviceRepository switchConnectedDeviceRepository;
    private final TransitVlanRepository transitVlanRepository;
    private final VxlanRepository vxlanRepository;
    private final FlowRepository flowRepository;

    public PacketService(PersistenceManager persistenceManager) {
        transactionManager = persistenceManager.getTransactionManager();
        switchRepository = persistenceManager.getRepositoryFactory().createSwitchRepository();
        switchConnectedDeviceRepository = persistenceManager.getRepositoryFactory()
                .createSwitchConnectedDeviceRepository();
        transitVlanRepository = persistenceManager.getRepositoryFactory().createTransitVlanRepository();
        vxlanRepository = persistenceManager.getRepositoryFactory().createVxlanRepository();
        flowRepository = persistenceManager.getRepositoryFactory().createFlowRepository();
    }

    /**
     * Handle LLDP info data.
     */
    public void handleLldpData(LldpInfoData data) {
        transactionManager.doInTransaction(() -> {

            FlowRelatedData flowRelatedData = findFlowRelatedData(data);
            if (flowRelatedData == null) {
                return;
            }

            SwitchConnectedDevice device = getOrBuildLldpDevice(data, flowRelatedData.vlans);

            if (device == null) {
                return;
            }

            device.setTtl(data.getTtl());
            device.setPortDescription(data.getPortDescription());
            device.setSystemName(data.getSystemName());
            device.setSystemDescription(data.getSystemDescription());
            device.setSystemCapabilities(data.getSystemCapabilities());
            device.setManagementAddress(data.getManagementAddress());
            device.setTimeLastSeen(Instant.ofEpochMilli(data.getTimestamp()));
            device.setFlowId(flowRelatedData.flowId);
            device.setSource(flowRelatedData.source);

            switchConnectedDeviceRepository.createOrUpdate(device);
        });
    }

    /**
     * Handle Arp info data.
     */
    public void handleArpData(ArpInfoData data) {
        transactionManager.doInTransaction(() -> {

            FlowRelatedData flowRelatedData = findFlowRelatedData(data);
            if (flowRelatedData == null) {
                return;
            }

            SwitchConnectedDevice device = getOrBuildArpDevice(data, flowRelatedData.vlans);

            if (device == null) {
                return;
            }

            device.setTimeLastSeen(Instant.ofEpochMilli(data.getTimestamp()));
            device.setFlowId(flowRelatedData.flowId);
            device.setSource(flowRelatedData.source);

            switchConnectedDeviceRepository.createOrUpdate(device);
        });
    }

    private FlowRelatedData findFlowRelatedData(ConnectedDevicePacketBase data) {
        long cookie = data.getCookie();
        if (cookie == LLDP_POST_INGRESS_COOKIE
                || cookie == ARP_POST_INGRESS_COOKIE) {
            return findFlowRelatedDataForVlanFlow(data);
        } else if (cookie == LLDP_POST_INGRESS_VXLAN_COOKIE
                || cookie == ARP_POST_INGRESS_VXLAN_COOKIE) {
            return findFlowRelatedDataForVxlanFlow(data);
        } else if (cookie == LLDP_POST_INGRESS_ONE_SWITCH_COOKIE
                || cookie == ARP_POST_INGRESS_ONE_SWITCH_COOKIE) {
            return findFlowRelatedDataForOneSwitchFlow(data);
        } else if (cookie == LLDP_INPUT_PRE_DROP_COOKIE
                || cookie == LLDP_INGRESS_COOKIE
                || cookie == LLDP_TRANSIT_COOKIE
                || cookie == ARP_INPUT_PRE_DROP_COOKIE
                || cookie == ARP_INGRESS_COOKIE
                || cookie == ARP_TRANSIT_COOKIE) {
            return new FlowRelatedData(data.getVlans(), null, null);
        }
        log.warn("Got {} packet from unknown rule with cookie {}. Switch {}, port {}, vlans {}",
                getPacketName(data), data.getCookie(), data.getSwitchId(), data.getPortNumber(), data.getVlans());
        return null;
    }

    @VisibleForTesting
    FlowRelatedData findFlowRelatedDataForVlanFlow(ConnectedDevicePacketBase data) {
        if (data.getVlans().isEmpty()) {
            log.warn("Got {} packet without transit VLAN: {}", getPacketName(data), data);
            return null;
        }
        int transitVlan = data.getVlans().get(0);
        Flow flow = findFlowByTransitVlan(transitVlan);

        if (flow == null) {
            return null;
        }

        boolean source;
        List<Integer> vlans;

        if (data.getSwitchId().equals(flow.getSrcSwitch().getSwitchId())) {
            source = true;
            vlans = makeVlanStack(flow.getSrcVlan(), flow.getSrcInnerVlan());
            // case 1:  customer vlan 0 ==> src vlan 0, transit vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan 1 ==> src vlan 0, transit vlan 2 ==> output vlan 2, vlans in packet: [2, 1]
            // case 3:  customer vlan 1 ==> src vlan 1, transit vlan 2 ==> output vlan 2, vlans in packet: [2]
        } else if (data.getSwitchId().equals(flow.getDestSwitch().getSwitchId())) {
            source = false;
            vlans = makeVlanStack(flow.getDestVlan(), flow.getDestInnerVlan());
            // case 1:  customer vlan 0 ==> dst vlan 0, transit vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan 1 ==> dst vlan 0, transit vlan 2 ==> output vlan 2, vlans in packet: [2, 1]
            // case 3:  customer vlan 1 ==> dst vlan 1, transit vlan 2 ==> output vlan 2, vlans in packet: [2]
        } else {
            log.warn("Got {} packet from Flow {} on non-src/non-dst switch {}. Transit vlan: {}",
                    getPacketName(data), flow.getFlowId(), data.getSwitchId(), transitVlan);
            return null;
        }

        vlans.addAll(subList(data.getVlans(), 1));
        return new FlowRelatedData(vlans, flow.getFlowId(), source);
    }

    @VisibleForTesting
    FlowRelatedData findFlowRelatedDataForVxlanFlow(ConnectedDevicePacketBase data) {
        if (data.getVni() == null) {
            log.info("Couldn't find flow related data by null VNI. Packet data {}", data);
        }
        Flow flow = findFlowByVxlan(data.getVni());

        if (flow == null) {
            return null;
        }

        boolean source;
        List<Integer> vlans;

        if (data.getSwitchId().equals(flow.getSrcSwitch().getSwitchId())) {
            vlans = makeVlanStack(flow.getSrcVlan(), flow.getSrcInnerVlan());
            source = true;
        } else if (data.getSwitchId().equals(flow.getDestSwitch().getSwitchId())) {
            vlans = makeVlanStack(flow.getDestVlan(), flow.getDestInnerVlan());
            source = false;
        } else {
            log.warn("Got {} packet from Flow {} on non-src/non-dst switch {}. Port number {}, vlans {}",
                    getPacketName(data), flow.getFlowId(), data.getSwitchId(), data.getPortNumber(), data.getVlans());
            return null;
        }

        vlans.addAll(data.getVlans());
        return new FlowRelatedData(vlans, flow.getFlowId(), source);
    }

    @VisibleForTesting
    FlowRelatedData findFlowRelatedDataForOneSwitchFlow(ConnectedDevicePacketBase data) {
        // top vlan with which we got LLDP packet in Floodlight.
        int outputVlan = data.getVlans().isEmpty() ? 0 : data.getVlans().get(0);
        // second vlan with which we got LLDP packet in Floodlight. Exists only for some full port flows.
        int customerVlan = data.getVlans().size() > 1 ? data.getVlans().get(1) : 0;
        Flow flow = getFlowBySwitchIdInPortAndOutVlan(
                data.getSwitchId(), data.getPortNumber(), outputVlan, getPacketName(data));

        if (flow == null) {
            return null;
        }

        if (!flow.isOneSwitchFlow()) {
            log.warn("Found NOT one switch flow {} by SwitchId {}, port number {}, vlan {} from {} packet",
                    flow.getFlowId(), data.getSwitchId(), data.getPortNumber(), outputVlan, getPacketName(data));
            return null;
        }

        List<Integer> srcVlans = makeVlanStack(flow.getSrcVlan(), flow.getSrcInnerVlan());
        List<Integer> dstVlans = makeVlanStack(flow.getDestVlan(), flow.getDestInnerVlan());

        if (flow.getSrcPort() == flow.getDestPort()) {
            return getOneSwitchOnePortFlowRelatedData(flow.getFlowId(), srcVlans, dstVlans, data);
        }

        boolean source;
        List<Integer> vlans = new ArrayList<>();

        if (data.getPortNumber() == flow.getSrcPort()) {
            source = true;
            vlans.addAll(srcVlans);
            vlans.addAll(subList(data.getVlans(), dstVlans.size()));
            // case 1:  customer vlan 0 ==> src vlan 0, dst vlan 0 ==> output vlan 0, vlans in packet: []
            // case 2:  customer vlan 1 ==> src vlan 0, dst vlan 0 ==> output vlan 1, vlans in packet: [1]
            //return new FlowRelatedData(outputVlan, flow.getFlowId(), true);
            // case 1:  customer vlan 0 ==> src vlan 0, dst vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan 1 ==> src vlan 0, dst vlan 2 ==> output vlan 2, vlans in packet: [2, 1]
            //return new FlowRelatedData(customerVlan, flow.getFlowId(), true);
            // case 1:  customer vlan 1 ==> src vlan 1, dst vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan [1,2,3] ==> src vlan [1,2] dst vlan 2 ==> output vlan 2, vlans in packet: [2,3]
            //return new FlowRelatedData(flow.getSrcVlan(), flow.getFlowId(), true);
        } else if (data.getPortNumber() == flow.getDestPort()) {
            source = false;
            vlans.addAll(dstVlans);
            vlans.addAll(subList(data.getVlans(), srcVlans.size()));
            // case 1:  customer vlan 0 ==> dst vlan 0, src vlan 0 ==> output vlan 0, vlans in packet: []
            // case 1:  customer vlan 1 ==> dst vlan 0, src vlan 0 ==> output vlan 1, vlans in packet: [1]
            //return new FlowRelatedData(outputVlan, flow.getFlowId(), false);
            // case 1:  customer vlan 0 ==> dst vlan 0, src vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan 1 ==> dst vlan 0, src vlan 2 ==> output vlan 2, vlans in packet: [2, 1]
            //return new FlowRelatedData(customerVlan, flow.getFlowId(), false);
            // case 1:  customer vlan 1 ==> dst vlan 1, src vlan 2 ==> output vlan 2, vlans in packet: [2]
            //return new FlowRelatedData(flow.getDestVlan(), flow.getFlowId(), false);
        } else {
            log.warn("Got LLDP packet from one switch flow {} with non-src/non-dst vlan {}. SwitchId {}, "
                    + "port number {}", flow.getFlowId(), outputVlan, data.getSwitchId(), data.getPortNumber());
            return null;
        }
        return new FlowRelatedData(vlans, flow.getFlowId(), source);
    }

    private FlowRelatedData getOneSwitchOnePortFlowRelatedData(
            String flowId, List<Integer> srcVlans, List<Integer> dstVlans, ConnectedDevicePacketBase data) {
        List<Integer> vlans = new ArrayList<>();
        boolean source;

        if (startsWith(data.getVlans(), dstVlans)) {
            source = true;
            vlans.addAll(srcVlans);
            vlans.addAll(subList(data.getVlans(), dstVlans.size()));
            // case 1:  customer vlan 0 ==> src vlan 0, dst vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan 1 ==> src vlan 0, dst vlan 2 ==> output vlan 2, vlans in packet: [2, 1]
            //return new FlowRelatedData(customerVlan, flow.getFlowId(), true);
            // case 1:  customer vlan 1 ==> src vlan 1, dst vlan 2 ==> output vlan 2, vlans in packet: [2]
            //return new FlowRelatedData(flow.getSrcVlan(), flow.getFlowId(), true);
        } else if (startsWith(data.getVlans(), srcVlans)) {
            source = false;
            vlans.addAll(dstVlans);
            vlans.addAll(subList(data.getVlans(), srcVlans.size()));
            // case 1:  customer vlan 0 ==> dst vlan 0, src vlan 2 ==> output vlan 2, vlans in packet: [2]
            // case 2:  customer vlan 1 ==> dst vlan 0, src vlan 2 ==> output vlan 2, vlans in packet: [2, 1]
            //return new FlowRelatedData(customerVlan, flow.getFlowId(), false);
            // case 1:  customer vlan 1 ==> dst vlan 1, src vlan 2 ==> output vlan 2, vlans in packet: [2]
            //return new FlowRelatedData(flow.getDestVlan(), flow.getFlowId(), false);
        } else {
            log.warn("Got {} data for one switch one Flow with unknown vlans {}. Flow {} Data {}",
                    getPacketName(data), data.getVlans(), flowId, data);
            return null;
        }
        return new FlowRelatedData(vlans, flowId, source);
    }

    private Flow findFlowByTransitVlan(int vlan) {
        Optional<TransitVlan> transitVlan = transitVlanRepository.findByVlan(vlan);

        if (!transitVlan.isPresent()) {
            log.info("Couldn't find flow encapsulation resources by Transit vlan '{}", vlan);
            return null;
        }
        return findFlowById(transitVlan.get().getFlowId());
    }

    private Flow findFlowByVxlan(int vni) {
        Optional<Vxlan> vxlan = vxlanRepository.findByVni(vni);

        if (!vxlan.isPresent()) {
            log.info("Couldn't find flow encapsulation resources by VXLAN '{}", vni);
            return null;
        }
        return findFlowById(vxlan.get().getFlowId());
    }

    private Flow findFlowById(String flowId) {
        Optional<Flow> flow = flowRepository.findByIdWithEndpoints(flowId);
        if (!flow.isPresent()) {
            log.warn("Couldn't find flow by flow ID '{}", flowId);
            return null;
        }
        return flow.get();
    }

    private Flow getFlowBySwitchIdInPortAndOutVlan(SwitchId switchId, int inPort, int outVlan, String packetName) {
        Optional<Flow> flow = flowRepository.findOneSwitchFlowBySwitchIdInPortAndOutVlan(switchId, inPort, outVlan);

        if (flow.isPresent()) {
            return flow.get();
        } else {
            // may be it's a full port flow
            Optional<Flow> fullPortFlow = flowRepository.findOneSwitchFlowBySwitchIdInPortAndOutVlan(
                    switchId, inPort, FULL_PORT_VLAN);
            if (fullPortFlow.isPresent()) {
                return fullPortFlow.get();
            } else {
                log.warn("Couldn't find Flow for {} packet by: Switch {}, InPort {}, OutVlan {}",
                        packetName, switchId, inPort, outVlan);
                return null;
            }
        }
    }

    private SwitchConnectedDevice getOrBuildLldpDevice(LldpInfoData data, List<Integer> vlans) {
        int outerVlan = vlans.size() > 0 ? vlans.get(0) : 0;
        int innerVlan = vlans.size() > 1 ? vlans.get(1) : 0;

        Optional<SwitchConnectedDevice> device = switchConnectedDeviceRepository
                .findLldpByUniqueFieldCombination(
                        data.getSwitchId(), data.getPortNumber(), outerVlan, innerVlan, data.getMacAddress(),
                        data.getChassisId(), data.getPortId());

        if (device.isPresent()) {
            return device.get();
        }

        Optional<Switch> sw = switchRepository.findById(data.getSwitchId());

        if (!sw.isPresent()) {
            log.warn("Got LLDP packet from non existent switch {}. Port number '{}', outerVlan '{}', innerVlan '{}', "
                            + "mac address '{}', chassis id '{}', port id '{}'",
                    data.getSwitchId(), data.getPortNumber(), outerVlan, innerVlan, data.getMacAddress(),
                    data.getChassisId(), data.getPortId());
            return null;
        }

        return SwitchConnectedDevice.builder()
                .switchObj(sw.get())
                .portNumber(data.getPortNumber())
                .vlan(outerVlan)
                .innerVlan(innerVlan)
                .macAddress(data.getMacAddress())
                .type(LLDP)
                .chassisId(data.getChassisId())
                .portId(data.getPortId())
                .timeFirstSeen(Instant.ofEpochMilli(data.getTimestamp()))
                .build();
    }

    private SwitchConnectedDevice getOrBuildArpDevice(ArpInfoData data, List<Integer> vlans) {
        int outerVlan = vlans.size() > 0 ? vlans.get(0) : 0;
        int innerVlan = vlans.size() > 1 ? vlans.get(1) : 0;

        Optional<SwitchConnectedDevice> device = switchConnectedDeviceRepository
                .findArpByUniqueFieldCombination(
                        data.getSwitchId(), data.getPortNumber(), outerVlan, innerVlan, data.getMacAddress(),
                        data.getIpAddress());

        if (device.isPresent()) {
            return device.get();
        }

        Optional<Switch> sw = switchRepository.findById(data.getSwitchId());

        if (!sw.isPresent()) {
            log.warn("Got ARP packet from non existent switch {}. Port number '{}', outer vlan '{}', inner vlan '{}', "
                            + "mac address '{}', ip address '{}'", data.getSwitchId(), data.getPortNumber(), outerVlan,
                    innerVlan, data.getMacAddress(), data.getIpAddress());
            return null;
        }

        return SwitchConnectedDevice.builder()
                .switchObj(sw.get())
                .portNumber(data.getPortNumber())
                .vlan(outerVlan)
                .innerVlan(innerVlan)
                .macAddress(data.getMacAddress())
                .type(ARP)
                .ipAddress(data.getIpAddress())
                .timeFirstSeen(Instant.ofEpochMilli(data.getTimestamp()))
                .build();
    }

    private String getPacketName(ConnectedDevicePacketBase data) {
        if (data instanceof LldpInfoData) {
            return "LLDP";
        } else if (data instanceof ArpInfoData) {
            return "ARP";
        } else {
            return "unknown";
        }
    }

    private boolean startsWith(List<Integer> parentList, List<Integer> childList) {
        if (parentList.size() < childList.size()) {
            return false;
        }
        for (int i = 0; i < childList.size(); i++) {
            if (!parentList.get(i).equals(childList.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> subList(List<Integer> list, int from) {
        if (from < 0 || from > list.size()) {
            return new ArrayList<>();
        }
        return list.subList(from, list.size());
    }

    @Value
    static class FlowRelatedData {
        List<Integer> vlans;
        String flowId;
        Boolean source; // device connected to source of Flow or to destination
    }
}
