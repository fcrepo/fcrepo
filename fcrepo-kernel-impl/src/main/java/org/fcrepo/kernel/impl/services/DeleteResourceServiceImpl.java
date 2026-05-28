/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.fcrepo.kernel.api.Transaction;
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
        return containmentIndex.getContains(tx, resource.getFedoraId());
    }

    @Override
    protected void doAction(final Transaction tx, final PersistentStorageSession pSession,
                            final FedoraId fedoraId, final String userPrincipal)
            throws PersistentStorageException {
        log.debug("starting delete of {}", fedoraId.getFullId());
        final ResourceOperation deleteOp = deleteResourceFactory.deleteBuilder(tx, fedoraId)
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResource(tx, pSession, fedoraId);
        tx.lockResource(fedoraId);

        pSession.persist(deleteOp);
        membershipService.resourceDeleted(tx, fedoraId);
        containmentIndex.removeResource(tx, fedoraId);
        referenceService.deleteAllReferences(tx, fedoraId);
        searchIndex.removeFromIndex(tx, fedoraId);
        recordEvent(tx, fedoraId, deleteOp);
        log.debug("deleted {}", fedoraId.getFullId());
    }

}
