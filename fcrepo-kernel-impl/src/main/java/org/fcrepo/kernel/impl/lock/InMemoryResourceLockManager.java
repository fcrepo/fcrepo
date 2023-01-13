/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import static org.fcrepo.kernel.api.lock.ResourceLockType.EXCLUSIVE;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLock;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.lock.ResourceLockType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;

/**
 * In memory resource lock manager
 *
 * @author pwinckles
 */
@Component
public class InMemoryResourceLockManager implements ResourceLockManager {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryResourceLockManager.class);

    private final Map<String, Set<ResourceLock>> transactionLocks;
    private final Map<String, Set<ResourceLock>> internalResourceLocks;
    private final Map<String, Set<ResourceLock>> resourceLocks;

    public InMemoryResourceLockManager() {
        transactionLocks = new ConcurrentHashMap<>();
        resourceLocks = new ConcurrentHashMap<>();
        internalResourceLocks = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .<String, Set<ResourceLock>>build()
                .asMap();
    }

    @Override
    public void acquire(final String txId, final FedoraId resourceId, final ResourceLockType lockType) {
        final var resourceIdStr = resourceId.getResourceId();

        if (transactionHoldsExclusiveLock(txId, resourceIdStr)) {
            return;
        }

        synchronized (acquireInternalLock(txId, resourceIdStr, lockType)) {
            if (transactionHoldsExclusiveLock(txId, resourceIdStr)) {
                return;
            }

            if (resourceLocks.containsKey(resourceIdStr)) {
                final var locks = resourceLocks.get(resourceIdStr);
                if (locks.size() > 0 && (
                        lockType.equals(EXCLUSIVE) && locks.parallelStream().anyMatch(l ->
                        !l.getTransactionId().equals(txId)) ||
                        locks.parallelStream().anyMatch(l -> l.hasLockType(EXCLUSIVE)))) {
                    // Can't get an exclusive lock on a resource that already has any lock not owned by the current
                    // transaction or get any lock if there is already an exclusive lock. If the current transaction
                    // held this exclusive lock we would not have gotten this far.
                       throw new ConcurrentUpdateException(
                               String.format("Cannot update %s because it is being updated by another transaction.",
                                       resourceIdStr));
                }
            }

            LOG.debug("Transaction {} acquiring lock on {}", txId, resourceIdStr);

            final var resourceLock = new ResourceLockImpl(lockType, txId, resourceIdStr);
            resourceLocks.computeIfAbsent(resourceIdStr, key -> Sets.newConcurrentHashSet()).add(resourceLock);
            transactionLocks.computeIfAbsent(txId, key -> Sets.newConcurrentHashSet()).add(resourceLock);
        }
    }

    @Override
    public void releaseAll(final String txId) {
        final var locks = transactionLocks.remove(txId);
        if (locks != null) {
            locks.forEach(lock -> {
                LOG.debug("Transaction {} releasing lock on {}", txId, lock);
                synchronized (acquireInternalLock(txId, lock.getResourceId(), lock.getLockType())) {
                    internalResourceLocks.get(lock.getResourceId()).removeIf(
                            l -> l.getTransactionId().equals(txId));
                    if (internalResourceLocks.get(lock.getResourceId()).size() == 0) {
                        internalResourceLocks.remove(lock.getResourceId());
                    }
                    resourceLocks.get(lock.getResourceId()).removeIf(
                            l -> l.getTransactionId().equals(txId));
                    if (resourceLocks.get(lock.getResourceId()).size() == 0) {
                        resourceLocks.remove(lock.getResourceId());
                    }
                }
            });
        }
    }

    private Object acquireInternalLock(final String txId, final String resourceId, final ResourceLockType lockType) {
        return internalResourceLocks.computeIfAbsent(resourceId,
                key -> Sets.newConcurrentHashSet()).add(new ResourceLockImpl(lockType, txId, resourceId));
    }

    private boolean transactionHoldsExclusiveLock(final String txId, final String resourceId) {
        final var locks = transactionLocks.get(txId);
        return locks != null && locks.parallelStream().anyMatch(r -> r.hasLockType(EXCLUSIVE) &&
                r.hasResource(resourceId));
    }

}
