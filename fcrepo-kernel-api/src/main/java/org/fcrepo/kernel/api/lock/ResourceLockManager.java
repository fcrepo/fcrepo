/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.lock;

import org.fcrepo.kernel.api.Transaction;
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
     * @param tx the transaction to associate the lock to
     * @param resourceId the resource to lock
     * @throws ConcurrentUpdateException when lock cannot be acquired
     */
    void acquire(final Transaction tx, final FedoraId resourceId);

    /**
     * Releases all of the locks held by the transaction
     *
     * @param txId the transaction id
     */
    void releaseAll(final String txId);

}
