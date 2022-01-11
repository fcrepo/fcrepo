/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.fcrepo.kernel.api.Transaction;
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
        return containmentIndex.getContainsDeleted(tx, resource.getFedoraId());
    }

    @Override
    protected void doAction(final Transaction tx, final PersistentStorageSession pSession, final FedoraId resourceId,
                  final String userPrincipal) throws PersistentStorageException {
        log.debug("starting purge of {}", resourceId.getFullId());
        final ResourceOperation purgeOp = deleteResourceFactory.purgeBuilder(tx, resourceId)
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResource(tx, pSession, resourceId);
        tx.lockResource(resourceId);

        pSession.persist(purgeOp);
        containmentIndex.purgeResource(tx, resourceId);
        recordEvent(tx, resourceId, purgeOp);
        log.debug("purged {}", resourceId.getFullId());
    }

}
