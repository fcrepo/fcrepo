/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.builder;
import static org.fcrepo.services.ServiceHelpers.getClusterExecutor;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.fcrepo.services.functions.CacheLocalTransform;
import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.fcrepo.utils.impl.ChainingCacheStoreEntry;
import org.fcrepo.utils.impl.LocalBinaryStoreEntry;
import org.infinispan.distexec.DistributedExecutorService;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

/**
 * Service for managing access to low-level binary blobs (which may include redundant copies, etc)
 * @author Chris Beer
 * @date Mar 11, 2013
 */
@Component
public class LowLevelStorageService {

    private static final Logger logger =
        getLogger(LowLevelStorageService.class);

    @Inject
    private Repository repo;

    private GetBinaryStore getBinaryStore = new GetBinaryStore();

    private GetBinaryKey getBinaryKey = new GetBinaryKey();

    /**
     * Apply some Function to the low-level cache entries for the Node
     * @param resource a JCR Node containing a jcr:data binary property
     *    (e.g. a jcr:content node)
     * @param transform a Function to transform the cache entries with
     * @throws RepositoryException  if the jcr:data property isn't found, a
     *    RepositoryException is thrown
     */
    public <T> Collection<T> transformLowLevelCacheEntries(final Node resource,
                                                           final Function <LowLevelCacheEntry, T>
                                                           transform)
        throws RepositoryException {
        return transformLowLevelCacheEntries(resource.
                getProperty(JcrConstants.JCR_DATA), transform);
    }
    
    public <T> Collection<T> transformLowLevelCacheEntries(final Property jcrBinaryProperty,
            final Function <LowLevelCacheEntry, T>
            transform)
         throws RepositoryException {

        return transformLowLevelCacheEntries(getBinaryKey.apply(jcrBinaryProperty), transform);
    }
    
    public <T> Collection<T> transformLowLevelCacheEntries(final BinaryKey key,
            final Function <LowLevelCacheEntry, T>
            transform)
         throws RepositoryException {

        final BinaryStore store = getBinaryStore.apply(repo);

        if (store == null) {
            return ImmutableSet.of();
        }
        if (store instanceof CompositeBinaryStore) {
            return
                    transformLowLevelCacheEntries((CompositeBinaryStore) store,
                                                 key, transform);

        } else if (store instanceof InfinispanBinaryStore) {
            try {
                return getClusterResults(
                        (InfinispanBinaryStore) store, key, transform);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                return ImmutableSet.of();
            } catch (ExecutionException e) {
                logger.error(e.getMessage(), e);
                return ImmutableSet.of();
            }
        } else {
            final ImmutableSet.Builder<T> blobs = builder();
            blobs.add(transform.apply(new LocalBinaryStoreEntry(store, key)));
            return blobs.build();
        }
    }
    
    /**
     * Transform low-level cache entries from a particular CompositeBinaryStore
     * @param key a Modeshape BinaryValue's key.
     * @return a set of transformed objects
     */
    protected <T> Set<T> transformLowLevelCacheEntries(
            final CompositeBinaryStore compositeStore,
            final BinaryKey key,
            final Function <LowLevelCacheEntry, T> transform) {

        final ImmutableSet.Builder<T> results = builder();

        Iterator<Map.Entry<String,BinaryStore>> it =
            compositeStore.getNamedStoreIterator();

        while (it.hasNext()) {
            Map.Entry<String,BinaryStore> entry = it.next();

            BinaryStore bs = entry.getValue();

            if (bs.hasBinary(key)) {

                final Function <LowLevelCacheEntry, T> decorator =
                        new ExternalIdDecorator<>(entry.getKey(), transform);
                final Set<T> transformeds =
                        transformLowLevelCacheEntries(bs, key, decorator);
                results.addAll(transformeds);
                
            }
        }

        return results.build();
    }
    
    /**
     * Steer low-level cache entries to transform functions according
     * to the subtype of BinaryStore in question
     * @param key a Modeshape BinaryValue's key.
     * @return a set of transformed objects
     */
    protected <T> Set<T> transformLowLevelCacheEntries(
            final BinaryStore store,
            final BinaryKey key,
            final Function <LowLevelCacheEntry, T> transform) {

        if (store == null) {
            return ImmutableSet.of();
        }
        if (store instanceof CompositeBinaryStore) {
            return
                    transformLowLevelCacheEntries((CompositeBinaryStore) store,
                                                 key, transform);

        } else if (store instanceof InfinispanBinaryStore) {
            try {
                return getClusterResults(
                    (InfinispanBinaryStore) store, key, transform);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                return ImmutableSet.of();
            } catch (ExecutionException e) {
                logger.error(e.getMessage(), e);
                return ImmutableSet.of();
            }
        } else {
            final ImmutableSet.Builder<T> blobs = builder();
            blobs.add(transform.apply(new LocalBinaryStoreEntry(store, key)));
            return blobs.build();
        }
    }


