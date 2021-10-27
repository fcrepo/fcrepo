/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * This class implements the persistence of a new NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
class CreateNonRdfSourcePersister extends AbstractNonRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(CreateNonRdfSourcePersister.class);

    /**
     * Constructor
     */
    protected CreateNonRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(NonRdfSourceOperation.class, CREATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);

        final var archivalGroupId = findArchivalGroupInAncestry(resourceId, session);
        final var rootObjectId = archivalGroupId.orElseGet(resourceId::asBaseId);
        final String ocflId = mapToOcflId(operation.getTransaction(), rootObjectId);
        final OcflObjectSession ocflObjectSession = session.findOrCreateSession(ocflId);

        persistNonRDFSource(operation, ocflObjectSession, rootObjectId.asBaseId(), archivalGroupId.isPresent());
        ocflIndex.addMapping(operation.getTransaction(), resourceId.asResourceId(), rootObjectId.asBaseId(), ocflId);
    }
}
