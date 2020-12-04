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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionUtils;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.DeleteResourceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.stream.Stream;

/**
 * This class mediates delete operations between the kernel and persistent storage layers
 *
 * @author dbernstein
 */
@Component
public class DeleteResourceServiceImpl extends AbstractDeleteResourceService implements DeleteResourceService {

    private final static Logger log = LoggerFactory.getLogger(DeleteResourceService.class);

    @Inject
    private DeleteResourceOperationFactory deleteResourceFactory;

    @Override
    protected Stream<String> getContained(final Transaction tx, final FedoraResource resource) {
        return containmentIndex.getContains(TransactionUtils.openTxId(tx), resource.getFedoraId());
    }

    @Override
    protected void doAction(final Transaction tx, final PersistentStorageSession pSession,
                            final FedoraId fedoraId, final String userPrincipal)
            throws PersistentStorageException {
        log.debug("starting delete of {}", fedoraId.getFullId());
        final ResourceOperation deleteOp = deleteResourceFactory.deleteBuilder(fedoraId)
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResource(tx, pSession, fedoraId);
        tx.lockResource(fedoraId);

        pSession.persist(deleteOp);
        membershipService.resourceDeleted(tx.getId(), fedoraId);
        containmentIndex.removeResource(tx.getId(), fedoraId);
        referenceService.deleteAllReferences(tx.getId(), fedoraId);

        recordEvent(tx.getId(), fedoraId, deleteOp);
        log.debug("deleted {}", fedoraId.getFullId());
    }

}
