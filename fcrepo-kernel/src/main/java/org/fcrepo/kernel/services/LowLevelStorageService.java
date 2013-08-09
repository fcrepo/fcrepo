/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.kernel.services;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.builder;
import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.emptySet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.services.functions.CacheLocalTransform;
import org.fcrepo.kernel.services.functions.GetBinaryKey;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.fcrepo.kernel.utils.impl.ChainingCacheStoreEntry;
import org.fcrepo.kernel.utils.impl.LocalBinaryStoreEntry;
import org.infinispan.distexec.DistributedExecutorService;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * Service for managing access to low-level binary blobs (which may include
 * redundant copies, etc)
 * 
 * @author Chris Beer
 * @date Mar 11, 2013
 */
@Component
public class LowLevelStorageService {

    private static final Logger LOGGER =
            getLogger(LowLevelStorageService.class);

    @Inject
    private Repository repo;

    private Echo echo = new Echo();

    private GetBinaryStore getBinaryStore = new GetBinaryStore();

    private GetBinaryKey getBinaryKey = new GetBinaryKey();

    /**
     * Apply some Function to the low-level cache entries for the Node
     * 
     * @param resource a JCR Node containing a jcr:data binary property (e.g. a
     *        jcr:content node)
     * @param transform a Function to transform the cache entries with
     * @throws RepositoryException if the jcr:data property isn't found, a
     *         RepositoryException is thrown
     */
    public <T> Collection<T> transformLowLevelCacheEntries(final Node resource,
        final Function<LowLevelCacheEntry, T> transform)
        throws RepositoryException {
        return transformLowLevelCacheEntries(resource.getProperty(JCR_DATA),
                transform);
    }

    /**
     * Apply some Function to the low-level cache entries for the binary
     * property
     * 
     * @param jcrBinaryProperty a binary property (e.g. jcr:data)
     * @param transform a Function to transform the underlying cache entry with
     * @throws RepositoryException
     */
    public <T> Collection<T> transformLowLevelCacheEntries(
        final Property jcrBinaryProperty,
        final Function<LowLevelCacheEntry, T> transform)
        throws RepositoryException {

        return transformLowLevelCacheEntries(getBinaryKey
                .apply(jcrBinaryProperty), transform);
    }

    /**
     * Apply some Function to the low-level cache entries for a BinaryKey in the
     * store
     * 
     * @param key a BinaryKey in the cache store
     * @param transform a Function to transform the underlying cache entry with
     * @throws RepositoryException
     */
    public <T> Collection<T> transformLowLevelCacheEntries(final BinaryKey key,
        final Function<LowLevelCacheEntry, T> transform)
        throws RepositoryException {

        final BinaryStore store = getBinaryStore.apply(repo);

        return transformLowLevelCacheEntries(store, key, transform);
    }

    /**
     * Transform low-level cache entries from a particular CompositeBinaryStore
     * 
     * @param key a Modeshape BinaryValue's key.
     * @return a set of transformed objects
     */
    protected <T> Set<T> transformLowLevelCacheEntries(
            final CompositeBinaryStore compositeStore, final BinaryKey key,
            final Function<LowLevelCacheEntry, T> transform) {

        final ImmutableSet.Builder<T> results = builder();

        final Iterator<Map.Entry<String, BinaryStore>> it =
                compositeStore.getNamedStoreIterator();

        while (it.hasNext()) {
            final Map.Entry<String, BinaryStore> entry = it.next();
            final BinaryStore bs = entry.getValue();
            if (bs.hasBinary(key)) {
                final Function<LowLevelCacheEntry, T> decorator =
                        new ExternalIdDecorator<>(entry.getKey(), transform);
                final Set<T> transformeds =
                        transformLowLevelCacheEntries(bs, key, decorator);
                results.addAll(transformeds);
            }
        }

        return results.build();
    }

    /**
     * Steer low-level cache entries to transform functions according to the
     * subtype of BinaryStore in question
     * 
     * @param key a Modeshape BinaryValue's key.
     * @return a set of transformed objects
     */
    protected <T> Set<T>
    transformLowLevelCacheEntries(final BinaryStore store,
        final BinaryKey key,
        final Function<LowLevelCacheEntry, T> transform) {

        if (store == null) {
            return emptySet();
        }
        if (store instanceof CompositeBinaryStore) {
            return transformLowLevelCacheEntries((CompositeBinaryStore) store,
                    key, transform);

        } else if (store instanceof InfinispanBinaryStore) {
            try {
                return getClusterResults((InfinispanBinaryStore) store, key,
                        transform);
            } catch (final InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                return emptySet();
            } catch (final ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
                return emptySet();
            }
        } else {
            return of(transform.apply(new LocalBinaryStoreEntry(store, key)));
        }
    }

