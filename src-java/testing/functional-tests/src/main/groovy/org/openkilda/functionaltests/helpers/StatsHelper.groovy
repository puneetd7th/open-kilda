package org.openkilda.functionaltests.helpers

import org.openkilda.testing.service.otsdb.OtsdbQueryService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class StatsHelper {
    private static int STATS_INTERVAL = 65
    @Autowired
    OtsdbQueryService otsdb
    @Value('${opentsdb.metric.prefix}')
    String metricPrefix

    void verifyFlowWritesStats(String flowId, Date from = 10.seconds.ago) {
        Wrappers.wait(STATS_INTERVAL) {
            def dps = otsdb.query(from, metricPrefix + "flow.raw.bytes", [flowid: flowId]).dps
            assert dps.values().any { it > 0 }, flowId
        }
    }
}
