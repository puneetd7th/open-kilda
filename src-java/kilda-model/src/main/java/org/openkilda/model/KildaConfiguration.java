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

package org.openkilda.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;

@ToString
public class KildaConfiguration implements CompositeDataEntity<KildaConfiguration.KildaConfigurationData> {
    public static final KildaConfiguration DEFAULTS = new KildaConfiguration(KildaConfigurationDataImpl.builder()
            .flowEncapsulationType(FlowEncapsulationType.TRANSIT_VLAN)
            .useMultiTable(false)
            .pathComputationStrategy(PathComputationStrategy.COST)
            .build());

    @Getter
    @Delegate
    @JsonIgnore
    private KildaConfigurationData data;

    /**
     * No args constructor for deserialization purpose.
     */
    private KildaConfiguration() {
        data = new KildaConfigurationDataImpl();
    }

    @Builder
    public KildaConfiguration(FlowEncapsulationType flowEncapsulationType, Boolean useMultiTable,
                              PathComputationStrategy pathComputationStrategy) {
        data = KildaConfigurationDataImpl.builder().flowEncapsulationType(flowEncapsulationType)
                .useMultiTable(useMultiTable)
                .pathComputationStrategy(pathComputationStrategy).build();
    }

    public KildaConfiguration(@NonNull KildaConfigurationData data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KildaConfiguration that = (KildaConfiguration) o;
        return new EqualsBuilder()
                .append(getFlowEncapsulationType(), that.getFlowEncapsulationType())
                .append(getUseMultiTable(), that.getUseMultiTable())
                .append(getPathComputationStrategy(), that.getPathComputationStrategy())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getFlowEncapsulationType())
                .append(getUseMultiTable())
                .append(getPathComputationStrategy())
                .toHashCode();
    }

    /**
     * Defines persistable data of the KildaConfiguration.
     */
    public interface KildaConfigurationData {
        FlowEncapsulationType getFlowEncapsulationType();

        void setFlowEncapsulationType(FlowEncapsulationType flowEncapsulationType);

        Boolean getUseMultiTable();

        void setUseMultiTable(Boolean useMultiTable);

        PathComputationStrategy getPathComputationStrategy();

        void setPathComputationStrategy(PathComputationStrategy pathComputationStrategy);
    }

    /**
     * POJO implementation of KildaConfigurationData.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static final class KildaConfigurationDataImpl implements KildaConfigurationData, Serializable {
        private static final long serialVersionUID = 1L;
        FlowEncapsulationType flowEncapsulationType;
        Boolean useMultiTable;
        PathComputationStrategy pathComputationStrategy;
    }

    /**
     * A cloner for KildaConfiguration entity.
     */
    @Mapper
    public interface KildaConfigurationCloner {
        KildaConfigurationCloner INSTANCE = Mappers.getMapper(KildaConfigurationCloner.class);

        void copy(KildaConfigurationData source, @MappingTarget KildaConfigurationData target);
    }
}
