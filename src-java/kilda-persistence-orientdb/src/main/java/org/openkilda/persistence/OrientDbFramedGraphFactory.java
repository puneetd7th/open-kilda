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

package org.openkilda.persistence;

import org.openkilda.persistence.ferma.FramedGraphFactory;

import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.WrappedFramedGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;

/**
 * A factory creates graph instances for interacting with Neo4j.
 */
class OrientDbFramedGraphFactory implements FramedGraphFactory {
    private final OrientGraphFactory factory;

    OrientDbFramedGraphFactory(OrientGraphFactory factory) {
        this.factory = factory;
    }

    /**
     * Return a new instance of framed graph which is connected to Neo4j.
     */
    @Override
    public WrappedFramedGraph<? extends Graph> getGraph() {
        OrientTransactionFactory txFactory =
                new OrientTransactionFactoryImpl(factory, false);
        return new DelegatingFramedGraph<>(factory);
    }
}
