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

package org.openkilda.persistence.ferma.frames.converters;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter to convert {@link Instant} to {@link String}.
 */
@Mapper
public interface InstantStringConverter {
    InstantStringConverter INSTANCE = Mappers.getMapper(InstantStringConverter.class);

    default String map(Instant value) {
        if (value == null) {
            return null;
        }
        return DateTimeFormatter.ISO_INSTANT.format(value);
    }

    default Instant map(String value) {
        if (value == null) {
            return null;
        }
        return Instant.parse(value);
    }
}
