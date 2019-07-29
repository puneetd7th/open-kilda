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

package org.openkilda.persistence.ferma.repositories;

import static java.lang.String.format;

import org.openkilda.model.PathId;
import org.openkilda.model.PathSegment;
import org.openkilda.model.PathSegment.PathSegmentData;
import org.openkilda.persistence.PersistenceException;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FramedGraphFactory;
import org.openkilda.persistence.ferma.frames.FlowPathFrame;
import org.openkilda.persistence.ferma.frames.PathSegmentFrame;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.repositories.PathSegmentRepository;

/**
 * Ferma (Tinkerpop) implementation of {@link PathSegmentRepository}.
 */
class FermaPathSegmentRepository extends FermaGenericRepository<PathSegment, PathSegmentData>
        implements PathSegmentRepository {
    FermaPathSegmentRepository(FramedGraphFactory<?> graphFactory, TransactionManager transactionManager) {
        super(graphFactory, transactionManager);
    }

    @Override
    public void updateFailedStatus(PathId pathId, PathSegment segment, boolean failed) {
        PathSegment.PathSegmentData data = segment.getData();
        transactionManager.doInTransaction(() -> {
            PathSegmentFrame segmentFrame;
            if (data instanceof PathSegmentFrame) {
                segmentFrame = (PathSegmentFrame) data;
            } else {
                FlowPathFrame pathFrame = framedGraph().traverse(g -> g.V()
                        .hasLabel(FlowPathFrame.FRAME_LABEL)
                        .has(FlowPathFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.map(pathId)))
                        .nextOrDefaultExplicit(FlowPathFrame.class, null);
                if (pathFrame == null) {
                    throw new IllegalArgumentException("Unable to locate the path " + pathId);
                }
                segmentFrame = (PathSegmentFrame) pathFrame.getSegments().stream()
                        .filter(pathSegment -> pathSegment.getSrcSwitchId().equals(segment.getSrcSwitchId())
                                && pathSegment.getSrcPort() == segment.getSrcPort()
                                && pathSegment.getDestSwitchId().equals(segment.getDestSwitchId())
                                && pathSegment.getDestPort() == segment.getDestPort())
                        .findAny()
                        .map(PathSegment::getData).orElse(null);
            }

            if (segmentFrame == null) {
                throw new PersistenceException(
                        format("PathSegment not found to be updated: %s_%d - %s_%d. Path id: %s.",
                                segment.getSrcSwitchId(), segment.getSrcPort(),
                                segment.getDestSwitchId(), segment.getDestPort(), pathId));
            }

            segmentFrame.setFailed(failed);
        });
    }

    @Override
    public PathSegment add(PathSegment entity) {
        PathSegmentData data = entity.getData();
        if (data instanceof PathSegmentFrame) {
            throw new IllegalArgumentException("Can't add entity " + entity + " which is already framed graph element");
        }
        return transactionManager.doInTransaction(() ->
                new PathSegment(PathSegmentFrame.create(framedGraph(), data)));
    }
}
