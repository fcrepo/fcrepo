/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

/**
 * This class implements the persistence of an existing RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
class UpdateRdfSourcePersister extends AbstractRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(UpdateRdfSourcePersister.class);

    /**
     * Constructor
     * @param index The FedoraToOcflObjectIndex
     */
    protected UpdateRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(RdfSourceOperation.class, UPDATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);

        final var fedoraOcflMapping = getMapping(operation.getTransaction(), resourceId);
        final var ocflId = fedoraOcflMapping.getOcflObjectId();
        final OcflObjectSession objSession = session.findOrCreateSession(ocflId);
        persistRDF(objSession, operation, fedoraOcflMapping.getRootObjectIdentifier(), false);
    }
}
