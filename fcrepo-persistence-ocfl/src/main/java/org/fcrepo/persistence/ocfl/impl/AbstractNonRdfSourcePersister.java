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

import java.util.List;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.FileWriteOutcome;
import org.fcrepo.persistence.common.MultiDigestInputStreamWrapper;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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
                                  final FedoraToOcflObjectIndex index) {
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
                                       final OcflObjectSession objectSession, final FedoraId rootIdentifier)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();

        final var contentPath = PersistencePaths.nonRdfContentPath(rootIdentifier, resourceId);
        final var headerPath = PersistencePaths.headerPath(rootIdentifier, resourceId);

        log.debug("persisting ({}) to {}", resourceId, contentPath);
        // write user content
        final var nonRdfSourceOperation = (NonRdfSourceOperation) operation;

        final FileWriteOutcome outcome;
        if (forExternalBinary(nonRdfSourceOperation)) {
            outcome = null;
        } else {
            final var transmissionDigestAlg = objectSession.getObjectDigestAlgorithm();
            final var providedDigests = nonRdfSourceOperation.getContentDigests();

            // Wrap binary stream in digest computing wrapper, requesting
            final var multiDigestWrapper = new MultiDigestInputStreamWrapper(
                    nonRdfSourceOperation.getContentStream(),
                    providedDigests,
                    List.of(transmissionDigestAlg));
            final var contentStream = multiDigestWrapper.getInputStream();

            outcome = (FileWriteOutcome) objectSession.write(contentPath, contentStream);

            // Verify that the content matches the provided digests
            if (!CollectionUtils.isEmpty(providedDigests)) {
                multiDigestWrapper.checkFixity();
            }
            // Store the computed and verified digests in the write outcome
            outcome.setDigests(multiDigestWrapper.getDigests());

            // Register the transmission digest for this file
            objectSession.registerTransmissionDigest(contentPath, multiDigestWrapper.getDigest(transmissionDigestAlg));
        }

        // Write resource headers
        final var headers = populateHeaders(objectSession, headerPath, nonRdfSourceOperation, outcome,
                resourceId.equals(rootIdentifier), contentPath);
        writeHeaders(objectSession, headers, headerPath);
    }

    /**
     * Constructs a ResourceHeaders object populated with the properties provided by the
     * operation, and merged with existing properties if appropriate.
     *
     * @param objSession the object session
     * @param headerPath the headerPath of the file
     * @param op the operation being persisted
     * @param writeOutcome outcome of persisting the original file
     * @param objectRoot flag indicating whether or not headerPath represents the object root resource
     * @return populated resource headers
     * @throws PersistentStorageException if unexpectedly unable to retrieve existing object headers
     */
    private ResourceHeaders populateHeaders(final OcflObjectSession objSession, final String headerPath,
                                            final NonRdfSourceOperation op, final WriteOutcome writeOutcome,
                                            final boolean objectRoot, final String contentPath)
            throws PersistentStorageException {

        final ResourceHeadersImpl headers;
        final var timeWritten = writeOutcome != null ? writeOutcome.getTimeWritten() : null;
        if (CREATE.equals(op.getType())) {
            final var createOperation = (CreateResourceOperation) op;
            headers = newResourceHeaders(createOperation.getParentId(),
                    op.getResourceId(),
                    NON_RDF_SOURCE.toString());
            headers.setObjectRoot(objectRoot);
            touchCreationHeaders(headers, op.getUserPrincipal(), timeWritten);
        } else {
            headers = (ResourceHeadersImpl) readHeaders(objSession, headerPath);
        }
        touchModificationHeaders(headers, op.getUserPrincipal(), timeWritten);

        final var contentSize = getContentSize(op, writeOutcome);
        final var digests = writeOutcome == null ? op.getContentDigests() : writeOutcome.getDigests();

        populateBinaryHeaders(headers, op.getMimeType(),
                op.getFilename(),
                contentSize,
                digests);

        if (forExternalBinary(op)) {
            // Clear any content path for externals
            headers.setContentPath(null);
            populateExternalBinaryHeaders(headers, op.getContentUri().toString(),
                    op.getExternalHandling());
        } else {
            headers.setContentPath(contentPath);
            if (!CREATE.equals(op.getType())) {
                // If performing an update of an internal binary, ensure that no external properties are retained
                populateExternalBinaryHeaders(headers, null, null);
            }
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
    protected boolean forExternalBinary(final NonRdfSourceOperation op) {
        return op.getContentUri() != null && op.getExternalHandling() != null;
    }
}
