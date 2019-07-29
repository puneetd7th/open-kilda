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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;

@ToString
public class PortProperties implements CompositeDataEntity<PortProperties.PortPropertiesData> {
    public static final boolean DISCOVERY_ENABLED_DEFAULT = true;

    @Getter
    @Delegate
    @JsonIgnore
    private PortPropertiesData data;

    /**
     * No args constructor for deserialization purpose.
     */
    private PortProperties() {
        data = new PortPropertiesDataImpl();
    }

    public PortProperties(@NonNull PortProperties entityToClone) {
        this();
        PortPropertiesCloner.INSTANCE.copyWithoutSwitch(entityToClone.getData(), data);
        data.setSwitchObj(new Switch(entityToClone.getSwitchObj()));
    }

    @Builder
    public PortProperties(@NonNull Switch switchObj, int port, boolean discoveryEnabled) {
        data = PortPropertiesDataImpl.builder().switchObj(switchObj).port(port)
                .discoveryEnabled(discoveryEnabled).build();
    }

    public PortProperties(@NonNull PortPropertiesData data) {
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
        PortProperties that = (PortProperties) o;
        return new EqualsBuilder()
                .append(getPort(), that.getPort())
                .append(isDiscoveryEnabled(), that.isDiscoveryEnabled())
                .append(getSwitchId(), that.getSwitchId())
                .append(getDiscriminator(), that.getDiscriminator())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getSwitchId())
                .append(getPort())
                .append(isDiscoveryEnabled())
                .append(getDiscriminator())
                .toHashCode();
    }

    /**
     * Defines persistable data of the PortProperties.
     */
    public interface PortPropertiesData {
        SwitchId getSwitchId();

        Switch getSwitchObj();

        void setSwitchObj(Switch switchObj);

        int getPort();

        void setPort(int port);

        boolean isDiscoveryEnabled();

        void setDiscoveryEnabled(boolean discoveryEnabled);

        String getDiscriminator();

        void setDiscriminator(String discriminator);
    }

    /**
     * POJO implementation of PortPropertiesData entity.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static final class PortPropertiesDataImpl implements PortPropertiesData, Serializable {
        private static final long serialVersionUID = 1L;
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        @NonNull Switch switchObj;
        int port;
        @Builder.Default
        boolean discoveryEnabled = DISCOVERY_ENABLED_DEFAULT;
        String discriminator;

        @Override
        public SwitchId getSwitchId() {
            return switchObj.getSwitchId();
        }
    }

    /**
     * A cloner for PortProperties entity.
     */
    @Mapper
    public interface PortPropertiesCloner {
        PortPropertiesCloner INSTANCE = Mappers.getMapper(PortPropertiesCloner.class);

        void copy(PortPropertiesData source, @MappingTarget PortPropertiesData target);

        @Mapping(target = "switchObj", ignore = true)
        void copyWithoutSwitch(PortPropertiesData source, @MappingTarget PortPropertiesData target);
    }
}