    /**
     * Get the low-level cache entries for a Node containing a jcr:data binary
     * property
     *
     * @param resource a JCR node that has a jcr:data property.
     * @return a map of binary stores and input streams
     * @throws RepositoryException if the jcr:data property isn't found, a
     *   RepositoryException is thrown
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final Node resource)
        throws RepositoryException {

        return getLowLevelCacheEntries(resource.
                                       getProperty(JcrConstants.JCR_DATA));

    }

    /**
     * Get the low-level cache entries for a JCR Binary property
     * @param jcrBinaryProperty a JCR Binary property (e.g. jcr:data)
     * @return a map of binary stores and input streams
     * @throws RepositoryException  if the binary key for property isn't found,
     *   a RepositoryException is thrown
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final Property jcrBinaryProperty)
        throws RepositoryException {

        return getLowLevelCacheEntries(getBinaryKey.apply(jcrBinaryProperty));
    }

    /**
     * Get the low-level Cache entries for a Modeshape BinaryKey
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final BinaryKey key) {

        final BinaryStore store = getBinaryStore.apply(repo);

        if (store == null) {
            return ImmutableSet.of();
        }

        return getLowLevelCacheEntriesFromStore(store, key);
    }

    /**
     * Get the low-level cache entries from a particular BinaryStore
     *
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final BinaryStore store,
                                                                    final BinaryKey key) {

        final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

        if (store instanceof CompositeBinaryStore) {
            for (LowLevelCacheEntry entries:
            transformLowLevelCacheEntries((CompositeBinaryStore) store, key, new Echo())) {
                blobs.add(entries);
            }

        } else if (store instanceof InfinispanBinaryStore) {
            for (LowLevelCacheEntry entries:
            transformLowLevelCacheEntries((InfinispanBinaryStore) store, key, new Echo())) {
                blobs.add(entries);
            }

        } else {
            blobs.add(new LocalBinaryStoreEntry(store, key));
        }
        return blobs.build();
    }

    /**
     * Get the low-level cache entries from a particular CompositeBinaryStore
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final CompositeBinaryStore compositeStore,
                                                                       final BinaryKey key) {
        final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

        for (LowLevelCacheEntry entries:
        transformLowLevelCacheEntries(compositeStore, key, new Echo())) {
            blobs.add(entries);
        }
        return blobs.build();

    }

    /**
     * Get the low-level cache entries from a particular InfinispanBinaryStore
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final InfinispanBinaryStore ispnStore,
                                                                       final BinaryKey key) {

        logger.trace("Retrieving low-level cache entries for key {} from an " +
                     "InfinispanBinaryStore {}", key, ispnStore);

        try {
            return getClusterResults(ispnStore, key, new Echo());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            return ImmutableSet.of();
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
            return ImmutableSet.of();
        }
    }

    public <T> Set<T> getClusterResults(InfinispanBinaryStore cacheStore,
            BinaryKey key, Function<LowLevelCacheEntry, T> transform)
        throws InterruptedException, ExecutionException {
        DistributedExecutorService exec =
                getClusterExecutor(cacheStore);
        @SuppressWarnings( {"synthetic-access", "unchecked", "rawtypes"} )
        List<Future<Collection<T>>> futures = exec.submitEverywhere(
                new CacheLocalTransform(key, new Unroll<T>(transform)));
        Set<T> results = new HashSet<T>(futures.size());
        while(futures.size() > 0) {
            Iterator<Future<Collection<T>>> futureIter =
                    futures.iterator();
            while(futureIter.hasNext()) {
                Future<Collection<T>> future = futureIter.next();
                try {
                    Collection<T> result = future.get(100, TimeUnit.MILLISECONDS);
                    futureIter.remove();
                    results.addAll(result);
                } catch (TimeoutException e) {
                    // we're just going to ignore this and try again!
                }
            }
        }
        return results;
    }

    /**
     * @todo Add Documentation.
     */
    public void setRepository(final Repository repository) {
        repo = repository;
    }

    /**
     * @todo Add Documentation.
     */
    public void setGetBinaryStore(final GetBinaryStore getBinaryStore) {
        this.getBinaryStore = getBinaryStore;
    }

    /**
     * @todo Add Documentation.
     */
    public void setGetBinaryKey(final GetBinaryKey getBinaryKey) {
        this.getBinaryKey = getBinaryKey;
    }

    static class ExternalIdDecorator<T> 
        implements Function<LowLevelCacheEntry, T>, Serializable {

        /**
         * Must be serializable
         */
        private static final long serialVersionUID = 7375231595038804409L;
        final String externalId;
        final Function<LowLevelCacheEntry, T> transform;
        ExternalIdDecorator(String externalId, Function<LowLevelCacheEntry, T> transform) {
            this.externalId = externalId;
            this.transform = transform;
        }

        @Override
        public T apply(LowLevelCacheEntry input) {
            input.setExternalId(externalId);
            return transform.apply(input);
        }

    }
    
    static class Echo implements Function<LowLevelCacheEntry, LowLevelCacheEntry>, Serializable {

        private static final long serialVersionUID = -1L;

        @Override
        public LowLevelCacheEntry apply(LowLevelCacheEntry input) {
            return input;
        }

    }
    
    static class Unroll<T> implements Function<LowLevelCacheEntry, Collection<T>>, Serializable {

        private final Function<LowLevelCacheEntry, T> transform;

        Unroll(Function<LowLevelCacheEntry, T> transform) {
            this.transform = transform;
        }

        private static final long serialVersionUID = -1L;

        @Override
        public Collection<T> apply(LowLevelCacheEntry input) {
            final ImmutableSet.Builder<T> entries = builder();
            if (input instanceof ChainingCacheStoreEntry) {
                entries.addAll(
                        transform(((ChainingCacheStoreEntry)input).chainedEntries(), transform)
                        );
            } else {
                entries.add(transform.apply(input)).build();
            }
            return entries.build();
        }
    }
    
}