    /**
     * Get the low-level cache entries for a Node containing a jcr:data binary
     * property
     * 
     * @param resource a JCR node that has a jcr:data property.
     * @return a map of binary stores and input streams
     * @throws RepositoryException if the jcr:data property isn't found, a
     *         RepositoryException is thrown
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final Node resource)
        throws RepositoryException {

        return getLowLevelCacheEntries(resource.getProperty(JCR_DATA));

    }

    /**
     * Get the low-level cache entries for a JCR Binary property
     * 
     * @param jcrBinaryProperty a JCR Binary property (e.g. jcr:data)
     * @return a map of binary stores and input streams
     * @throws RepositoryException if the binary key for property isn't found, a
     *         RepositoryException is thrown
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(
            final Property jcrBinaryProperty) throws RepositoryException {

        return getLowLevelCacheEntries(getBinaryKey.apply(jcrBinaryProperty));
    }

    /**
     * Get the low-level Cache entries for a Modeshape BinaryKey
     * 
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(
        final BinaryKey key) {

        return getLowLevelCacheEntriesFromStore(getBinaryStore.apply(repo), key);
    }

    /**
     * Get the low-level cache entries from a particular BinaryStore
     * 
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(
            final BinaryStore store, final BinaryKey key) {

        return transformLowLevelCacheEntries(store, key, this.echo);
    }

    /**
     * Get the low-level cache entries from a particular CompositeBinaryStore
     * 
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(
            final CompositeBinaryStore compositeStore, final BinaryKey key) {

        return transformLowLevelCacheEntries(compositeStore, key, this.echo);
    }

    /**
     * Get the low-level cache entries from a particular InfinispanBinaryStore
     * 
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(
            final InfinispanBinaryStore ispnStore, final BinaryKey key) {

        LOGGER.trace("Retrieving low-level cache entries for key {} from an "
                + "InfinispanBinaryStore {}", key, ispnStore);

        try {
            return getClusterResults(ispnStore, key, this.echo);
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            return emptySet();
        } catch (final ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            return emptySet();
        }
    }

    /**
     * Get the transform results in a clustered Infinispan binary store
     * 
     * @param cacheStore the Modeshape BinaryStore to use
     * @param key the BinaryKey we want to transform
     * @param transform the Function to apply
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public <T> Set<T>
    getClusterResults(final InfinispanBinaryStore cacheStore,
        final BinaryKey key,
        final Function<LowLevelCacheEntry, T> transform)
        throws InterruptedException, ExecutionException {
        final DistributedExecutorService exec = ServiceHelpers.getClusterExecutor(
                cacheStore);
        @SuppressWarnings({"synthetic-access", "unchecked", "rawtypes"})
        final List<Future<Collection<T>>> futures =
                exec.submitEverywhere(new CacheLocalTransform(key,
                        new Unroll<T>(transform)));
        final Set<T> results = new HashSet<T>(futures.size());

        while (futures.size() > 0) {
            final Iterator<Future<Collection<T>>> futureIter =
                    futures.iterator();
            while (futureIter.hasNext()) {
                final Future<Collection<T>> future = futureIter.next();
                try {
                    final Collection<T> result = future.get(100, MILLISECONDS);
                    futureIter.remove();
                    results.addAll(result);
                } catch (final TimeoutException e) {
                    // we're just going to ignore this and try again!
                }
            }
        }

        return results;
    }

    /**
     * Set the repository (used for testing)
     */
    public void setRepository(final Repository repository) {
        repo = repository;
    }

    /**
     * Set the function to use to retrieve the BinaryStore
     */
    public void setGetBinaryStore(final GetBinaryStore getBinaryStore) {
        this.getBinaryStore = getBinaryStore;
    }

    /**
     * Set the function that retrieves a BinaryKey for a property
     */
    public void setGetBinaryKey(final GetBinaryKey getBinaryKey) {
        this.getBinaryKey = getBinaryKey;
    }

    static class ExternalIdDecorator<T> implements
            Function<LowLevelCacheEntry, T>, Serializable {

        /**
         * Must be serializable
         */
        private static final long serialVersionUID = 7375231595038804409L;

        final String externalId;

        final Function<LowLevelCacheEntry, T> transform;

        ExternalIdDecorator(final String externalId,
                final Function<LowLevelCacheEntry, T> transform) {
            this.externalId = externalId;
            this.transform = transform;
        }

        @Override
        public T apply(final LowLevelCacheEntry input) {
            input.setExternalId(externalId);
            return transform.apply(input);
        }

    }

    static class Echo implements
            Function<LowLevelCacheEntry, LowLevelCacheEntry>, Serializable {

        private static final long serialVersionUID = -1L;

        @Override
        public LowLevelCacheEntry apply(final LowLevelCacheEntry input) {
            return input;
        }

    }

    static class Unroll<T> implements
            Function<LowLevelCacheEntry, Collection<T>>, Serializable {

        private final Function<LowLevelCacheEntry, T> transform;

        Unroll(final Function<LowLevelCacheEntry, T> transform) {
            this.transform = transform;
        }

        private static final long serialVersionUID = -1L;

        @Override
        public Collection<T> apply(final LowLevelCacheEntry input) {
            if (input instanceof ChainingCacheStoreEntry) {
                final ImmutableSet.Builder<T> entries = builder();
                final Collection<T> transformResult =
                        transform(((ChainingCacheStoreEntry) input)
                                .chainedEntries(), transform);
                entries.addAll(transformResult);
                return entries.build();
            } else {
                return of(transform.apply(input));
            }
        }
    }
}
