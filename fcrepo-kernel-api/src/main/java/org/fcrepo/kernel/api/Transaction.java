/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.exception.TransactionRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.time.Duration;
import java.time.Instant;

/**
 * The Fedora Transaction abstraction
 *
 * @author mohideen
 */
public interface Transaction {

    /**
     * Commit the transaction
     */
    void commit();

    /**
     * Commit the transaction only if the transaction is shortLived
     */
    void commitIfShortLived();

    /**
     * @return returns true if this transaction has already been committed
     */
    boolean isCommitted();

    /**
     * Rollback the transaction
     */
    void rollback();

    /**
     * Marks the transaction as failed. Failed transactions cannot be committed but may be rolledback.
     */
    void fail();

    /**
     * @return true if this transaction has been rolled back
     */
    boolean isRolledBack();

    /**
     * @return true if the tx is a long-running tx that has not expired and is not in a
     * COMMITTED, ROLLEDBACK, or FAILED state
     */
    boolean isOpenLongRunning();

    /**
     * @return true if the tx is in an OPEN state and has not expired
     */
    boolean isOpen();

    /**
     * Throws an exception if the tx is not in a COMMITTING state
     * @throws TransactionRuntimeException when not in committing
     */
    void ensureCommitting();

    /**
     * @return true the tx is read-only
     */
    boolean isReadOnly();

    /**
     * Get the transaction id
     *
     * @return the transaction id.
     */
    String getId();

    /**
     * Check if the transaction is short-lived.
     *
     * @return is the transaction short-lived.
     */
    boolean isShortLived();

    /**
     * Set transaction short-lived state.
     *
     * @param shortLived boolean true (short-lived) or false (not short-lived)
     */
    void setShortLived(final boolean shortLived);

    /**
     * Expire a transaction
     */
    void expire();

    /**
     * Has the transaction expired?
     * @return true if expired
     */
    boolean hasExpired();

    /**
    * Update the expiry by the provided amount
    * @param amountToAdd the amount of time to add
    * @return the new expiration date
    */
    Instant updateExpiry(final Duration amountToAdd);

    /**
     * Get the date this session expires
     * @return expiration date, if one exists
     */
    Instant getExpires();

    /**
     * Refresh the transaction to extend its expiration window.
     */
    void refresh();

    /**
     * Acquires an exclusive lock on the specified resource for this transaction.
     *
     * @param resourceId the resource to lock
     * @throws ConcurrentUpdateException if the lock cannot be acquired
     */
    void lockResource(final FedoraId resourceId);

    /**
     * Acquires a non-exclusive lock on the specified resource for this transaction.
     *
     * @param resourceId the resource to lock
     * @throws ConcurrentUpdateException if the lock cannot be acquired
     */
    void lockResourceNonExclusive(final FedoraId resourceId);

    /**
     * Acquire an exclusive lock on the specified resource and any ghost nodes above it for this transaction.
     *
     * @param resourceId the resource to lock
     * @throws ConcurrentUpdateException if the lock cannot be acquired
     */
    void lockResourceAndGhostNodes(final FedoraId resourceId);

    /**
     * Releases any resource locks held by the transaction if the session is short-lived. This method should always be
     * called after handling a request, regardless of the outcome, so that any held locks are released immediately
     * without having to wait for the short-lived transaction to expire.
     */
    void releaseResourceLocksIfShortLived();

    /**
     * Executes the runnable within the tx. While there are active runnables being executed, the tx may not be
     * committed or rolledback. Runnables may only be executed when the tx is in an OPEN state and has not expired.
     *
     * @param runnable the code to execute within the tx
     */
    void doInTx(final Runnable runnable);

    /**
     * Sets the baseUri on the transaction
     * @param baseUri the baseUri of the requests
     */
    void setBaseUri(final String baseUri);

    /**
     * Sets the user-agent on the transaction
     * @param userAgent the request's user-agent
     */
    void setUserAgent(final String userAgent);

    /**
     * After invoking, any accumulated events will be suppressed.
     */
    void suppressEvents();

}
