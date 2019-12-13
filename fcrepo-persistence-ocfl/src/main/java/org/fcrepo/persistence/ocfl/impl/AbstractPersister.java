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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.serializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.api.Persister;

/**
 * A base abstract persister class
 *
 * @author dbernstein
 * @since 6.0.0
 */
public abstract class AbstractPersister implements Persister {

    private final Class<? extends ResourceOperation> resourceOperation;
    private final ResourceOperationType resourceOperationType;
    private final OCFLObjectSessionFactory objectFactory;
    private final FedoraToOCFLObjectIndex index;

    private static final String DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID = "_fedora_repository_root";

    AbstractPersister(final Class<? extends ResourceOperation> resourceOperation,
                      final ResourceOperationType resourceOperationType,
                      final OCFLObjectSessionFactory objectFactory,
                      final FedoraToOCFLObjectIndex index) {
        this.resourceOperation = resourceOperation;
        this.resourceOperationType = resourceOperationType;
        this.objectFactory = objectFactory;
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

    protected FedoraOCFLMapping findOrCreateFedoraOCFLMapping(final ResourceOperation operation,
                                                            final OCFLPersistentStorageSession session)
            throws PersistentStorageException {

        final String resourceId = operation.getResourceId();

        try {
            return index.getMapping(resourceId);
        } catch (FedoraOCFLMappingNotFoundException e) {
            //if no mapping exists, create one
            if (operation instanceof CreateResourceOperation) {
                final CreateResourceOperation createResourceOp = ((CreateResourceOperation)operation);
                final boolean archivalGroup = createResourceOp.isArchivalGroup();
                final String rootObjectId = archivalGroup ? resourceId : resolveRootObjectId(createResourceOp, session);
                return index.addMapping(resourceId, rootObjectId, mintOCFLObjectId(rootObjectId));
            } else {
                throw new PersistentStorageException("Unable to resolve parent identifier for " + resourceId);
            }
        }
    }

    protected String resolveRootObjectId(final CreateResourceOperation operation,
                                       final OCFLPersistentStorageSession session) {
        final String parentId = operation.getParentId();

        //final ResourceHeaders headers = session.getHeaders(parentId, null);
        final boolean parentIsAg  = false; // TODO uncomment when headers.isAchivalGroup() is available
        if(parentIsAg) {
            return parentId;
        } else {
            return operation.getResourceId();
        }

    }

    private String mintOCFLObjectId(final String fedoraIdentifier) {
        //TODO make OCFL Object Id minting more configurable.
        String bareFedoraIdentifier = fedoraIdentifier;
        if (fedoraIdentifier.indexOf(FEDORA_ID_PREFIX) == 0) {
            bareFedoraIdentifier = fedoraIdentifier.substring(FEDORA_ID_PREFIX.length());
        }

        //ensure no accidental collisions with the root ocfl identifier
        if (bareFedoraIdentifier.equals(DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID)) {
            throw new RepositoryRuntimeException(bareFedoraIdentifier + " in a reserved identifier");
        }

        bareFedoraIdentifier = bareFedoraIdentifier.replace("/", "_");

        if (bareFedoraIdentifier.length() == 0) {
            bareFedoraIdentifier = DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID;
        }

        return bareFedoraIdentifier;
    }
}
