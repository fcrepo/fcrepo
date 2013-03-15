
package org.fcrepo.services;

import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.Maps.transformEntries;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.utils.ContentDigest;
import org.fcrepo.utils.FixityInputStream;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class LowLevelStorageService {

    private static final Logger logger = LoggerFactory
            .getLogger(LowLevelStorageService.class);

    @Inject
    private Repository repo;

    /**
     * For use with non-mutating methods.
     */
    private static Session readOnlySession;

    private static List<LowLevelCacheEntry> cacheStores;

    private static JcrRepository getRepositoryInstance() {
        return (JcrRepository) readOnlySession.getRepository();
    }

    public static Collection<FixityResult> getFixity(
            final Node resource, final MessageDigest digest) throws RepositoryException {
        logger.debug("Checking resource: " + resource.getPath());

        return transformBinaryBlobs(
                resource,
                new Maps.EntryTransformer<LowLevelCacheEntry, InputStream, FixityResult>() {

                    public FixityResult transformEntry(LowLevelCacheEntry entry,
                                                       InputStream is) {
                        logger.debug("Checking fixity for resource in cache store " + entry.toString());
                        FixityResult result = null;
                        FixityInputStream ds = null;
                        try {
                            ds =
                                    new FixityInputStream(is,
                                            (MessageDigest) digest.clone());

                            result = new FixityResult(entry);
                            result.storeIdentifier = entry.getExternalIdentifier();
                            //result.
                            while (ds.read() != -1) ;

                            String calculatedDigest =
                                    encodeHexString(ds.getMessageDigest()
                                            .digest());
                            result.computedChecksum = ContentDigest.asURI(digest.getAlgorithm(), calculatedDigest);
                            result.computedSize = ds.getByteCount();

                            logger.debug("Got " + result.toString());
                            ds.close();

                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new IllegalStateException(e);
                        }

                        return result;
                    }

                }).values();
    }

    public static
            <T>
            Map<LowLevelCacheEntry, T>
            transformBinaryBlobs(
                    final Node resource,
                    final Maps.EntryTransformer<LowLevelCacheEntry, InputStream, T> transform)
                    throws RepositoryException {
        return transformEntries(getBinaryBlobs(resource), transform);
    }

    /**
     *
     * @param resource a JCR node that has a jcr:content/jcr:data child.
     * @return a map of binary stores and input streams
     * @throws RepositoryException
     */
    public static Map<LowLevelCacheEntry, InputStream> getBinaryBlobs(
            final Node resource) throws RepositoryException {

        final BinaryValue v =
                (BinaryValue) resource.getNode(JCR_CONTENT).getProperty(
                        JCR_DATA).getBinary();

        return getBinaryBlobs(v.getKey());

    }

    /**
     *
     * @param key a Modeshape BinaryValue's key.
     * @return a map of binary stores and input streams
     */
    public static Map<LowLevelCacheEntry, InputStream> getBinaryBlobs(BinaryKey key) {

        ImmutableMap.Builder<LowLevelCacheEntry, InputStream> blobs = builder();

        for (LowLevelCacheEntry c : getLowLevelCacheStores(key)) {
            try {
                final InputStream is = c.getInputStream();
                blobs.put(c, is);
            } catch (BinaryStoreException e) {
                //we didn't find anything.
            }
        }
        return blobs.build();
    }

    /**
     * Extract the BinaryStore out of Modeshape (infinspan, jdbc, file, transient, etc)
     * @return
     */
    private static BinaryStore getBinaryStore() {
        try {
            return getRepositoryInstance().getConfiguration()
                    .getBinaryStorage().getBinaryStore();
        } catch (Exception e) { // boo, catching all exceptions. unfortunately, that's all getBinaryStore promises..
            throw new IllegalStateException(e);
        }

    }

    /**
     * Get the list of low-level cache stores at play. If it's an infinispan node, for instance, figure out exactly
     * which cachestores are being used.
     *
     * @return a list of "BinaryCacheStore", an abstraction over a plain BinaryStore or a specific Infinispan Cache
     */
    private static List<LowLevelCacheEntry> getLowLevelCacheStores(BinaryKey key) {
        //TODO I'm assuming the list of stores doesn't change.. probably not a safe assumption      
        if (cacheStores != null) {
            return cacheStores;
        }

        List<LowLevelCacheEntry> stores = new ArrayList<LowLevelCacheEntry>();

        BinaryStore store = getBinaryStore();

        if (store == null) {
            return stores;
        }

        // if we have an Infinispan store, it may have multiple stores (or cluster nodes)
        if (store instanceof InfinispanBinaryStore) {
            InfinispanBinaryStore ispnStore = (InfinispanBinaryStore) store;

            //seems like we have to start it, not sure why.
            ispnStore.start();

            for (Cache<?, ?> c : ImmutableSet.copyOf(ispnStore.getCaches())) {

                final CacheStore cacheStore =
                        c.getAdvancedCache().getComponentRegistry()
                                .getComponent(CacheLoaderManager.class)
                                .getCacheStore();

                // A ChainingCacheStore indicates we (may) have multiple CacheStores at play
                if (cacheStore instanceof ChainingCacheStore) {
                    final ChainingCacheStore chainingCacheStore =
                            (ChainingCacheStore) cacheStore;
                    // the stores are a map of the cache store and the configuration; i'm just throwing the configuration away..
                    for (CacheStore s : chainingCacheStore.getStores().keySet()) {
                        stores.add(new LowLevelCacheEntry(store, s, key));
                    }
                } else {
                    // just a nice, simple infinispan cache.
                    stores.add(new LowLevelCacheEntry(store, cacheStore, key));
                }
            }
        } else {
            stores.add(new LowLevelCacheEntry(store, key));
        }

        cacheStores = stores;

        return stores;
    }

    @PostConstruct
    public final void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public final void logoutSession() {
        readOnlySession.logout();
    }

    public void setRepository(Repository repository) {
        if(readOnlySession != null) {
            logoutSession();
        }
        repo = repository;

        getSession();
    }

}
