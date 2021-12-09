/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;

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

    @Inject
    private ContainmentIndex containmentIndex;

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
    public void acquire(final Transaction tx, final FedoraId resourceId) {
        final var resourceIdStr = resourceId.getResourceId();

        if (transactionHoldsLock(tx.getId(), resourceIdStr)) {
            return;
        }

        synchronized (acquireInternalLock(resourceIdStr)) {
            if (transactionHoldsLock(tx.getId(), resourceIdStr)) {
                return;
            }

            if (lockedResources.contains(resourceIdStr)) {
                throw new ConcurrentUpdateException(
                        String.format("Cannot update %s because it is being updated by another transaction.",
                                resourceIdStr));
            }
            final String estimateParentPath = resourceIdStr.indexOf('/') > -1 ?
                    resourceIdStr.substring(0,resourceIdStr.lastIndexOf('/')) : resourceIdStr;
            // NonRdfSourceDescriptions will resolve to their NonRdfSource and cause a false failure.
            if (!resourceId.isDescription()) {
                final var actualParent = containmentIndex.getContainerIdByPath(tx, resourceId, false);

                if (!estimateParentPath.equals(actualParent.getResourceId())) {
                    // If the expected parent does not match the actual parent, then we have ghost nodes.
                    // Add them as well.
                    LOG.debug("Getting lock for ghost parents as well.");
                    final List<String> ghostPaths = Arrays.stream(estimateParentPath
                            .replace(actualParent.getResourceId(), "")
                            .split("/")).filter(a -> !a.isBlank()).collect(Collectors.toList());
                    FedoraId tempParent = actualParent;
                    for (final String part : ghostPaths) {
                        tempParent = tempParent.resolve(part);
                        final var parentId = tempParent.getResourceId();
                        if (lockedResources.contains(parentId)) {
                            throw new ConcurrentUpdateException(
                                    String.format(
                                            "Cannot update %s because it is being updated by another transaction.",
                                            resourceIdStr));
                        }
                        lockedResources.add(parentId);
                        transactionLocks.computeIfAbsent(tx.getId(), key -> Sets.newConcurrentHashSet())
                                .add(tempParent.getResourceId());
                    }
                }
            }

            LOG.debug("Transaction {} acquiring lock on {}", tx.getId(), resourceIdStr);

            lockedResources.add(resourceIdStr);
            transactionLocks.computeIfAbsent(tx.getId(), key -> Sets.newConcurrentHashSet())
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
