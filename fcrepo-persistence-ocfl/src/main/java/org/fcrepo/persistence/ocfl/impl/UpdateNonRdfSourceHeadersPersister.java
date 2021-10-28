/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE_HEADERS;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperation;
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
public class UpdateNonRdfSourceHeadersPersister extends AbstractRdfSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(UpdateRdfSourcePersister.class);

    /**
     * Constructor
     *
     * @param fedoraOcflIndex the FedoraToOcflObjectIndex
     */
    public UpdateNonRdfSourceHeadersPersister(final FedoraToOcflObjectIndex fedoraOcflIndex) {
        super(UpdateNonRdfSourceHeadersOperation.class, UPDATE_HEADERS, fedoraOcflIndex);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
        throws PersistentStorageException {
        final var resourceId = operation.getResourceId();
        log.debug("persisting {} headers to {}", resourceId, session);

        final var fedoraOcflMapping = getMapping(operation.getTransaction(), resourceId);
        final var ocflId = fedoraOcflMapping.getOcflObjectId();
        final OcflObjectSession objSession = session.findOrCreateSession(ocflId);

        // unlike with normal updates we don't want to clear the digests/content-size, just the server managed headers
        // in the event the server managed mode is strict, all values will be null
        final var headers = new ResourceHeadersAdapter(objSession.readHeaders(resourceId.getResourceId()));

        final var updateHeadersOp = (UpdateNonRdfSourceHeadersOperation) operation;
        final var createdDate = updateHeadersOp.getCreatedDate();
        final var lastModifiedDate = updateHeadersOp.getLastModifiedDate();
        final var createdBy = updateHeadersOp.getCreatedBy();
        final var lastModifiedBy = updateHeadersOp.getLastModifiedBy();
        final var mimetype = updateHeadersOp.getMimeType();
        final var filename = updateHeadersOp.getFilename();
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
        if (mimetype != null) {
            headers.setMimeType(mimetype);
        }
        if (filename != null) {
            headers.setFilename(filename);
        }

        objSession.writeHeaders(headers.asStorageHeaders());
    }

}
