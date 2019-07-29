/* Copyright 2020 Telstra Open Source
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

package org.openkilda.persistence.ferma.frames;

import org.openkilda.model.FeatureToggles;
import org.openkilda.model.FeatureToggles.FeatureTogglesData;
import org.openkilda.persistence.ConstraintViolationException;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;

public abstract class FeatureTogglesFrame extends KildaBaseVertexFrame implements FeatureTogglesData {
    public static final String FRAME_LABEL = "feature_toggles";

    @Override
    @Property("flows_reroute_on_isl_discovery_enabled")
    public abstract Boolean getFlowsRerouteOnIslDiscoveryEnabled();

    @Override
    @Property("flows_reroute_on_isl_discovery_enabled")
    public abstract void setFlowsRerouteOnIslDiscoveryEnabled(Boolean flowsRerouteOnIslDiscoveryEnabled);

    @Override
    @Property("create_flow_enabled")
    public abstract Boolean getCreateFlowEnabled();

    @Override
    @Property("create_flow_enabled")
    public abstract void setCreateFlowEnabled(Boolean createFlowEnabled);

    @Override
    @Property("update_flow_enabled")
    public abstract Boolean getUpdateFlowEnabled();

    @Override
    @Property("update_flow_enabled")
    public abstract void setUpdateFlowEnabled(Boolean updateFlowEnabled);

    @Override
    @Property("delete_flow_enabled")
    public abstract Boolean getDeleteFlowEnabled();

    @Override
    @Property("delete_flow_enabled")
    public abstract void setDeleteFlowEnabled(Boolean deleteFlowEnabled);

    @Override
    @Property("push_flow_enabled")
    public abstract Boolean getPushFlowEnabled();

    @Override
    @Property("push_flow_enabled")
    public abstract void setPushFlowEnabled(Boolean pushFlowEnabled);

    @Override
    @Property("unpush_flow_enabled")
    public abstract Boolean getUnpushFlowEnabled();

    @Override
    @Property("unpush_flow_enabled")
    public abstract void setUnpushFlowEnabled(Boolean unpushFlowEnabled);

    @Override
    @Property("use_bfd_for_isl_integrity_check")
    public abstract Boolean getUseBfdForIslIntegrityCheck();

    @Override
    @Property("use_bfd_for_isl_integrity_check")
    public abstract void setUseBfdForIslIntegrityCheck(Boolean useBfdForIslIntegrityCheck);

    @Override
    @Property("floodlight_route_periodic_sync")
    public abstract Boolean getFloodlightRoutePeriodicSync();

    @Override
    @Property("floodlight_route_periodic_sync")
    public abstract void setFloodlightRoutePeriodicSync(Boolean floodlightRoutePeriodicSync);

    @Override
    @Property("flows_reroute_via_flow_hs")
    public abstract Boolean getFlowsRerouteViaFlowHs();

    @Override
    @Property("flows_reroute_via_flow_hs")
    public abstract void setFlowsRerouteViaFlowHs(Boolean flowsRerouteViaFlowHs);

    @Override
    @Property("flows_reroute_using_default_encap_type")
    public abstract Boolean getFlowsRerouteUsingDefaultEncapType();

    @Override
    @Property("flows_reroute_using_default_encap_type")
    public abstract void setFlowsRerouteUsingDefaultEncapType(Boolean flowsRerouteUsingDefaultEncapType);

    public static FeatureTogglesFrame create(FramedGraph framedGraph, FeatureTogglesData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL).count())
                .getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a duplicated vertex " + FRAME_LABEL);
        }

        FeatureTogglesFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                FeatureTogglesFrame.class);
        FeatureToggles.FeatureTogglesCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
