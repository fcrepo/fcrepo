/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.lock;

/**
 * A simple lock with a type, transaction and resource.
 * <p>
 * It is essential that implementations of this interface implement {@link Object#equals(Object)} and
 * {@link Object#hashCode()} so that all locks with the same transaction id and resource id are considered equal.
 *
 * @author whikloj
 * @since 6.3.1
 */
public interface ResourceLock {

    /**
     * @return the resource ID that is locked.
     */
    String getResourceId();

    /**
     * Does this lock hold the mentioned item?
     * @param resourceId the resource ID.
     * @return true if this lock holds it.
     */
    boolean hasResource(final String resourceId);

    /**
     * @return the lock type
     */
    ResourceLockType getLockType();

    /**
     * Does this lock type match the provided one.
     * @param lockType the provided lock type
     * @return true if matches.
     */
    boolean hasLockType(final ResourceLockType lockType);

    /**
     * If an exclusive lock is requested, returns true only if this lock is exclusive. If non-exclusive is requested,
     * then true is always returned.
     *
     * @param lockType the type of lock requested
     * @return true if this lock is adequate
     */
    boolean isAdequate(final ResourceLockType lockType);

    /**
     * @return the transaction ID for this lock.
     */
    String getTransactionId();
}
