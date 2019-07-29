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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;

/**
 * Represents a segment of a flow path.
 */
@ToString
public class PathSegment implements CompositeDataEntity<PathSegment.PathSegmentData> {
    @Getter
    @Delegate
    @JsonIgnore
    private PathSegmentData data;

    /**
     * No args constructor for deserialization purpose.
     */
    private PathSegment() {
        data = new PathSegmentDataImpl();
    }

    public PathSegment(@NonNull PathSegment segmentToClone) {
        this();
        PathSegmentCloner.INSTANCE.copyWithoutSwitches(segmentToClone.getData(), data);
        data.setSrcSwitch(new Switch(segmentToClone.getSrcSwitch()));
        data.setDestSwitch(new Switch(segmentToClone.getDestSwitch()));
    }

    @Builder
    public PathSegment(@NonNull Switch srcSwitch, @NonNull Switch destSwitch, int srcPort, int destPort,
                       boolean srcWithMultiTable, boolean destWithMultiTable, int seqId, Long latency, boolean failed) {
        data = PathSegmentDataImpl.builder().srcSwitch(srcSwitch).destSwitch(destSwitch)
                .srcPort(srcPort).destPort(destPort).srcWithMultiTable(srcWithMultiTable)
                .destWithMultiTable(destWithMultiTable).seqId(seqId).latency(latency).failed(failed).build();
    }

    public PathSegment(@NonNull PathSegmentData data) {
        this.data = data;
    }

    /**
     * Checks whether endpoint belongs to segment or not.
     *
     * @param switchId target switch
     * @param port target port
     * @return result of check
     */
    public boolean containsNode(SwitchId switchId, int port) {
        if (switchId == null) {
            throw new IllegalArgumentException("Switch id must be not null");
        }
        return (switchId.equals(getSrcSwitchId()) && port == getSrcPort())
                || (switchId.equals(getDestSwitchId()) && port == getDestPort());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PathSegment that = (PathSegment) o;
        return new EqualsBuilder()
                .append(getSrcPort(), that.getSrcPort())
                .append(getDestPort(), that.getDestPort())
                .append(isSrcWithMultiTable(), that.isSrcWithMultiTable())
                .append(isDestWithMultiTable(), that.isDestWithMultiTable())
                .append(getSeqId(), that.getSeqId())
                .append(isFailed(), that.isFailed())
                .append(getPathId(), that.getPathId())
                .append(getSrcSwitchId(), that.getSrcSwitchId())
                .append(getDestSwitchId(), that.getDestSwitchId())
                .append(getLatency(), that.getLatency())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getPathId())
                .append(getSrcSwitchId())
                .append(getDestSwitchId())
                .append(getSrcPort())
                .append(getDestPort())
                .append(isSrcWithMultiTable())
                .append(isDestWithMultiTable())
                .append(getSeqId())
                .append(getLatency())
                .append(isFailed())
                .toHashCode();
    }

    /**
     * Defines persistable data of the PathSegment.
     */
    public interface PathSegmentData {
        PathId getPathId();

        FlowPath getPath();

        SwitchId getSrcSwitchId();

        Switch getSrcSwitch();

        void setSrcSwitch(Switch srcSwitch);

        SwitchId getDestSwitchId();

        Switch getDestSwitch();

        void setDestSwitch(Switch destSwitch);

        int getSrcPort();

        void setSrcPort(int srcPort);

        int getDestPort();

        void setDestPort(int destPort);

        boolean isSrcWithMultiTable();

        void setSrcWithMultiTable(boolean srcWithMultiTable);

        boolean isDestWithMultiTable();

        void setDestWithMultiTable(boolean destWithMultiTable);

        int getSeqId();

        void setSeqId(int seqId);

        Long getLatency();

        void setLatency(Long latency);

        boolean isFailed();

        void setFailed(boolean failed);
    }

    /**
     * POJO implementation of PathSegmentData.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static final class PathSegmentDataImpl implements PathSegmentData, Serializable {
        private static final long serialVersionUID = 1L;
        @Setter(AccessLevel.NONE)
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        FlowPath path;
        @NonNull Switch srcSwitch;
        @NonNull Switch destSwitch;
        int srcPort;
        int destPort;
        boolean srcWithMultiTable;
        boolean destWithMultiTable;
        int seqId;
        Long latency;
        boolean failed;

        @Override
        public PathId getPathId() {
            return path != null ? path.getPathId() : null;
        }

        @Override
        public SwitchId getSrcSwitchId() {
            return srcSwitch.getSwitchId();
        }

        @Override
        public SwitchId getDestSwitchId() {
            return destSwitch.getSwitchId();
        }
    }

    /**
     * A cloner for PathSegment entity.
     */
    @Mapper
    public interface PathSegmentCloner {
        PathSegmentCloner INSTANCE = Mappers.getMapper(PathSegmentCloner.class);

        void copy(PathSegmentData source, @MappingTarget PathSegmentData target);

        @Mapping(target = "srcSwitch", ignore = true)
        @Mapping(target = "destSwitch", ignore = true)
        void copyWithoutSwitches(PathSegmentData source, @MappingTarget PathSegmentData target);
    }
}
