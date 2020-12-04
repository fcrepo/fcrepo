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
import org.fcrepo.kernel.api.services.PurgeResourceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.stream.Stream;

/**
 * Implementation of purge resource service.
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class PurgeResourceServiceImpl extends AbstractDeleteResourceService implements PurgeResourceService {

    private final static Logger log = LoggerFactory.getLogger(PurgeResourceServiceImpl.class);

    @Inject
    private DeleteResourceOperationFactory deleteResourceFactory;

    @Override
    protected Stream<String> getContained(final Transaction tx, final FedoraResource resource) {
        return containmentIndex.getContainsDeleted(TransactionUtils.openTxId(tx), resource.getFedoraId());
    }

    @Override
    protected void doAction(final Transaction tx, final PersistentStorageSession pSession, final FedoraId resourceId,
                  final String userPrincipal) throws PersistentStorageException {
        log.debug("starting purge of {}", resourceId.getFullId());
        final ResourceOperation purgeOp = deleteResourceFactory.purgeBuilder(resourceId)
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResource(tx, pSession, resourceId);
        tx.lockResource(resourceId);

        pSession.persist(purgeOp);
        containmentIndex.purgeResource(tx.getId(), resourceId);
        recordEvent(tx.getId(), resourceId, purgeOp);
        log.debug("purged {}", resourceId.getFullId());
    }

}
