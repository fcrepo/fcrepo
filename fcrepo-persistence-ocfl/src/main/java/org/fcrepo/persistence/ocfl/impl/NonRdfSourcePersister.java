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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.populateBinaryHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.populateExternalBinaryHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;

import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the persistence of a NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
public class NonRdfSourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(NonRdfSourcePersister.class);

    private static final Set<ResourceOperationType> OPERATION_ACTIONS = new HashSet<>(asList(CREATE, UPDATE));

    /**
     * Constructor
     */
    public NonRdfSourcePersister() {
        super(NonRdfSourceOperation.class, OPERATION_ACTIONS);
    }

    @Override
    public void persist(final PersistentStorageSession storageSession, final OCFLObjectSession session,
                        final ResourceOperation operation, final FedoraOCFLMapping mapping)
                                throws PersistentStorageException {
        log.debug("persisting ({}) to {}", operation.getResourceId(), mapping.getOcflObjectId());
        final String subpath = relativizeSubpath(mapping.getParentFedoraResourceId(), operation.getResourceId());

        // write user content
        final var nonRdfSourceOperation = (NonRdfSourceOperation) operation;
        // TODO supply list of digests to calculate or wrap contentStream in DigestInputStream
        final WriteOutcome outcome;
        if (forExternalBinary(nonRdfSourceOperation)) {
            outcome = null;
        } else {
            outcome = session.write(subpath, nonRdfSourceOperation.getContentStream());
        }
        // TODO verify digests in the outcome match supplied digests

        // Write resource headers
        final var headers = populateHeaders(storageSession, nonRdfSourceOperation, outcome);
        writeHeaders(session, headers, subpath);
    }

    /**
     * Constructs a ResourceHeaders object populated with the properties provided by the
     * operation, and merged with existing properties if appropriate.
     *
     * @param storageSession the storage session encapsulating the operation
     * @param op the operation being persisted
     * @param writeOutcome outcome of persisting the original file
     * @return populated resource headers
     * @throws PersistentStorageException if unexpectedly unable to retrieve existing object headers
     */
    private ResourceHeaders populateHeaders(final PersistentStorageSession storageSession,
            final NonRdfSourceOperation op, final WriteOutcome writeOutcome) throws PersistentStorageException {

        final ResourceHeadersImpl headers;
        final var timeWritten = writeOutcome != null ? writeOutcome.getTimeWritten() : null;
        if (CREATE.equals(op.getType())) {
            final var createOperation = (CreateResourceOperation) op;
            headers = newResourceHeaders(createOperation.getParentId(),
                    op.getResourceId(),
                    NON_RDF_SOURCE.toString());
            touchCreationHeaders(headers, op.getUserPrincipal(), timeWritten);
        } else {
            headers = (ResourceHeadersImpl) storageSession.getHeaders(op.getResourceId(), null);
        }
        touchModificationHeaders(headers, op.getUserPrincipal(), timeWritten);

        final var contentSize = getContentSize(op, writeOutcome);

        populateBinaryHeaders(headers, op.getMimeType(),
                op.getFilename(),
                contentSize,
                op.getContentDigests());
        if (forExternalBinary(op)) {
            populateExternalBinaryHeaders(headers, op.getContentUri().toString(),
                    op.getExternalHandling());
        }

        return headers;
    }

    /**
     * Return the size of the written file in bytes. This will either come from the number of bytes written to disk,
     * or in the case of external binaries, the size provided in the operation
     *
     * @param op operation
     * @param writeOutcome outcome of persisting the original file if applicable
     * @return size of the file
     * @throws PersistentStorageException if the number of bytes written does not match the expected file size
     *         provided in the operation.
     */
    private Long getContentSize(final NonRdfSourceOperation op, final WriteOutcome writeOutcome)
            throws PersistentStorageException {
        if (writeOutcome == null) {
            return op.getContentSize();
        } else {
            final var writtenSize = writeOutcome.getContentSize();
            if (op.getContentSize() != null && !writtenSize.equals(op.getContentSize())) {
                throw new PersistentStorageException(format(
                        "Size of persisted binary did not match supplied expected size: expected %s, received %s",
                        op.getContentSize(), writtenSize));
            }
            return writtenSize;
        }
    }

    /**
     * @param op the operation
     * @return Returns true if the operation involved persisting an external binary
     */
    private boolean forExternalBinary(final NonRdfSourceOperation op) {
        return op.getContentUri() != null && op.getExternalHandling() != null;
    }
}
