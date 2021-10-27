/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeaderUtils;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.CommitType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;

/**
 * Delete Resource Persister
 * @author whikloj
 */
class DeleteResourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(DeleteResourcePersister.class);

    protected DeleteResourcePersister(final FedoraToOcflObjectIndex fedoraOcflIndex) {
        super(ResourceOperation.class, DELETE, fedoraOcflIndex);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var mapping = getMapping(operation.getTransaction(), operation.getResourceId());
        final var resourceId = operation.getResourceId();

        final var objectSession = session.findOrCreateSession(mapping.getOcflObjectId());

        log.debug("Deleting {} from {}", resourceId, mapping.getOcflObjectId());

        try {
            final var headers = new ResourceHeadersAdapter(
                    objectSession.readHeaders(resourceId.getResourceId()))
                    .asKernelHeaders();
            headers.setDeleted(true);
            ResourceHeaderUtils.touchModificationHeaders(headers, operation.getUserPrincipal());

            objectSession.deleteContentFile(new ResourceHeadersAdapter(headers).asStorageHeaders());
            if (headers.getArchivalGroupId() == null) {
                objectSession.commitType(CommitType.NEW_VERSION);
            }
        } catch (final RuntimeException e) {
            throw new PersistentStorageException(
                    String.format("Failed to delete resource content for %s", resourceId), e);
        }

        if (!objectSession.containsResource(resourceId.getResourceId())) {
            ocflIndex.removeMapping(operation.getTransaction(), resourceId);
        }
    }

}
