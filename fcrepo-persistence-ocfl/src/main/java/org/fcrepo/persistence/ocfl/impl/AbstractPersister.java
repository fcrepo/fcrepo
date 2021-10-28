/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeaderUtils;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.fcrepo.storage.ocfl.OcflObjectSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * A base abstract persister class
 *
 * @author dbernstein
 * @since 6.0.0
 */
abstract class AbstractPersister implements Persister {

    /*
     * The resourceOperationClass variable, in conjunction with the resourceOperationType, is used by the handle(...)
     * method to determine whether or not the persister can perform the persistence routine on the operation passed in
     * the persist(...) method.
     */
    private final Class<? extends ResourceOperation> resourceOperationClass;
    /*
     * The resourceOperationType, in conjunction with the resourceOperationClass is used by the handle(...) method
     * to determine which operations the persister knows how to handle.
     */
    private final ResourceOperationType resourceOperationType;
    protected final FedoraToOcflObjectIndex ocflIndex;

    protected AbstractPersister(final Class<? extends ResourceOperation> resourceOperationClass,
                      final ResourceOperationType resourceOperationType,
                      final FedoraToOcflObjectIndex ocflIndex) {
        this.resourceOperationClass = resourceOperationClass;
        this.resourceOperationType = resourceOperationType;
        this.ocflIndex = ocflIndex;
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
            return resourceOperationClass.isInstance(operation) && resourceOperationType.equals(operation.getType());
    }

    /**
     *
     * @param transaction The externally generated transaction.
     * @param resourceId The fedora resource identifier
     * @return The associated mapping
     * @throws PersistentStorageException When no mapping is found.
     */
    protected FedoraOcflMapping getMapping(final Transaction transaction, final FedoraId resourceId)
            throws PersistentStorageException {
        try {
            return this.ocflIndex.getMapping(transaction, resourceId);
        } catch (final FedoraOcflMappingNotFoundException e) {
            throw new PersistentStorageException(e.getMessage());
        }
    }

    protected Optional<FedoraId> findArchivalGroupInAncestry(final FedoraId fedoraId,
                                                             final OcflPersistentStorageSession session) {
            if (fedoraId.isRepositoryRoot()) {
                return Optional.empty();
            }

            final var resourceId = fedoraId.getResourceId();

            try {
                final var headers = session.getHeaders(fedoraId.asResourceId(), null);
                if (headers != null && headers.isArchivalGroup()) {
                    return Optional.of(fedoraId);
                }
            } catch (final PersistentItemNotFoundException ex) {
                //do nothing since there are cases where the resourceId will be the resource
                //that is about to be created and thus will not yet exist in peristent storage.
            } catch (final PersistentStorageException ex) {
                throw new RepositoryRuntimeException(ex.getMessage(), ex);
            }

            //get the previous path segment not including the trailing slash
            final String parentId = resourceId.substring(0, resourceId.lastIndexOf('/'));
            return findArchivalGroupInAncestry(FedoraId.create(parentId), session);
    }

    /**
     * Maps the Fedora ID to an OCFL ID.
     * @param transaction The externally generated transaction.
     * @param fedoraId The fedora identifier for the root OCFL object
     * @return The OCFL ID
     */
    protected String mapToOcflId(final Transaction transaction, final FedoraId fedoraId) {
        try {
            final var mapping = ocflIndex.getMapping(transaction, fedoraId.asBaseId());
            return mapping.getOcflObjectId();
        } catch (final FedoraOcflMappingNotFoundException e) {
            // If the mapping doesn't already exist, use a one-to-one Fedora ID to OCFL ID mapping
            return fedoraId.getBaseId();
        }
    }

    protected ResourceHeadersImpl createCommonHeaders(final OcflObjectSession session,
                                                      final ResourceOperation operation,
                                                      final boolean isResourceRoot,
                                                      final FedoraId archivalGroupId)
            throws PersistentStorageException {
        final var now = Instant.now();

        final ResourceHeadersImpl headers;
        if (CREATE.equals(operation.getType())) {
            final var createOperation = (CreateResourceOperation) operation;
            headers = ResourceHeaderUtils.newResourceHeaders(
                    createOperation.getParentId(),
                    createOperation.getResourceId(),
                    createOperation.getInteractionModel());
            ResourceHeaderUtils.touchCreationHeaders(headers, createOperation.getUserPrincipal(), now);
            headers.setArchivalGroup(createOperation.isArchivalGroup());
            headers.setObjectRoot(isResourceRoot);
            headers.setArchivalGroupId(archivalGroupId);
        } else {
            headers = new ResourceHeadersAdapter(session.readHeaders(operation.getResourceId().getResourceId()))
                    .asKernelHeaders();
        }

        // Existing size and digests must be cleared so they can be populated for the new content
        headers.setDigests(new ArrayList<>());
        headers.setContentSize(-1);

        ResourceHeaderUtils.touchModificationHeaders(headers, operation.getUserPrincipal(), now);

        return headers;
    }

}
