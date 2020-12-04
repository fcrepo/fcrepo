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

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

/**
 * This class implements the persistence of a NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
class UpdateNonRdfSourcePersister extends AbstractNonRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(UpdateNonRdfSourcePersister.class);

    /**
     * Constructor
     */
    protected UpdateNonRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(NonRdfSourceOperation.class, UPDATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);
        final FedoraOcflMapping mapping = getMapping(session.getId(), resourceId);
        final var rootIdentifier = mapping.getRootObjectIdentifier();

        log.debug("retrieved mapping: {}", mapping);
        final OcflObjectSession objSession = session.findOrCreateSession(mapping.getOcflObjectId());

        // If storing an external binary, clean up internal binary if needed
        if (forExternalBinary((NonRdfSourceOperation) operation)) {
            // Read the resource headers prior to updating how the existing resource is stored
            final var headers = objSession.readHeaders(resourceId.getResourceId());
            if (headers.getExternalUrl() == null) {
                objSession.deleteContentFile(headers);
            }
        }

        persistNonRDFSource(operation, objSession, rootIdentifier, false);
    }
}
