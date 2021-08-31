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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE_HEADERS;


import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the persistence of the headers for an RDFSource to bring in new system managed properties
 *
 * @author mikejritter
 * @since 6.0.0
 */
public class UpdateRdfHeadersPersister extends AbstractRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(UpdateRdfSourcePersister.class);

    /**
     * Constructor
     *
     * @param fedoraOcflIndex the FedoraToOcflObjectIndex
     */
    public UpdateRdfHeadersPersister(final FedoraToOcflObjectIndex fedoraOcflIndex) {
        super(RdfSourceOperation.class, UPDATE_HEADERS, fedoraOcflIndex);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
        throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.info("persisting {} headers to {}", resourceId, session);

        final var fedoraOcflMapping = getMapping(operation.getTransaction(), resourceId);
        final var ocflId = fedoraOcflMapping.getOcflObjectId();
        final OcflObjectSession objSession = session.findOrCreateSession(ocflId);

        // unlike with normal updates we don't want to clear the digests/content-size, just the server managed headers
        // in the event the server managed mode is strict, all values will be null
        final var headers = new ResourceHeadersAdapter(objSession.readHeaders(resourceId.getResourceId()));

        final RdfSourceOperation rdfSourceOp = (RdfSourceOperation) operation;
        final var createdDate = rdfSourceOp.getCreatedDate();
        final var lastModifiedDate = rdfSourceOp.getLastModifiedDate();
        final var createdBy = rdfSourceOp.getCreatedBy();
        final var lastModifiedBy = rdfSourceOp.getLastModifiedBy();
        if (createdDate != null) {
            headers.setCreatedDate(createdDate);
        }
        if (lastModifiedDate != null) {
            headers.setLastModifiedDate(lastModifiedDate);
        }
        if (createdBy != null) {
            headers.setCreatedBy(createdBy);
        }
        if (lastModifiedBy != null) {
            headers.setLastModifiedBy(lastModifiedBy);
        }

        objSession.writeHeaders(headers.asStorageHeaders());
    }

}
