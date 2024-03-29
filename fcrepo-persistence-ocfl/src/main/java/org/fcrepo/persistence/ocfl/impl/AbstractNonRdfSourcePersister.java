/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.MultiDigestInputStreamWrapper;
import org.fcrepo.persistence.common.ResourceHeaderUtils;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.BufferedInputStream;
import java.util.Collections;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

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
     * @param isArchivalPart indicates if the resource is an AG part resource, ignored on update
     * @throws PersistentStorageException thrown if writing fails
     */
    protected void persistNonRDFSource(final ResourceOperation operation,
                                       final OcflObjectSession objectSession,
                                       final FedoraId rootIdentifier,
                                       final boolean isArchivalPart)
            throws PersistentStorageException {
        final var resourceId = operation.getResourceId();

        log.debug("persisting NonRDFSource {} to OCFL", resourceId);

        final var nonRdfSourceOperation = (NonRdfSourceOperation) operation;

        final var headers = new ResourceHeadersAdapter(
                createHeaders(objectSession,
                        nonRdfSourceOperation,
                        resourceId.equals(rootIdentifier),
                        isArchivalPart ? rootIdentifier : null));

        // This covers the case where a resource may have been temporarily deleted if it was changed from an internal
        // non-RDF resource to an external non-RDF resource
        headers.setDeleted(false);

        if (forExternalBinary(nonRdfSourceOperation)) {
            objectSession.writeResource(headers.asStorageHeaders(), null);
        } else {
            final var providedDigests = nonRdfSourceOperation.getContentDigests();

            // Wrap binary stream in digest computing wrapper, requesting
            final var multiDigestWrapper = new MultiDigestInputStreamWrapper(
                    nonRdfSourceOperation.getContentStream(),
                    providedDigests,
                    Collections.emptyList());
            final var contentStream = multiDigestWrapper.getInputStream();

            objectSession.writeResource(headers.asStorageHeaders(), new BufferedInputStream(contentStream));

            // Verify that the content matches the provided digests
            if (!CollectionUtils.isEmpty(providedDigests)) {
                multiDigestWrapper.checkFixity();
            }
        }
    }

    /**
     * Constructs a ResourceHeaders object populated with the properties provided by the
     * operation, and merged with existing properties if appropriate.
     *
     * @param objSession the object session
     * @param op the operation being persisted
     * @param objectRoot flag indicating whether or not headerPath represents the object root resource
     * @param archivalGroupId for AG parts, the id of the containg AG, otherwise null
     * @return populated resource headers
     */
    private ResourceHeadersImpl createHeaders(final OcflObjectSession objSession,
                                              final NonRdfSourceOperation op,
                                              final boolean objectRoot,
                                              final FedoraId archivalGroupId) throws PersistentStorageException {

        final var headers = createCommonHeaders(objSession, op, objectRoot, archivalGroupId);

        ResourceHeaderUtils.populateBinaryHeaders(headers,
                op.getMimeType(),
                op.getFilename(),
                op.getContentSize(),
                op.getContentDigests());

        if (forExternalBinary(op)) {
            // Clear any content path for externals
            headers.setContentPath(null);
            ResourceHeaderUtils.populateExternalBinaryHeaders(headers,
                    op.getContentUri().toString(),
                    op.getExternalHandling());
        } else {
            if (!CREATE.equals(op.getType())) {
                // If performing an update of an internal binary, ensure that no external properties are retained
                headers.setExternalHandling(null);
                headers.setExternalUrl(null);
            }
        }

        return headers;
    }

    /**
     * @param op the operation
     * @return Returns true if the operation involved persisting an external binary
     */
    protected boolean forExternalBinary(final NonRdfSourceOperation op) {
        return op.getContentUri() != null && op.getExternalHandling() != null;
    }
}
