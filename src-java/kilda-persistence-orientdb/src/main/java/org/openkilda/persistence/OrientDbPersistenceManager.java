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

package org.openkilda.persistence.orientdb;

import org.openkilda.persistence.PersistenceManager;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.ferma.FermaTransactionManager;
import org.openkilda.persistence.ferma.repositories.FermaRepositoryFactory;

import com.orientechnologies.orient.core.db.OrientDB;

/**
 * OrientDB implementation of {@link PersistenceManager}.
 */
public class OrientDbPersistenceManager implements PersistenceManager {
    private final OrientDbConfig config;
    private final NetworkConfig networkConfig;

    private transient volatile FermaTransactionManager transactionManager;

    public OrientDbPersistenceManager(OrientDbConfig config, NetworkConfig networkConfig) {
        this.config = config;
        this.networkConfig = networkConfig;
    }

    @Override
    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            synchronized (this) {
                if (transactionManager == null) {
                    OrientDB orientDb = new OrientDB("remote:" + host, serverUser, serverPassword, OrientDBConfig.defaultConfig());
                    OrientGraphFactory factory = new OrientGraphFactory(orientDb,
                            config.getDbName(),
                            ODatabaseType.valueOf(config.getDbType()),
                            config.getDbUser(),
                            config.getDbPassword());

                    transactionManager = new FermaTransactionManager(new OrientDbFramedGraphFactory(factory));
                }
            }
        }
        return transactionManager;
    }

    @Override
    public RepositoryFactory getRepositoryFactory() {
        return new FermaRepositoryFactory(getTransactionManager(), networkConfig);
    }
}
