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

package org.openkilda.model.history;

import org.openkilda.model.CompositeDataEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Delegate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents information about the flow event.
 * The event has an actor and represents actions from outside of Kilda.
 */
public class FlowEvent implements CompositeDataEntity<FlowEvent.FlowEventData> {
    @Getter
    @Delegate
    @JsonIgnore
    private FlowEventData data;

    public FlowEvent() {
        data = new FlowEventDataImpl();
    }

    public FlowEvent(@NonNull FlowEventData data) {
        this.data = data;
    }

    /**
     * Defines persistable data of the FlowEvent.
     */
    public interface FlowEventData {
        String getFlowId();

        void setFlowId(String flowId);

        Instant getTimestamp();

        void setTimestamp(Instant timestamp);

        String getActor();

        void setActor(String actor);

        String getAction();

        void setAction(String action);

        String getTaskId();

        void setTaskId(String taskId);

        String getDetails();

        void setDetails(String details);

        Collection<FlowHistory> getHistoryRecords();

        Collection<FlowDump> getFlowDumps();
    }

    /**
     * POJO implementation of FlowEventData.
     */
    @Data
    @NoArgsConstructor
    final class FlowEventDataImpl implements FlowEventData, Serializable {
        private static final long serialVersionUID = 1L;
        String flowId;
        Instant timestamp;
        String actor;
        String action;
        String taskId;
        String details;

        @Override
        public Collection<FlowHistory> getHistoryRecords() {
            return Collections.emptyList();
        }

        @Override
        public Collection<FlowDump> getFlowDumps() {
            return Collections.emptyList();
        }
    }

    @Mapper
    public interface FlowEventCloner {
        FlowEventCloner INSTANCE = Mappers.getMapper(FlowEventCloner.class);

        void copy(FlowEventData source, @MappingTarget FlowEventData target);
    }
}
