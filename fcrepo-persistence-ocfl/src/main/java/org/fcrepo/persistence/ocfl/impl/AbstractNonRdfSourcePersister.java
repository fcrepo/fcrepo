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
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.populateBinaryHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.populateExternalBinaryHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resolveOCFLSubpath;

import java.io.InputStream;

import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.MultiDigestInputStreamWrapper;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the persistence of a NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
abstract class AbstractNonRdfSourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(AbstractNonRdfSourcePersister.class);

    /**
     * Constructor
     */
    protected AbstractNonRdfSourcePersister(final Class<? extends ResourceOperation> resourceOperation,
                                  final ResourceOperationType resourceOperationType,
                                  final FedoraToOCFLObjectIndex index) {
        super(resourceOperation, resourceOperationType, index);
    }

    /**
     * This method handles the shared logic for writing the resource specified in the operation parameter to
     * the OCFL Object Session.
     *
     * @param operation The operation to perform the persistence routine on
     * @param objectSession The ocfl object session
     * @param rootIdentifier The fedora object root identifier associated with the resource to be persisted.
     * @throws PersistentStorageException thrown if writing fails
     */
    protected void persistNonRDFSource(final ResourceOperation operation,
                                       final OCFLObjectSession objectSession, final String rootIdentifier)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        final var fedoraSubpath = relativizeSubpath(rootIdentifier, resourceId);
        final var subpath = resolveOCFLSubpath(rootIdentifier, fedoraSubpath);
        log.debug("persisting ({}) to {}", resourceId, subpath);
        // write user content
        final var nonRdfSourceOperation = (NonRdfSourceOperation) operation;

        final WriteOutcome outcome;
        if (forExternalBinary(nonRdfSourceOperation)) {
            outcome = null;
        } else {
            // if transmission digests provided, wrap inputstream to calculate for incoming data
            final var digests = nonRdfSourceOperation.getContentDigests();
            MultiDigestInputStreamWrapper multiDigestWrapper = null;
            final InputStream contentStream;
            if (digests == null || digests.isEmpty()) {
                contentStream = nonRdfSourceOperation.getContentStream();
            } else {
                multiDigestWrapper = new MultiDigestInputStreamWrapper(
                        nonRdfSourceOperation.getContentStream(),
                        nonRdfSourceOperation.getContentDigests());
                contentStream = multiDigestWrapper.getInputStream();
            }

            outcome = objectSession.write(subpath, contentStream);

            // Verify that the content matches the provided digests
            if (multiDigestWrapper != null) {
                multiDigestWrapper.checkFixity();
            }
        }

        // Write resource headers
        final var headers = populateHeaders(objectSession, subpath, nonRdfSourceOperation, outcome);
        writeHeaders(objectSession, headers, subpath);
    }

    /**
     * Constructs a ResourceHeaders object populated with the properties provided by the
     * operation, and merged with existing properties if appropriate.
     *
     * @param objSession the object session
     * @param subpath the subpath of the file
     * @param op the operation being persisted
     * @param writeOutcome outcome of persisting the original file
     * @return populated resource headers
     * @throws PersistentStorageException if unexpectedly unable to retrieve existing object headers
     */
    private ResourceHeaders populateHeaders(final OCFLObjectSession objSession, final String subpath,
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
            headers = (ResourceHeadersImpl) readHeaders(objSession, subpath);
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
