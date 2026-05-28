/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import static org.fcrepo.kernel.api.lock.ResourceLockType.EXCLUSIVE;
import static org.fcrepo.kernel.api.lock.ResourceLockType.NONEXCLUSIVE;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;

/**
 * In memory resource lock manager
 *
 * @author pwinckles
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class InMemoryResourceLockManager implements ResourceLockManager {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryResourceLockManager.class);

    private final Map<String, Set<ResourceLock>> transactionLocks;
    private final Map<FedoraId, Set<ResourceLock>> resourceLocks;

    /**
     * The internal lock is used so that internal to this class there is only one thread at a time acquiring or
     * releasing locks on a specific resource.
     */
    private final Map<String, Object> internalResourceLocks;

    public InMemoryResourceLockManager() {
        transactionLocks = new ConcurrentHashMap<>();
        resourceLocks = new ConcurrentHashMap<>();
        internalResourceLocks = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .<String, Object>build()
                .asMap();
    }

    @Override
    public void acquireExclusive(final String txId, final FedoraId resourceId) {
        acquireInternal(txId, resourceId, EXCLUSIVE);
    }

    @Override
    public void acquireNonExclusive(final String txId, final FedoraId resourceId) {
        acquireInternal(txId, resourceId, NONEXCLUSIVE);
    }

    private void acquireInternal(final String txId, final FedoraId resourceId, final ResourceLockType lockType) {
        final var resourceLock = new ResourceLockImpl(lockType, txId, resourceId);

        if (transactionHoldsAdequateLock(resourceLock)) {
            return;
        }

        synchronized (acquireInternalLock(resourceId)) {
            if (transactionHoldsAdequateLock(resourceLock)) {
                return;
            }

            final var locks = resourceLocks.get(resourceId);

            if (locks != null) {
                for (final var lock : locks) {
                    // Throw an exception if either:
                    // 1. We need an exclusive lock, but another tx already holds any kind of lock
                    // 2. We need a non-exclusive lock, but another tx holds an exclusive lock
                    if ((lockType == EXCLUSIVE && !lock.getTransactionId().equals(txId))
                            || lock.hasLockType(EXCLUSIVE)) {
                        throw new ConcurrentUpdateException(resourceId.getResourceId(), txId, lock.getTransactionId());
                    }
                }
            }

            LOG.debug("Transaction {} acquiring lock on {}", txId, resourceId.getResourceId());

            // This does not need to be a synchronized collection because we already synchronize internally on the
            // resource id, so it's not possible to modify concurrently.
            //
            // Because we're using set to store the resource locks and the resource's identity is based on its
            // transaction id and resource id, then a tx will only ever have at most one lock per resource.
            // This works because we do not release locks individually, but rather all at once.
            resourceLocks.computeIfAbsent(resourceId, key -> new HashSet<>()).add(resourceLock);
            transactionLocks.computeIfAbsent(txId, key -> Sets.newConcurrentHashSet()).add(resourceLock);
        }
    }

    @Override
    public void releaseAll(final String txId) {
        final var txLocks = transactionLocks.remove(txId);
        if (txLocks != null) {
            txLocks.forEach(lock -> {
                LOG.debug("Transaction {} releasing lock on {}", txId, lock);
                synchronized (acquireInternalLock(lock.getResourceId())) {
                    final var locks = resourceLocks.get(lock.getResourceId());
                    locks.remove(lock);
                    if (locks.isEmpty()) {
                        resourceLocks.remove(lock.getResourceId());
                    }
                }
            });
        }
    }

    private Object acquireInternalLock(final FedoraId resourceId) {
        return internalResourceLocks.computeIfAbsent(resourceId.getResourceId(), key -> new Object());
    }

    /**
     * Returns true if the transaction already holds an adequate lock on the resource. This means that it holds an
     * exclusive lock if an exclusive lock is requested, or any lock if a non-exclusive lock is requested.
     *
     * @param requested the requested resource lock
     * @return true if the transaction already holds an adequate lock
     */
    private boolean transactionHoldsAdequateLock(final ResourceLock requested) {
        final var locks = transactionLocks.get(requested.getTransactionId());

        if (locks == null) {
            return false;
        }

        final var held = locks.stream().filter(l -> Objects.equals(requested, l)).findFirst();

        return held.map(l -> l.isAdequate(requested.getLockType()))
                .orElse(false);
    }

}
