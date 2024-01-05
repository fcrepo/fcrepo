/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.OverwriteTombstoneOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistence for overwriting tombstones of an RDFSource
 *
 * @author mikejritter
 */
public class OverwriteRdfTombstonePersister extends AbstractRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(OverwriteRdfTombstonePersister.class);

    /**
     * Constructor
     *
     * @param index the FedoraToOCFLObjectIndex
     */
    protected OverwriteRdfTombstonePersister(final FedoraToOcflObjectIndex index) {
        super(RdfSourceOperation.class, ResourceOperationType.OVERWRITE_TOMBSTONE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
        throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);

        final OverwriteTombstoneOperation overwriteTombstoneOp = ((OverwriteTombstoneOperation) operation);
        final boolean archivalGroup = overwriteTombstoneOp.isArchivalGroup();

        final var headers = session.getHeaders(resourceId, null);
        if (headers == null) {
            throw new PersistentItemNotFoundException("Resource does not exist");
        } else if (!headers.isDeleted()) {
            throw new PersistentStorageException("Cannot overwrite tombstone of non-deleted resource");
        }


        final FedoraId rootObjectId;
        // Check to make sure the interaction model hasn't changed from an ArchivalGroup or Atomic Resource
        if (archivalGroup) {
            if (!headers.isArchivalGroup()) {
                throw new PersistentItemConflictException("Changing from an Atomic Resource to an ArchivalGroup is " +
                                                          "not permitted");
            }

            rootObjectId = resourceId;
        } else if (headers.isArchivalGroup()) {
            throw new PersistentItemConflictException("Changing from an ArchivalGroup to an Atomic Resource is not " +
                                                      "permitted");
        } else {
            rootObjectId = headers.getArchivalGroupId() != null ? headers.getArchivalGroupId() : resourceId.asBaseId();
        }

        final String ocflObjectId = mapToOcflId(operation.getTransaction(), rootObjectId);
        final OcflObjectSession ocflObjectSession = session.findOrCreateSession(ocflObjectId);
        persistRDF(ocflObjectSession, operation, rootObjectId.asBaseId(), headers.getArchivalGroupId() != null);
        ocflIndex.addMapping(operation.getTransaction(), resourceId.asResourceId(), rootObjectId.asBaseId(),
                             ocflObjectId);
    }
}
