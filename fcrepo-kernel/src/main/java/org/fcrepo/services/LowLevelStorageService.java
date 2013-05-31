/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.builder;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.services.functions.GetCacheStore;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 11, 2013
 */
public class LowLevelStorageService {

    private static final Logger logger =
        getLogger(LowLevelStorageService.class);

    @Inject
    private Repository repo;

    private GetBinaryStore getBinaryStore = new GetBinaryStore();

    private GetBinaryKey getBinaryKey = new GetBinaryKey();

    private GetCacheStore getCacheStore = new GetCacheStore();


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
        return transform(getLowLevelCacheEntries(resource), transform);
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
     * Get the low-level Cache etnries for a Modeshpae BinaryKey
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final BinaryKey key) {

        final BinaryStore store = getBinaryStore.apply(repo);

        if (store == null) {
            return new HashSet<>();
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

        if (store instanceof CompositeBinaryStore) {
            return
                getLowLevelCacheEntriesFromStore((CompositeBinaryStore) store,
                                                 key);

        } else if (store instanceof InfinispanBinaryStore) {
            return
                getLowLevelCacheEntriesFromStore((InfinispanBinaryStore) store,
                                                 key);

        } else {
            final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();
            blobs.add(new LowLevelCacheEntry(store, key));
            return blobs.build();
        }

    }

    /**
     * Get the low-level cache entries from a particular CompositeBinaryStore
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final CompositeBinaryStore compositeStore,
                                                                       final BinaryKey key) {

        final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

        Iterator<Map.Entry<String,BinaryStore>> it =
            compositeStore.getNamedStoreIterator();

        while (it.hasNext()) {
            Map.Entry<String,BinaryStore> entry = it.next();

            BinaryStore bs = entry.getValue();

            if (bs.hasBinary(key)) {

                final Set<LowLevelCacheEntry> binaryBlobs =
                    getLowLevelCacheEntriesFromStore(bs, key);

                for (LowLevelCacheEntry e : binaryBlobs) {
                    e.setExternalId(entry.getKey());
                    blobs.add(e);
                }
            }
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

        final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

        final List<Cache<?,?>> caches = ispnStore.getCaches();
        logger.trace("Found {} caches", caches.size());
        for (final Cache<?, ?> c : ImmutableSet.copyOf(caches)) {
            logger.trace("Looking in Infinispan cache {}", c);

            final CacheStore cacheStore = getCacheStore.apply(c);

            if (cacheStore == null) {
                logger.trace("Could not retrieve cache store from component " +
                             "registry");
                continue;
            }

            // A ChainingCacheStore indicates we (may) have multiple CacheStores
            // at play
            if (cacheStore instanceof ChainingCacheStore) {
                logger.trace("Found a ChainingCacheStore; looking in each " +
                             "cache store for key");
                final ChainingCacheStore chainingCacheStore =
                    (ChainingCacheStore) cacheStore;
                final LinkedHashMap<CacheStore,CacheStoreConfiguration> stores =
                    chainingCacheStore.getStores();
                logger.trace("Found {} chained cache stores", stores.size());

                for (final CacheStore s : stores.keySet()) {
                    final Set<LowLevelCacheEntry>
                        lowLevelCacheEntriesFromStore =
                        getLowLevelCacheEntriesFromStore(ispnStore, s, key);

                    blobs.addAll(lowLevelCacheEntriesFromStore);
                }
            } else {
                // just a nice, simple infinispan cache.
                blobs.addAll(getLowLevelCacheEntriesFromStore(ispnStore,
                                                              cacheStore, key));
            }
        }

        return blobs.build();
    }

    /**
     * Get the low-level cache entries for a particular Cache Store inside an
     * InfinispanBinaryStore
     *
     * @param ispnStore an InfinispanBinaryStore to look in
     * @param cacheStore an Infinispan CacheStore reference
     * @param key a Modeshape BinaryValue key
     * @return
     */
    private Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final InfinispanBinaryStore ispnStore,
                                                                     final CacheStore cacheStore,
                                                                     final BinaryKey key) {

        final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();
        logger.trace("Looking in Infinispan CacheStore {}", cacheStore);

        try {
            if (cacheStore.containsKey(key + "-data-0")) {
                logger.trace("Found first part of binary in CacheStore {}",
                             cacheStore);
                blobs.add(new LowLevelCacheEntry(ispnStore, cacheStore, key));
            } else {
                logger.trace("CacheStore {} did not contain the first part " +
                             "of our binary", cacheStore);
            }
        } catch (CacheLoaderException e) {
            logger.warn("Cache loader raised exception: {}", e);
        }

        return blobs.build();

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

    /**
     * @todo Add Documentation.
     */
    public void setGetCacheStore(final GetCacheStore getCacheStore) {
        this.getCacheStore = getCacheStore;
    }

}
