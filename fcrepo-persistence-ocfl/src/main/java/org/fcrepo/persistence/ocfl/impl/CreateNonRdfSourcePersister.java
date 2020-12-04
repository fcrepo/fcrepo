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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * This class implements the persistence of a new NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
class CreateNonRdfSourcePersister extends AbstractNonRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(CreateNonRdfSourcePersister.class);

    /**
     * Constructor
     */
    protected CreateNonRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(NonRdfSourceOperation.class, CREATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);

        final var archivalGroupId = findArchivalGroupInAncestry(resourceId, session);
        final var rootObjectId = archivalGroupId.orElseGet(resourceId::asBaseId);
        final String ocflId = mapToOcflId(session.getId(), rootObjectId);
        final OcflObjectSession ocflObjectSession = session.findOrCreateSession(ocflId);

        persistNonRDFSource(operation, ocflObjectSession, rootObjectId.asBaseId(), archivalGroupId.isPresent());
        ocflIndex.addMapping(session.getId(), resourceId.asResourceId(), rootObjectId.asBaseId(), ocflId);
    }
}
