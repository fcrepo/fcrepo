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

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageRuntimeException;
import org.fcrepo.persistence.common.ResourceHeaderUtils;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

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
        final var mapping = getMapping(session.getId(), operation.getResourceId());
        final var fedoraResourceRoot = mapping.getRootObjectIdentifier();
        final var resourceId = operation.getResourceId();
        final var objectSession = session.findOrCreateSession(mapping.getOcflObjectId());
        final var user = operation.getUserPrincipal();
        final var deleteTime = Instant.now();
        log.debug("Deleting {} from {}", resourceId, mapping.getOcflObjectId());
        if (fedoraResourceRoot.equals(resourceId)) {
            // We are at the root of the object, so delete all the data files.
            try {
                if (objectSession.isNewInSession()) {
                    // This means the object has no versions (only staged), so just delete the whole thing immediately
                    objectSession.deleteObject();
                } else {
                    objectSession.listHeadSubpaths().filter(PersistencePaths::isHeaderFile)
                            .forEach(p -> deletePathWrapped(p, objectSession, user, deleteTime));
                }
            } catch (final PersistentStorageRuntimeException exc) {
                // Rethrow the exception as a checked exception
                throw new PersistentStorageException(exc);
            }
        } else {
            final var headerPath = PersistencePaths.headerPath(fedoraResourceRoot, operation.getResourceId());
            final var headers = (ResourceHeadersImpl) readHeaders(objectSession, headerPath);
            final var contentPath = headers.getContentPath();
            if (objectSession.isNewInSession(headers.getContentPath())) {
                objectSession.delete(contentPath);
                objectSession.delete(headerPath);
                index.removeMapping(session.getId(), resourceId);
            } else {
                deletePath(contentPath, objectSession, headers, user, deleteTime, headerPath);
            }
        }
    }

    /**
     * Simple utility to delete a path's files and mark them as deleted in the headers file.
     * @param path Path to delete
     * @param session Session to delete the path in.
     * @param headers The headers for the file.
     * @throws PersistentStorageException if can't read, write or delete a file.
     */
    private void deletePath(final String path, final OcflObjectSession session, final ResourceHeadersImpl headers,
                            final String user, final Instant deleteTime, final String headerPath)
        throws PersistentStorageException {
        if (path != null) {
            session.delete(path);
        }
        headers.setDeleted(true);
        ResourceHeaderUtils.touchModificationHeaders(headers, user, deleteTime);
        writeHeaders(session, headers, headerPath);
    }

    /**
     * Wrapper to use above function in a lambda.
     * @param headerPath Path to delete.
     * @param session Session to delete the path in.
     */
    private void deletePathWrapped(final String headerPath, final OcflObjectSession session, final String user,
                                   final Instant deleteTime) {
        try {
            deletePath(headerPath, session, user, deleteTime);
        } catch (final PersistentStorageException exc) {
            throw new PersistentStorageRuntimeException(exc);
        }
    }

    /**
     * Simple utility to delete files and mark them as deleted in the headers file.
     * @param headerPath Path to delete
     * @param session Session to delete the path in.
     */
    private void deletePath(final String headerPath, final OcflObjectSession session, final String user,
                            final Instant deleteTime) throws PersistentStorageException {
        final var headers = (ResourceHeadersImpl) readHeaders(session, headerPath);
        deletePath(headers.getContentPath(), session, headers, user, deleteTime, headerPath);
    }

}
