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

package org.openkilda.persistence.repositories.impl;

import static java.lang.String.format;

import org.openkilda.model.FlowPath;
import org.openkilda.model.PathId;
import org.openkilda.model.TransitVlan;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.exceptions.PersistenceException;
import org.openkilda.persistence.repositories.TransitVlanRepository;

import com.google.common.collect.ImmutableMap;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Neo4j OGM implementation of {@link TransitVlanRepository}.
 */
public class Neo4jTransitVlanRepository extends Neo4jGenericRepository<TransitVlan> implements TransitVlanRepository {
    static final String PATH_ID_PROPERTY_NAME = "path_id";
    static final String VLAN_PROPERTY_NAME = "vlan";

    public Neo4jTransitVlanRepository(Neo4jSessionFactory sessionFactory, TransactionManager transactionManager) {
        super(sessionFactory, transactionManager);
    }

    /**
     * Lookup for {@link FlowPath} object by pathId (or opposite pathId) value.
     *
     * <p>It make lookup by pathId first and if there is no result it make lookup by {@code oppositePathId}. Such
     * weird logic allow to support both kind of flows(first kind - each path have it's own transit vlan, second
     * kind - only one path have transit vlan, but both of them use it).
     */
    @Override
    public Collection<TransitVlan> findByPathId(PathId pathId, PathId oppositePathId) {
        Filter pathIdFilter = new Filter(PATH_ID_PROPERTY_NAME, ComparisonOperator.EQUALS, pathId);
        Collection<TransitVlan> result = loadAll(pathIdFilter);
        if (result.isEmpty() && oppositePathId != null) {
            pathIdFilter = new Filter(PATH_ID_PROPERTY_NAME, ComparisonOperator.EQUALS, oppositePathId);
            result = loadAll(pathIdFilter);
        }
        return result;
    }

    @Override
    public Optional<TransitVlan> findByPathId(PathId pathId) {
        Filter pathIdFilter = new Filter(PATH_ID_PROPERTY_NAME, ComparisonOperator.EQUALS, pathId);
        Collection<TransitVlan> transitVlans = loadAll(pathIdFilter);
        if (transitVlans.size() > 1) {
            throw new PersistenceException(format("Found more than 1 Transit VLAN entity for path ID '%s'", pathId));
        }
        if (transitVlans.size() == 1) {
            return Optional.of(transitVlans.iterator().next());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TransitVlan> findByVlan(int vlan) {
        Filter vlanFilter = new Filter(VLAN_PROPERTY_NAME, ComparisonOperator.EQUALS, vlan);
        Collection<TransitVlan> transitVlans = loadAll(vlanFilter);

        if (transitVlans.size() > 1) {
            throw new PersistenceException(format("Found more than 1 Transit VLAN entity for vlan ID '%s'", vlan));
        }
        if (transitVlans.size() == 1) {
            return Optional.of(transitVlans.iterator().next());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> findUnassignedTransitVlan(int minVlan, int maxVlan) {
        Map<String, Object> parameters = ImmutableMap.of(
                "min_vlan", minVlan,
                "max_vlan", maxVlan);

        // The query returns the min_vlan if it's not used in any transit_vlan,
        // otherwise locates a gap between / after the values used in transit_vlan entities.

        String query = "UNWIND [$min_vlan] AS vlan "
                + "OPTIONAL MATCH (n:transit_vlan) "
                + "WHERE vlan = n.vlan "
                + "WITH vlan, n "
                + "WHERE n IS NULL "
                + "RETURN vlan "
                + "UNION ALL "
                + "MATCH (n1:transit_vlan) "
                + "WHERE n1.vlan >= $min_vlan AND n1.vlan < $max_vlan "
                + "OPTIONAL MATCH (n2:transit_vlan) "
                + "WHERE (n1.vlan + 1) = n2.vlan "
                + "WITH n1, n2 "
                + "WHERE n2 IS NULL "
                + "RETURN n1.vlan + 1 AS vlan "
                + "ORDER BY vlan "
                + "LIMIT 1";

        return queryForLong(query, parameters, "vlan").map(Long::intValue);
    }

    @Override
    protected Class<TransitVlan> getEntityType() {
        return TransitVlan.class;
    }
}
