/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.api;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageSession;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public interface Persister {

    /**
     * The method returns true if the operation can be persisted by this persister.
     * @param operation the operation to persist
     * @return true or false
     */
    boolean handle(ResourceOperation operation);

    /**
     * The persistence handling for the given operation.
     *
     * @param session The persistent storage session
     * @param operation The operation and associated data need to perform the operation.
     * @throws PersistentStorageException on failure
     */
    void persist(final OcflPersistentStorageSession session,
            final ResourceOperation operation)
            throws PersistentStorageException;
}
