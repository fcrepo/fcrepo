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
import org.fcrepo.storage.ocfl.ResourceHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

/**
 * This class implements the persistence of a NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
class UpdateNonRdfSourcePersister extends AbstractNonRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(UpdateNonRdfSourcePersister.class);

    /**
     * Constructor
     */
    protected UpdateNonRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(NonRdfSourceOperation.class, UPDATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);
        final FedoraOcflMapping mapping = getMapping(operation.getTransaction(), resourceId);
        final var rootIdentifier = mapping.getRootObjectIdentifier();

        log.debug("retrieved mapping: {}", mapping);
        final OcflObjectSession objSession = session.findOrCreateSession(mapping.getOcflObjectId());

        // If storing an external binary, clean up internal binary if needed
        if (forExternalBinary((NonRdfSourceOperation) operation)) {
            // Read the resource headers prior to updating how the existing resource is stored
            final var headers = objSession.readHeaders(resourceId.getResourceId());
            if (headers.getExternalUrl() == null) {
                // must mark as deleted here so that the headers a valid
                objSession.deleteContentFile(ResourceHeaders.builder(headers)
                        .withDeleted(true).build());
            }
        }

        persistNonRDFSource(operation, objSession, rootIdentifier, false);
    }
}
