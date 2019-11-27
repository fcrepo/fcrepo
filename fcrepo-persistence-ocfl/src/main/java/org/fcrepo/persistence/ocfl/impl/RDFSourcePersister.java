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

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.resolveOCFLSubpath;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.writeRDF;

import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the persistence of a new RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class RDFSourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(RDFSourcePersister.class);

    private static final Set<ResourceOperationType> OPERATION_TYPES = new HashSet<>(asList(CREATE, UPDATE));

    /**
     * Constructor
     */
    public RDFSourcePersister() {
        super(RdfSourceOperation.class, OPERATION_TYPES);
    }

    @Override
    public void persist(final OCFLObjectSession session, final ResourceOperation operation,
            final FedoraOCFLMapping mapping) throws PersistentStorageException {
        final RdfSourceOperation rdfSourceOp = (RdfSourceOperation)operation;
        log.debug("persisting RDFSource ({}) to {}", operation.getResourceId(), mapping.getOcflObjectId());
        final String subpath = relativizeSubpath(mapping.getParentFedoraResourceId(), operation.getResourceId());
        final String resolvedSubpath = resolveOCFLSubpath(subpath);
        //write user triples
        final var outcome = writeRDF(session, rdfSourceOp.getTriples(), resolvedSubpath);

        // Write resource headers
        final var headers = populateHeaders(session, subpath, rdfSourceOp, outcome);
        writeHeaders(session, headers, subpath);
    }

    /**
     * Constructs a ResourceHeaders object populated with the properties provided by the
     * operation, and merged with existing properties if appropriate.
     *
     * @param objSession the object session
     * @param subpath the subpath of the file
     * @param operation the operation being persisted
     * @param outcome outcome of persisting the RDF file
     * @return populated resource headers
     * @throws PersistentStorageException if unexpectedly unable to retrieve existing object headers
     */
    private ResourceHeaders populateHeaders(final OCFLObjectSession objSession, final String subpath,
            final RdfSourceOperation operation, final WriteOutcome outcome) throws PersistentStorageException {

        final ResourceHeadersImpl headers;
        final var timeWritten = outcome.getTimeWritten();
        if (CREATE.equals(operation.getType())) {
            final var createOperation = (CreateResourceOperation) operation;
            headers = newResourceHeaders(createOperation.getParentId(),
                    operation.getResourceId(),
                    createOperation.getInteractionModel());
            touchCreationHeaders(headers, operation.getUserPrincipal(), timeWritten);
        } else {
            headers = (ResourceHeadersImpl) readHeaders(objSession, subpath);
        }
        touchModificationHeaders(headers, operation.getUserPrincipal(), timeWritten);

        overrideRelaxedProperties(headers, operation);

        return headers;
    }

    /**
     * Overrides generated creation and modification headers with the values
     * provided in the operation if they are present. They should only be present
     * if the server is in relaxed mode for handling server managed triples
     *
     * @param headers the resource headers
     * @param operation the operation
     */
    private void overrideRelaxedProperties(final ResourceHeadersImpl headers, final RdfSourceOperation operation) {
        // Override relaxed properties if provided
        if (operation.getLastModifiedBy() != null) {
            headers.setLastModifiedBy(operation.getLastModifiedBy());
        }
        if (operation.getLastModifiedDate() != null) {
            headers.setLastModifiedDate(operation.getLastModifiedDate());
        }
        if (operation.getCreatedBy() != null) {
            headers.setCreatedBy(operation.getCreatedBy());
        }
        if (operation.getCreatedDate() != null) {
            headers.setCreatedDate(operation.getCreatedDate());
        }
    }
}
