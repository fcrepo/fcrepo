/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api;

import org.fcrepo.kernel.api.Transaction;

/**
 * Interface to access PersistentStorageSessions.
 *
 * @author whikloj
 * @author dbernstein
 * @since 2019-09-19
 */
public interface PersistentStorageSessionManager {

    /**
     * Retrieve a PersistentStorageSession.
     *
     * @param transaction the externally generated transaction.
     * @return the PersistentStorageSession instance.
     */
    PersistentStorageSession getSession(final Transaction transaction);

    /**
     * Retrieve a read-only PersistentStorageSession. Clients should expect
     * invocation on storage modifying methods to throw exception.
     *
     * @return the PersistentStorageSession instance.
     */
    PersistentStorageSession getReadOnlySession();

    /**
     * Removes the indicated session. If the session does not exist, null is returned.
     *
     * @param sessionId the id of the session to remove
     * @return the session, if it exists
     */
    PersistentStorageSession removeSession(final String sessionId);

    /**
     * Clears all sessions. This is useful for cleaning up after a shutdown when sessions were not able to rollback.
     */
    void clearAllSessions();
}
