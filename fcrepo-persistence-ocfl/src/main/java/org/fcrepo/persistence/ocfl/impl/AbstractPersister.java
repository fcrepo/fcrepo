/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.Persister;

import java.util.Optional;

import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getSidecarSubpath;

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
    protected final FedoraToOcflObjectIndex index;

    protected AbstractPersister(final Class<? extends ResourceOperation> resourceOperationClass,
                      final ResourceOperationType resourceOperationType,
                      final FedoraToOcflObjectIndex index) {
        this.resourceOperationClass = resourceOperationClass;
        this.resourceOperationType = resourceOperationType;
        this.index = index;
    }

    /**
     * Writes the resource headers to the sidecar file.
     * @param session The OCFL object session
     * @param headers The resource headers
     * @param subpath The subpath of the resource whose headers you are writing
     * @throws PersistentStorageException
     */
    protected static void writeHeaders(final OCFLObjectSession session, final ResourceHeaders headers,
            final String subpath) throws PersistentStorageException {
        final var headerStream = serializeHeaders(headers);
        session.write(getSidecarSubpath(subpath), headerStream);
    }

    /**
     * Reads the headers associated with the resource at specified subpath.
     * @param objSession The OCFL object session
     * @param subpath The subpath of the resource whose headers you are reading
     * @return The resource's headers object
     * @throws PersistentStorageException
     */
    protected static ResourceHeaders readHeaders(final OCFLObjectSession objSession, final String subpath)
            throws PersistentStorageException {
        final var headerStream = objSession.read(getSidecarSubpath(subpath));
        return deserializeHeaders(headerStream);
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
            return resourceOperationClass.isInstance(operation) && resourceOperationType.equals(operation.getType());
    }

    /**
     *
     * @param transactionId The storage session/transaction identifier.
     * @param resourceId The fedora resource identifier
     * @return The associated mapping
     * @throws PersistentStorageException When no mapping is found.
     */
    protected FedoraOCFLMapping getMapping(final String transactionId, final String resourceId)
            throws PersistentStorageException {
        try {
            return this.index.getMapping(transactionId, resourceId);
        } catch (final FedoraOCFLMappingNotFoundException e){
            throw new PersistentStorageException(e.getMessage());
        }
    }

    /**
     * Resolves the fedora root object identifier associated with the operation's resource identifier.
     * @param fedoraId The fedoraId of the resource the being acted on
     * @param session The OCFL persistent storage session.
     * @return The fedora root object identifier associated with the resource described by the operation.
     */
    protected FedoraId resolveRootObjectId(final FedoraId fedoraId,
                                       final OCFLPersistentStorageSession session) {
        final var archivalGroupId = findArchivalGroupInAncestry(fedoraId, session);
        return archivalGroupId.orElseGet(() -> FedoraId.create(fedoraId.getContainingId()));
    }

    protected Optional<FedoraId> findArchivalGroupInAncestry(final FedoraId fedoraId,
                                                             final OCFLPersistentStorageSession session) {
            if (fedoraId.isRepositoryRoot()) {
                return Optional.empty();
            }

            final var resourceId = fedoraId.getResourceId();

            try {
                final var headers = session.getHeaders(resourceId, null);
                if (headers != null && headers.isArchivalGroup()) {
                    return Optional.of(fedoraId);
                }
            } catch (final PersistentItemNotFoundException ex) {
                //do nothing since there are cases where the resourceId will be the resource
                //that is about to be created and thus will not yet exist in peristent storage.
            } catch (final PersistentStorageException ex) {
                throw new RepositoryRuntimeException(ex);
            }

            //get the previous path segment not including the trailing slash
            final String parentId = resourceId.substring(0, resourceId.lastIndexOf('/'));
            return findArchivalGroupInAncestry(FedoraId.create(parentId), session);
    }

    /**
     * Maps the Fedor ID to an OCFL ID.
     * @param fedoraId The fedora identifier for the root OCFL object
     * @return The OCFL ID
     */
    protected String mapToOcflId(final FedoraId fedoraId) {
        try {
            final var mapping = index.getMapping(fedoraId.getContainingId());
            return mapping.getOcflObjectId();
        } catch (FedoraOCFLMappingNotFoundException e) {
            // If the a mapping doesn't already exist, use a one-to-one Fedora ID to OCFL ID mapping
            return fedoraId.getContainingId();
        }
    }

}
