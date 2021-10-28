/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.PURGE;

/**
 * Purge Resource Persister
 * @author whikloj
 */
class PurgeResourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(PurgeResourcePersister.class);

    protected PurgeResourcePersister(final FedoraToOcflObjectIndex fedoraOcflIndex) {
        super(ResourceOperation.class, PURGE, fedoraOcflIndex);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var mapping = getMapping(operation.getTransaction(), operation.getResourceId());
        final var resourceId = operation.getResourceId();
        final var objectSession = session.findOrCreateSession(mapping.getOcflObjectId());
        log.debug("Deleting {} from {}", resourceId, mapping.getOcflObjectId());

        try {
            objectSession.deleteResource(resourceId.getResourceId());
        } catch (final RuntimeException e) {
            throw new PersistentStorageException(String.format("Purge resource %s failed", resourceId), e);
        }

        ocflIndex.removeMapping(operation.getTransaction(), resourceId.asResourceId());
    }

}
