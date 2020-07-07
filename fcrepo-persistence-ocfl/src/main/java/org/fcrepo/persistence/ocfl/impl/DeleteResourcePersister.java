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

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.isSidecarSubpath;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resolveExtensions;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resolveOCFLSubpath;

import java.time.Instant;
import java.util.Objects;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageRuntimeException;
import org.fcrepo.persistence.common.ResourceHeaderUtils;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void persist(final OCFLPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var mapping = getMapping(operation.getResourceId());
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
                    objectSession.listHeadSubpaths().filter(p -> !isSidecarSubpath(p))
                            .forEach(p -> deletePathWrapped(p, objectSession, user, deleteTime));
                }
            } catch (final PersistentStorageRuntimeException exc) {
                // Rethrow the exception as a checked exception
                throw new PersistentStorageException(exc);
            }
        } else {
            final var relativeSubPath = relativizeSubpath(fedoraResourceRoot, operation.getResourceId());
            final var ocflSubPath = resolveOCFLSubpath(fedoraResourceRoot, relativeSubPath);
            final var headers = (ResourceHeadersImpl) readHeaders(objectSession, ocflSubPath);
            final boolean isRdf = !Objects.equals(NON_RDF_SOURCE.toString(), headers.getInteractionModel());
            final var filePath = resolveExtensions(ocflSubPath, isRdf);
            deletePath(filePath, objectSession, headers, user, deleteTime);
        }
    }

    /**
     * Simple utility to delete a path's files and mark them as deleted in the headers file.
     * @param path Path to delete
     * @param session Session to delete the path in.
     */
    private void deletePath(final String path, final OCFLObjectSession session, final String user,
                            final Instant deleteTime) throws PersistentStorageException {
        // readHeaders and writeHeaders need the subpath where as delete needs the file name. So remove any extensions.
        // TODO: See https://jira.lyrasis.org/browse/FCREPO-3287
        final var no_extension = (path.contains(".") ? path.substring(0, path.indexOf(".")) : path);
        final var headers = (ResourceHeadersImpl) readHeaders(session, no_extension);
        deletePath(path, session, headers, user, deleteTime);
    }

    /**
     * Simple utility to delete a path's files and mark them as deleted in the headers file.
     * @param path Path to delete
     * @param session Session to delete the path in.
     * @param headers The headers for the file.
     * @throws PersistentStorageException if can't read, write or delete a file.
     */
    private void deletePath(final String path, final OCFLObjectSession session, final ResourceHeadersImpl headers,
                            final String user, final Instant deleteTime)
        throws PersistentStorageException {
        session.delete(path);
        headers.setDeleted(true);
        ResourceHeaderUtils.touchModificationHeaders(headers, user, deleteTime);
        // readHeaders and writeHeaders need the subpath where as delete needs the file name. So remove any extensions.
        // TODO: See https://jira.lyrasis.org/browse/FCREPO-3287
        final var no_extension = (path.contains(".") ? path.substring(0, path.indexOf(".")) : path);
        if (!session.isNewInSession(path)) {
            writeHeaders(session, headers, no_extension);
        }
    }

    /**
     * Wrapper to use above function in a lambda.
     * @param path Path to delete.
     * @param session Session to delete the path in.
     */
    private void deletePathWrapped(final String path, final OCFLObjectSession session, final String user,
                                   final Instant deleteTime) {
        try {
            deletePath(path, session, user, deleteTime);
        } catch (final PersistentStorageException exc) {
            throw new PersistentStorageRuntimeException(exc);
        }
    }
}
