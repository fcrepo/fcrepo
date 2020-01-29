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

import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getSidecarSubpath;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.Persister;

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
    protected final FedoraToOCFLObjectIndex index;

    protected AbstractPersister(final Class<? extends ResourceOperation> resourceOperationClass,
                      final ResourceOperationType resourceOperationType,
                      final FedoraToOCFLObjectIndex index) {
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
     * @param resourceId The fedora resource identifier
     * @return The associated mapping
     * @throws PersistentStorageException When no mapping is found.
     */
    protected FedoraOCFLMapping getMapping(final String resourceId) throws PersistentStorageException {
        try {
            return this.index.getMapping(resourceId);
        } catch (FedoraOCFLMappingNotFoundException e){
            throw new PersistentStorageException(e.getMessage());
        }
    }

    /**
     * Resolves the fedora root object identifier associated with the operation's resource identifier.
     * @param operation The operation
     * @param session The OCFL persistent storage session.
     * @return The fedora root object identifier associated with the resource described by the operation.
     */
    protected String resolveRootObjectId(final CreateResourceOperation operation,
                                       final OCFLPersistentStorageSession session) {

        final var resourceId = operation.getResourceId();
        //is resource or any parent an archival group?
        final var startingResourceId = operation.getType().equals(CREATE) ? operation.getParentId() : resourceId;
        final var archivalGroupId = findArchivalGroupInAncestry(startingResourceId, session);

        if (archivalGroupId != null) {
            return archivalGroupId;
        } else if (resourceId.endsWith("/" + FCR_METADATA) || resourceId.endsWith("/" + FCR_ACL)) {
            return resourceId.substring(0, resourceId.lastIndexOf("/"));
        } else {
            return resourceId;
        }
}

    protected String findArchivalGroupInAncestry(final String resourceId, final OCFLPersistentStorageSession session) {
            if (resourceId == null || resourceId.endsWith(FEDORA_ID_PREFIX)) {
                return null;
            }

            //strip off trailing slash if exists
            String cleanedResourceId = resourceId;
            if (resourceId.endsWith("/")) {
                cleanedResourceId = resourceId.substring(0, resourceId.length() - 1);
            }

            try {
                final var headers = session.getHeaders(cleanedResourceId, null);
                if (headers.isArchivalGroup()) {
                    return cleanedResourceId;
                }
            } catch (final PersistentItemNotFoundException ex) {
                //do nothing
            } catch (final PersistentStorageException ex) {
                throw new RepositoryRuntimeException(ex);
            }

            //get the previous path segment including the trailing slash
            final String parentId = cleanedResourceId.substring(0, cleanedResourceId.lastIndexOf('/') + 1);
            return findArchivalGroupInAncestry(parentId, session);
    }

}
