/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.ReindexResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.REINDEX;

/**
 * Reindex resource rersister
 *
 * @author dbernstein
 */
class ReindexResourcePersister implements Persister {

    private static final Logger log = LoggerFactory.getLogger(ReindexResourcePersister.class);

    private ReindexService reindexService;

    /**
     * Constructor
     *
     * @param reindexService the reindex service
     */
    protected ReindexResourcePersister(final ReindexService reindexService) {
        this.reindexService = reindexService;
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
        return operation != null && REINDEX.equals(operation.getType());
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var ocflId = operation.getResourceId().getBaseId();
        final ReindexResourceOperation reindexOp = (ReindexResourceOperation) operation;
        try {
            this.reindexService.indexOcflObject(reindexOp.getTransaction(), ocflId);
        } catch (final Exception ex) {
            throw new PersistentStorageException(ex.getMessage(), ex);
        }
    }
}
