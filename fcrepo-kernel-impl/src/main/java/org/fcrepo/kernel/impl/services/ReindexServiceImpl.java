/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ReindexResourceOperationFactory;
import org.fcrepo.kernel.api.services.ReindexService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Implementation of {@link org.fcrepo.kernel.api.services.ReindexService}
 *
 * @author dbernstein
 */
@Component
public class ReindexServiceImpl extends AbstractService implements ReindexService {

    @Inject
    private TransactionManager transactionManager;

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Inject
    private ReindexResourceOperationFactory resourceOperationFactory;

    @Override
    public void reindexByFedoraId(final String txId, final String principal, final FedoraId fedoraId) {
        final var tx = transactionManager.get(txId);
        final var psession = persistentStorageSessionManager.getSession(txId);
        final var operation = resourceOperationFactory.create(fedoraId).userPrincipal(principal).build();
        tx.lockResource(fedoraId);
        psession.persist(operation);
    }
}
