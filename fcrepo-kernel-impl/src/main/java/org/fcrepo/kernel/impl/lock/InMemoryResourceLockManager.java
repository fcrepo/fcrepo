/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * In memory resource lock manager
 *
 * @author pwinckles
 */
@Component
public class InMemoryResourceLockManager implements ResourceLockManager {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryResourceLockManager.class);

    private final Map<String, Set<String>> transactionLocks;
    private final Set<String> lockedResources;
    private final Map<String, Object> internalResourceLocks;

    public InMemoryResourceLockManager() {
        transactionLocks = new ConcurrentHashMap<>();
        lockedResources = Sets.newConcurrentHashSet();
        internalResourceLocks = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .<String, Object>build()
                .asMap();
    }

    @Override
    public void acquire(final String txId, final FedoraId resourceId) {
        final var resourceIdStr = resourceId.getResourceId();

        if (transactionHoldsLock(txId, resourceIdStr)) {
            return;
        }

        synchronized (acquireInternalLock(resourceIdStr)) {
            if (transactionHoldsLock(txId, resourceIdStr)) {
                return;
            }

            if (lockedResources.contains(resourceIdStr)) {
                throw new ConcurrentUpdateException(
                        String.format("Cannot update %s because it is being updated by another transaction.",
                                resourceIdStr));
            }

            LOG.debug("Transaction {} acquiring lock on {}", txId, resourceIdStr);

            lockedResources.add(resourceIdStr);
            transactionLocks.computeIfAbsent(txId, key -> Sets.newConcurrentHashSet())
                    .add(resourceIdStr);
        }
    }

    @Override
    public void releaseAll(final String txId) {
        final var locks = transactionLocks.remove(txId);
        if (locks != null) {
            locks.forEach(resourceId -> {
                LOG.debug("Transaction {} releasing lock on {}", txId, resourceId);
                synchronized (acquireInternalLock(resourceId)) {
                    lockedResources.remove(resourceId);
                    internalResourceLocks.remove(resourceId);
                }
            });
        }
    }

    private Object acquireInternalLock(final String resourceId) {
        return internalResourceLocks.computeIfAbsent(resourceId, key -> new Object());
    }

    private boolean transactionHoldsLock(final String txId, final String resourceId) {
        final var locks = transactionLocks.get(txId);
        return locks != null && locks.contains(resourceId);
    }

}
