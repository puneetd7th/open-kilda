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

package org.openkilda.wfm.topology.switchmanager.bolt.speaker.command;

import org.openkilda.floodlight.api.request.SpeakerRequest;
import org.openkilda.wfm.topology.switchmanager.bolt.speaker.SpeakerWorkerBolt;

public class SpeakerSyncRequestCommand extends SpeakerWorkerCommand {
    private final String hubKey;
    private final SpeakerRequest request;

    public SpeakerSyncRequestCommand(String workerKey, String hubKey, SpeakerRequest request) {
        super(workerKey);
        this.hubKey = hubKey;
        this.request = request;
    }

    @Override
    public void apply(SpeakerWorkerBolt handler) {
        handler.processSyncSpeakerRequest(hubKey, request);
    }
}