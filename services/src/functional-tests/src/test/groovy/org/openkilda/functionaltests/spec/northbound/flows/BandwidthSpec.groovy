package org.openkilda.functionaltests.spec.northbound.flows

import org.openkilda.functionaltests.BaseSpecification
import org.openkilda.functionaltests.helpers.FlowHelper
import org.openkilda.functionaltests.helpers.PathHelper
import org.openkilda.functionaltests.helpers.Wrappers
import org.openkilda.messaging.payload.flow.FlowState
import org.openkilda.testing.model.topology.TopologyDefinition
import org.openkilda.testing.model.topology.TopologyDefinition.Switch
import org.openkilda.testing.service.northbound.NorthboundService
import org.openkilda.testing.service.topology.TopologyEngineService
import org.openkilda.testing.tools.IslUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpClientErrorException

class BandwidthSpec extends BaseSpecification {

    @Autowired
    TopologyDefinition topology
    @Autowired
    FlowHelper flowHelper
    @Autowired
    NorthboundService northboundService
    @Autowired
    TopologyEngineService topologyEngineService
    @Autowired
    PathHelper pathHelper
    @Autowired
    IslUtils islUtils

    def "Available bandwidth on ISLs changes respectively when creating a flow"() {
        given: "Two active not neighboring switches"
        def switches = topology.getActiveSwitches()
        def linksBeforeFlow = northboundService.getAllLinks()
        def (Switch srcSwitch, Switch dstSwitch) = [switches, switches].combinations()
                .findAll { src, dst -> src != dst }.find { Switch src, Switch dst ->
            linksBeforeFlow.every { link ->
                def switchIds = link.path*.switchId
                !(switchIds.contains(src.dpId) && switchIds.contains(dst.dpId))
            }
        }
        assert srcSwitch && dstSwitch

        when: "Create a flow with a valid bandwidth"
        def maximumBandwidth = 1000

        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flow.maximumBandwidth = maximumBandwidth
        northboundService.addFlow(flow)

        then: "Flow is really created and has 'Up' status"
        Wrappers.wait(5) {
            northboundService.getFlowStatus(flow.id).status == FlowState.UP
        }

        and: "Available bandwidth on ISLs is changed in accordance with flow maximum bandwidth"
        def linksAfterFlow = northboundService.getAllLinks()
        def flowPath = PathHelper.convert(northboundService.getFlowPath(flow.id))
        pathHelper.getInvolvedIsls(flowPath).every { link ->
            [link, islUtils.reverseIsl(link)].every {
                def bwBeforeFlow = islUtils.getIslInfo(linksBeforeFlow, it).availableBandwidth
                def bwAfterFlow = islUtils.getIslInfo(linksAfterFlow, it).availableBandwidth
                bwAfterFlow == bwBeforeFlow - maximumBandwidth
            }
        }

        cleanup: "Delete created flow"
        flow?.id && northboundService.deleteFlow(flow.id)
    }

    def "Unable to exceed bandwidth limit on ISL when creating a flow"() {
        given: "Two active switches"
        def switches = topology.getActiveSwitches()
        def (Switch srcSwitch, Switch dstSwitch) = [switches, switches].combinations()
                .find { Switch src, Switch dst -> src != dst }
        assert srcSwitch && dstSwitch

        when: "Create a flow with a bandwidth that exceeds available bandwidth on ISL"
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        def possibleFlowPaths = topologyEngineService.getPaths(srcSwitch.dpId, dstSwitch.dpId)*.path
        def allLinks = northboundService.getAllLinks()
        def involvedBandwidths = []

        possibleFlowPaths.each { path ->
            pathHelper.getInvolvedIsls(path).each { link ->
                involvedBandwidths.add(islUtils.getIslInfo(allLinks, link).availableBandwidth)
            }
        }
        flow.maximumBandwidth = involvedBandwidths.max() + 1
        flow.ignoreBandwidth = false
        northboundService.addFlow(flow)

        then: "Flow is not created because flow path should not be found"
        def exc = thrown(HttpClientErrorException)
        exc.rawStatusCode == 404
    }

    def "Able to exceed bandwidth limit on ISL when creating a flow with ignore_bandwidth = true"() {
        given: "Two active switches"
        def switches = topology.getActiveSwitches()
        def (Switch srcSwitch, Switch dstSwitch) = [switches, switches].combinations()
                .find { Switch src, Switch dst -> src != dst }
        assert srcSwitch && dstSwitch

        when: "Create a flow with a bandwidth that exceeds available bandwidth on ISL (ignore_bandwidth = true)"
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flow.maximumBandwidth = Integer.MAX_VALUE
        flow.ignoreBandwidth = true
        northboundService.addFlow(flow)

        then: "Flow is successfully created"
        Wrappers.wait(5) {
            northboundService.getFlowStatus(flow.id).status == FlowState.UP
        }

        cleanup: "Delete created flow"
        flow?.id && northboundService.deleteFlow(flow.id)
    }
}