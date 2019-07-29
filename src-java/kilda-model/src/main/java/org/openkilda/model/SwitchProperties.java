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
import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Represents switch properties.
 */
@ToString
public class SwitchProperties implements CompositeDataEntity<SwitchProperties.SwitchPropertiesData> {
    public static Set<FlowEncapsulationType> DEFAULT_FLOW_ENCAPSULATION_TYPES =
            Collections.singleton(FlowEncapsulationType.TRANSIT_VLAN);
    @Getter
    @Delegate
    @JsonIgnore
    private SwitchPropertiesData data;

    /**
     * No args constructor for deserialization purpose.
     */
    private SwitchProperties() {
        data = new SwitchPropertiesDataImpl();
    }

    public SwitchProperties(@NonNull SwitchProperties entityToClone) {
        this();
        SwitchPropertiesCloner.INSTANCE.copyWithoutSwitch(entityToClone.getData(), data);
        data.setSwitchObj(new Switch(entityToClone.getSwitchObj()));
    }

    @Builder
    public SwitchProperties(@NonNull Switch switchObj, Set<FlowEncapsulationType> supportedTransitEncapsulation,
                            boolean multiTable, boolean switchLldp, boolean switchArp) {
        this.data = SwitchPropertiesDataImpl.builder().switchObj(switchObj)
                .supportedTransitEncapsulation(supportedTransitEncapsulation)
                .multiTable(multiTable).switchLldp(switchLldp).switchArp(switchArp).build();
    }

    public SwitchProperties(@NonNull SwitchPropertiesData data) {
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
        SwitchProperties that = (SwitchProperties) o;
        return new EqualsBuilder()
                .append(isMultiTable(), that.isMultiTable())
                .append(isSwitchLldp(), that.isSwitchLldp())
                .append(isSwitchArp(), that.isSwitchArp())
                .append(getSwitchId(), that.getSwitchId())
                .append(getSupportedTransitEncapsulation(), that.getSupportedTransitEncapsulation())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getSwitchId())
                .append(getSupportedTransitEncapsulation())
                .append(isMultiTable())
                .append(isSwitchLldp())
                .append(isSwitchArp())
                .toHashCode();
    }

    /**
     * Defines persistable data of the SwitchProperties.
     */
    public interface SwitchPropertiesData {
        SwitchId getSwitchId();

        Switch getSwitchObj();

        void setSwitchObj(Switch switchObj);

        Set<FlowEncapsulationType> getSupportedTransitEncapsulation();

        void setSupportedTransitEncapsulation(Set<FlowEncapsulationType> supportedTransitEncapsulation);

        boolean isMultiTable();

        void setMultiTable(boolean multiTable);

        boolean isSwitchLldp();

        void setSwitchLldp(boolean switchLldp);

        boolean isSwitchArp();

        void setSwitchArp(boolean switchArp);

        /**
         * Checks whether the feature is set for the switch.
         */
        @VisibleForTesting
        default boolean validateProp(SwitchFeature feature) {
            if (!getSwitchObj().getFeatures().contains(feature)) {
                String message = String.format("Switch %s doesn't support requested feature %s",
                        getSwitchId(), feature);
                throw new IllegalArgumentException(message);
            }
            return true;
        }
    }

    /**
     * POJO implementation of SwitchPropertiesData entity.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static final class SwitchPropertiesDataImpl implements SwitchPropertiesData, Serializable {
        private static final long serialVersionUID = 1L;
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        @NonNull Switch switchObj;
        Set<FlowEncapsulationType> supportedTransitEncapsulation;
        boolean multiTable;
        boolean switchLldp;
        boolean switchArp;

        @Override
        public SwitchId getSwitchId() {
            return switchObj.getSwitchId();
        }

        /**
         * Sets multi-table flag. Validates it against supported features under the hood.
         *
         * @param multiTable target flag
         */
        @Override
        public void setMultiTable(boolean multiTable) {
            if (multiTable) {
                validateProp(SwitchFeature.MULTI_TABLE);
            }
            this.multiTable = multiTable;
        }

        /**
         * Sets allowed transit encapsulations. Validates it against supported features under the hood.
         *
         * @param supportedTransitEncapsulation target supported transit encapsulations.
         */
        @Override
        public void setSupportedTransitEncapsulation(Set<FlowEncapsulationType> supportedTransitEncapsulation) {
            if (supportedTransitEncapsulation != null
                    && supportedTransitEncapsulation.contains(FlowEncapsulationType.VXLAN)) {
                validateProp(SwitchFeature.NOVIFLOW_COPY_FIELD);
            }
            this.supportedTransitEncapsulation = supportedTransitEncapsulation;
        }
    }

    /**
     * A cloner for SwitchProperties entity.
     */
    @Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
    public interface SwitchPropertiesCloner {
        SwitchPropertiesCloner INSTANCE = Mappers.getMapper(SwitchPropertiesCloner.class);

        void copy(SwitchPropertiesData source, @MappingTarget SwitchPropertiesData target);

        @Mapping(target = "switchObj", ignore = true)
        void copyWithoutSwitch(SwitchPropertiesData source, @MappingTarget SwitchPropertiesData target);
    }
}
