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
import static org.fcrepo.kernel.api.operations.ResourceOperationType.PURGE;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getSidecarSubpath;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resolveOCFLSubpath;

import java.util.Objects;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purge Resource Persister
 * @author whikloj
 */
class PurgeResourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(PurgeResourcePersister.class);

    protected PurgeResourcePersister(final FedoraToOCFLObjectIndex fedoraOcflIndex) {
        super(ResourceOperation.class, PURGE, fedoraOcflIndex);
    }

    @Override
    public void persist(final OCFLPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var mapping = getMapping(operation.getResourceId());
        final var fedoraResourceRoot = mapping.getRootObjectIdentifier();
        final var resourceId = operation.getResourceId();
        final var objectSession = session.findOrCreateSession(mapping.getOcflObjectId());
        log.debug("Deleting {} from {}", resourceId, mapping.getOcflObjectId());
        if (fedoraResourceRoot.equals(resourceId)) {
            // We are at the root of the object, so remove the entire OCFL object.
            objectSession.deleteObject();
        } else {
            final var relativeSubPath = relativizeSubpath(fedoraResourceRoot, operation.getResourceId());
            final var ocflSubPath = resolveOCFLSubpath(fedoraResourceRoot, relativeSubPath);
            final var headers = (ResourceHeadersImpl) readHeaders(objectSession, ocflSubPath);
            final var sidecar = getSidecarSubpath(ocflSubPath);
            final boolean isRdf = !Objects.equals(NON_RDF_SOURCE.toString(), headers.getInteractionModel());
            purgePath(sidecar, objectSession);
            if (!isRdf) {
                // Delete the description sidecar file too.
                final var descSidecar = getSidecarSubpath(ocflSubPath + "-description");
                purgePath(descSidecar, objectSession);
            }
        }
    }

    /**
     * Simple utility to delete a path's sidecar files.
     * @param path Path to purge.
     * @param session Session to delete the path in.
     * @throws PersistentStorageException if can't read, write or delete a file.
     */
    private void purgePath(final String path, final OCFLObjectSession session) throws PersistentStorageException {
        session.delete(path);
    }

}
