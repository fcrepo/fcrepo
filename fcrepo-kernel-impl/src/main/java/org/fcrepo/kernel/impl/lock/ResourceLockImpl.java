/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLock;
import org.fcrepo.kernel.api.lock.ResourceLockType;

import java.util.Objects;

/**
 * Simple implementation of the complex lock.
 * @author whikloj
 */
public class ResourceLockImpl implements ResourceLock {

    private final String transactionId;
    private final ResourceLockType resourceLockType;
    private final FedoraId resourceId;

    ResourceLockImpl(final ResourceLockType resourceLock, final String txId, final FedoraId id) {
        resourceLockType = resourceLock;
        transactionId = txId;
        resourceId = id;
    }

    ResourceLockImpl(final String lockType, final String txId, final FedoraId resourceId) {
        this(ResourceLockType.fromString(lockType), txId, resourceId);
    }

    @Override
    public FedoraId getResourceId() {
        return resourceId;
    }

    @Override
    public boolean hasResource(final FedoraId resourceId) {
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
    public boolean isAdequate(final ResourceLockType lockType) {
        return this.resourceLockType == ResourceLockType.EXCLUSIVE || lockType == ResourceLockType.NONEXCLUSIVE;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ResourceLockImpl that = (ResourceLockImpl) o;
        return transactionId.equals(that.transactionId) && resourceId.equals(that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, resourceId);
    }
}
