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
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;

import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
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

    private final Class<? extends ResourceOperation> resourceOperation;
    private final ResourceOperationType resourceOperationType;
    protected final FedoraToOCFLObjectIndex index;

    protected AbstractPersister(final Class<? extends ResourceOperation> resourceOperation,
                      final ResourceOperationType resourceOperationType,
                      final FedoraToOCFLObjectIndex index) {
        this.resourceOperation = resourceOperation;
        this.resourceOperationType = resourceOperationType;
        this.index = index;
    }

    protected static String getSidecarSubpath(final String subpath) {
        return getInternalFedoraDirectory() + subpath + RESOURCE_HEADER_EXTENSION;
    }

    protected static void writeHeaders(final OCFLObjectSession session, final ResourceHeaders headers,
            final String subpath) throws PersistentStorageException {
        final var headerStream = serializeHeaders(headers);
        session.write(getSidecarSubpath(subpath), headerStream);
    }

    protected static ResourceHeaders readHeaders(final OCFLObjectSession objSession, final String subpath)
            throws PersistentStorageException {
        final var headerStream = objSession.read(getSidecarSubpath(subpath));
        return deserializeHeaders(headerStream);
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
            return resourceOperation.isInstance(operation) && resourceOperationType.equals(operation.getType());
    }

    protected FedoraOCFLMapping getMapping(final String resourceId) throws PersistentStorageException {
        try {
            return this.index.getMapping(resourceId);
        } catch (FedoraOCFLMappingNotFoundException e){
            throw new PersistentStorageException(e.getMessage());
        }
    }

    protected FedoraOCFLMapping findMapping(final ResourceOperation operation,
                                            final OCFLPersistentStorageSession session)
            throws PersistentStorageException {

        final String resourceId = operation.getResourceId();

        try {
            return index.getMapping(resourceId);
        } catch (FedoraOCFLMappingNotFoundException e) {
            throw new PersistentStorageException("Unable to resolve parent identifier for " + resourceId);
        }
    }

    protected String resolveRootObjectId(final CreateResourceOperation operation,
                                       final OCFLPersistentStorageSession session) {

        final var parentId = operation.getParentId();
        final var resourceId = operation.getResourceId();
        //final ResourceHeaders headers = session.getHeaders(parentId, null);
        final boolean parentIsAg  = false; // TODO uncomment when headers.isAchivalGroup() is available
        if (parentIsAg) {
            return parentId;
        } else if (resourceId.endsWith("/" + FCR_METADATA) || resourceId.endsWith("/" + FCR_ACL)) {
            return resourceId.substring(0, resourceId.lastIndexOf("/"));
        } else {
            return resourceId;
        }

    }

}
