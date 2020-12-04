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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * This class implements the persistence of a new RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
class CreateRdfSourcePersister extends AbstractRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(CreateRdfSourcePersister.class);

    /**
     * Constructor
     * @param index The FedoraToOCFLObjectIndex
     */
    protected CreateRdfSourcePersister(final FedoraToOcflObjectIndex index) {
        super(RdfSourceOperation.class, CREATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {

        final var resourceId = operation.getResourceId();
        log.debug("persisting {} to {}", resourceId, session);

        final CreateResourceOperation createResourceOp = ((CreateResourceOperation)operation);
        final boolean archivalGroup = createResourceOp.isArchivalGroup();

        final var archivalGroupId = findArchivalGroupInAncestry(resourceId, session);
        final FedoraId rootObjectId;

        if (archivalGroup) {
            //if archival group, ensure that there are no archival group ancestors
            if (archivalGroupId.isPresent()) {
                throw new PersistentItemConflictException("Nesting an ArchivalGroup within an ArchivalGroup is not " +
                        "permitted");
            }
            rootObjectId = resourceId;
        } else {
            rootObjectId = archivalGroupId.orElseGet(resourceId::asBaseId);
        }

        final String ocflObjectId = mapToOcflId(session.getId(), rootObjectId);
        final OcflObjectSession ocflObjectSession = session.findOrCreateSession(ocflObjectId);
        persistRDF(ocflObjectSession, operation, rootObjectId.asBaseId(), archivalGroupId.isPresent());
        ocflIndex.addMapping(session.getId(), resourceId.asResourceId(), rootObjectId.asBaseId(), ocflObjectId);
    }
}
