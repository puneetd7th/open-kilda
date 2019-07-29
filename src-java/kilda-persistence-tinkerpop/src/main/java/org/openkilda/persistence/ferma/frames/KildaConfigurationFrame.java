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

import org.openkilda.model.FlowEncapsulationType;
import org.openkilda.model.KildaConfiguration;
import org.openkilda.model.KildaConfiguration.KildaConfigurationData;
import org.openkilda.model.PathComputationStrategy;
import org.openkilda.persistence.ConstraintViolationException;
import org.openkilda.persistence.ferma.frames.converters.FlowEncapsulationTypeConverter;
import org.openkilda.persistence.ferma.frames.converters.PathComputationStrategyConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.annotations.Property;

public abstract class KildaConfigurationFrame extends KildaBaseVertexFrame implements KildaConfigurationData {
    public static final String FRAME_LABEL = "kilda_configuration";

    @Override
    public FlowEncapsulationType getFlowEncapsulationType() {
        String flowEncapsulationType = getProperty("flow_encapsulation_type");
        if (flowEncapsulationType != null) {
            return FlowEncapsulationTypeConverter.INSTANCE.map(flowEncapsulationType);
        } else {
            return KildaConfiguration.DEFAULTS.getFlowEncapsulationType();
        }
    }

    @Override
    public void setFlowEncapsulationType(FlowEncapsulationType flowEncapsulationType) {
        setProperty("flow_encapsulation_type", FlowEncapsulationTypeConverter.INSTANCE.map(flowEncapsulationType));
    }

    @Override
    public Boolean getUseMultiTable() {
        Boolean useMultiTable = getProperty("use_multi_table");
        return useMultiTable != null ? useMultiTable : KildaConfiguration.DEFAULTS.getUseMultiTable();
    }

    @Override
    @Property("use_multi_table")
    public abstract void setUseMultiTable(Boolean useMultiTable);

    @Override
    public PathComputationStrategy getPathComputationStrategy() {
        String pathComputationStrategy = getProperty("path_computation_strategy");
        if (pathComputationStrategy != null) {
            return PathComputationStrategyConverter.INSTANCE.map(pathComputationStrategy);
        } else {
            return KildaConfiguration.DEFAULTS.getPathComputationStrategy();
        }
    }

    @Override
    public void setPathComputationStrategy(PathComputationStrategy pathComputationStrategy) {
        setProperty("path_computation_strategy",
                PathComputationStrategyConverter.INSTANCE.map(pathComputationStrategy));
    }

    public static KildaConfigurationFrame create(FramedGraph framedGraph, KildaConfigurationData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL).count())
                .getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a duplicated vertex " + FRAME_LABEL);
        }

        KildaConfigurationFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL,
                KildaConfigurationFrame.class);
        KildaConfiguration.KildaConfigurationCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
