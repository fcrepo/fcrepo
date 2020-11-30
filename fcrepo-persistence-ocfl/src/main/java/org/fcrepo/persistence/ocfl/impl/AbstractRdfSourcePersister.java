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

import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.lang.String.format;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;

/**
 * This class implements the persistence of a new RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
abstract class AbstractRdfSourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(AbstractRdfSourcePersister.class);

    /**
     * Constructor
     */
    protected AbstractRdfSourcePersister(final Class<? extends ResourceOperation> resourceOperation,
                                         final ResourceOperationType resourceOperationType,
                                         final FedoraToOcflObjectIndex index) {
        super(resourceOperation, resourceOperationType, index);
    }

    /**
     * Persists the RDF using the specified operation and session.
     * @param objectSession The object session.
     * @param operation The operation
     * @param rootId The fedora root object identifier tha maps to the OCFL object root.
     * @param isArchivalPart indicates if the resource is an AG part resource, ignored on update
     * @throws PersistentStorageException
     */
    protected void persistRDF(final OcflObjectSession objectSession,
                              final ResourceOperation operation,
                              final FedoraId rootId,
                              final boolean isArchivalPart) throws PersistentStorageException {

        final RdfSourceOperation rdfSourceOp = (RdfSourceOperation)operation;
        log.debug("persisting RDFSource ({}) to OCFL", operation.getResourceId());

        final var headers = createHeaders(
                objectSession,
                rdfSourceOp,
                operation.getResourceId().equals(rootId),
                isArchivalPart ? rootId : null);

        writeRdf(objectSession, headers, rdfSourceOp.getTriples());
    }

    /**
     * Constructs a ResourceHeaders object populated with the properties provided by the
     * operation, and merged with existing properties if appropriate.
     *
     * @param objSession the object session
     * @param operation the operation being persisted
     * @param objectRoot indicates this is the object root
     * @param archivalGroupId for AG parts, the id of the containg AG, otherwise null
     * @return populated resource headers
     */
    private ResourceHeadersImpl createHeaders(final OcflObjectSession objSession,
                                          final RdfSourceOperation operation,
                                          final boolean objectRoot,
                                          final FedoraId archivalGroupId) throws PersistentStorageException {
        final var headers = createCommonHeaders(objSession, operation, objectRoot, archivalGroupId);
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

    /**
     * Writes an RDFStream to a contentPath within an ocfl object.
     *
     * @param session The object session
     * @param triples The triples
     * @throws PersistentStorageException on write failure
     */
    private void writeRdf(final OcflObjectSession session,
                          final ResourceHeadersImpl headers,
                          final RdfStream triples) throws PersistentStorageException {
        try (final var os = new ByteArrayOutputStream()) {
            final StreamRDF streamRDF = getWriterStream(os, OcflPersistentStorageUtils.getRdfFormat().getLang());
            streamRDF.start();
            if (triples != null) {
                triples.forEach(streamRDF::triple);
            }
            streamRDF.finish();

            final var is = new ByteArrayInputStream(os.toByteArray());
            session.writeResource(new ResourceHeadersAdapter(headers).asStorageHeaders(), is);
            log.debug("wrote {} to {}", headers.getId().getFullId(), session.sessionId());
        } catch (final IOException ex) {
            throw new PersistentStorageException(
                    format("failed to write %s in %s", headers.getId().getFullId(), session.sessionId()), ex);
        }
    }

}
