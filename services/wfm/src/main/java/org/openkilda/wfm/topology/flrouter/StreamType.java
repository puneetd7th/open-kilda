/* Copyright 2018 Telstra Open Source
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

package org.openkilda.wfm.topology.flrouter;

/**
 * Stream types for FL Router topology.
 */
public enum StreamType {

    /**
     * Request message for FL speaker topic.
     */
    REQUEST_SPEAKER,

    /**
     * Request message for FL speaker flow topic.
     */
    REQUEST_SPEAKER_FLOW,

    /**
     * Request message for FL speaker disco topic.
     */
    REQUEST_SPEAKER_DISCO,

    /**
     * Response message.
     */
    RESPONSE,

    /**
     * Error message.
     */
    ERROR,

    /**
     * Response for topology engine.
     */
    TPE_RESPONSE,

    /**
     * Response for Northbound.
     */
    NB_RESPONSE,

    /**
     * Response for Event topology.
     */
    WFM_RESPONSE;
}