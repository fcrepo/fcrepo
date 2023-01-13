/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import org.fcrepo.kernel.api.lock.ResourceLock;
import org.fcrepo.kernel.api.lock.ResourceLockType;

/**
 * Simple implementation of the complex lock.
 * @author whikloj
 */
public class ResourceLockImpl implements ResourceLock {

    private String transactionId;
    private ResourceLockType resourceLockType;
    private String resourceId;

    ResourceLockImpl(final ResourceLockType resourceLock, final String txId, final String id) {
        resourceLockType = resourceLock;
        transactionId = txId;
        resourceId = id;
    }

    ResourceLockImpl(final String lockType, final String txId, final String resourceId) {
        this(ResourceLockType.fromString(lockType), txId, resourceId);
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public boolean hasResource(final String resourceId) {
        return this.resourceId.equals(resourceId);
    }

    @Override
    public ResourceLockType getLockType() {
        return resourceLockType;
    }

    @Override
    public boolean hasLockType(final ResourceLockType lockType) {
        return resourceLockType.equals(lockType);
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public String toString() {
        return String.format("type: %s, txId: %s, resource: %s", resourceLockType, transactionId,
                resourceId);
    }
}
