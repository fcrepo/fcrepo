/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * This class implements the persistence of a new RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
class CreateRdfSourcePersister extends AbstractRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(CreateRdfSourcePersister.class);

    /**
     * Constructor
     * @param index The FedoraToOCFLObjectIndex
     */
    protected CreateRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(RdfSourceOperation.class, CREATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {

        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);

        final CreateResourceOperation createResourceOp = ((CreateResourceOperation)operation);
        final boolean archivalGroup = createResourceOp.isArchivalGroup();

        final var archivalGroupId = findArchivalGroupInAncestry(resourceId, session);
        boolean isArchivalPart = archivalGroupId.isPresent();
        final FedoraId rootObjectId;

        if (archivalGroup) {
            // if archival group, ensure that there are no archival group ancestors
            // unless this is the root and is overwriting a tombstone of an archive group
            if (isArchivalPart) {
                final var ancestorId = archivalGroupId.get();
                final var headers = session.getHeaders(ancestorId, null);
                if (headers.isDeleted() && ancestorId.equals(resourceId)) {
                    isArchivalPart = false;
                } else {
                    throw new PersistentItemConflictException(
                        "Nesting an ArchivalGroup within an ArchivalGroup is not " +
                        "permitted");
                }
            }
            rootObjectId = resourceId;
        } else if (isArchivalPart && resourceId.equals(archivalGroupId.get())) {
            throw new PersistentItemConflictException("Changing from an ArchivalGroup to an Atomic resource is not " +
                                                      "permitted");
        } else {
            rootObjectId = archivalGroupId.orElseGet(resourceId::asBaseId);
        }

        final String ocflObjectId = mapToOcflId(operation.getTransaction(), rootObjectId);
        final OcflObjectSession ocflObjectSession = session.findOrCreateSession(ocflObjectId);
        persistRDF(ocflObjectSession, operation, rootObjectId.asBaseId(), isArchivalPart);
        ocflIndex.addMapping(operation.getTransaction(), resourceId.asResourceId(), rootObjectId.asBaseId(),
                ocflObjectId);
    }
}
