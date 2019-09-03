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

package org.openkilda.model.validate;

import lombok.Value;

import java.io.Serializable;
import java.util.Optional;

@Value
public class ValidateDefect implements Serializable {
    private final ValidateOfFlowDefect flow;
    private final ValidateOfMeterDefect meter;

    public Optional<ValidateOfFlowDefect> getFlow() {
        return Optional.ofNullable(flow);
    }

    public Optional<ValidateOfMeterDefect> getMeter() {
        return Optional.ofNullable(meter);
    }
}