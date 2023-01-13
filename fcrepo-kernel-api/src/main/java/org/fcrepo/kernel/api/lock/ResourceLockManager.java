/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.lock;

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Responsible for managing write locks on Fedora resources
 *
 * @author pwinckles
 */
public interface ResourceLockManager {

    /**
     * Acquires a lock on the resource, associating it to the txId. If the lock is held by a different transaction,
     * an exception is thrown. If the lock is already held by the same transaction, then it returns successfully.
     *
     * @param txId the transaction id to associate the lock to
     * @param resourceId the resource to lock
     * @param lockType the type of lock we are trying to acquire
     * @throws ConcurrentUpdateException when lock cannot be acquired
     */
    void acquire(final String txId, final FedoraId resourceId, final ResourceLockType lockType);

    /**
     * Releases all of the locks held by the transaction
     *
     * @param txId the transaction id
     */
    void releaseAll(final String txId);

}
