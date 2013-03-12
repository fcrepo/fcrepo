
package org.fcrepo.services;

import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.Maps.transformEntries;
import static java.lang.Boolean.FALSE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.codec.binary.Hex;
import org.fcrepo.utils.LowLevelCacheStore;
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

    private static List<LowLevelCacheStore> cacheStores;

    private static JcrRepository getRepositoryInstance() {
        return (JcrRepository) readOnlySession.getRepository();
    }

    public static Map<LowLevelCacheStore, Boolean> applyDigestToBlobs(
            final Node resource, final MessageDigest digest,
            final String checksum) throws RepositoryException {
        return applyToBlob(
                resource,
                new Maps.EntryTransformer<LowLevelCacheStore, InputStream, Boolean>() {

                    public Boolean transformEntry(LowLevelCacheStore store,
                            InputStream is) {
                        DigestInputStream ds = null;
                        try {
                            ds =
                                    new DigestInputStream(is,
                                            (MessageDigest) digest.clone());

                            while (ds.read() != -1);

                            String calculatedDigest =
                                    Hex.encodeHexString(ds.getMessageDigest()
                                            .digest());

                            return checksum.equals(calculatedDigest);
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return FALSE;
                    }
                });

    }

    public static
            <T>
            Map<LowLevelCacheStore, T>
            applyToBlob(
                    final Node resource,
                    final Maps.EntryTransformer<LowLevelCacheStore, InputStream, T> transform)
                    throws RepositoryException {
        return transformEntries(getBlobs(resource), transform);
    }

    /**
     *
     * @param resource a JCR node that has a jcr:content/jcr:data child.
     * @return a map of binary stores and input streams
     * @throws RepositoryException
     */
    public static Map<LowLevelCacheStore, InputStream> getBlobs(
            final Node resource) throws RepositoryException {

        final BinaryValue v =
                (BinaryValue) resource.getNode(JCR_CONTENT).getProperty(
                        JCR_DATA).getBinary();

        return getBlobs(v.getKey());

    }

    /**
     *
     * @param key a Modeshape BinaryValue's key.
     * @return a map of binary stores and input streams
     */
    public static Map<LowLevelCacheStore, InputStream> getBlobs(BinaryKey key) {

        ImmutableMap.Builder<LowLevelCacheStore, InputStream> blobs = builder();

        for (LowLevelCacheStore c : getLowLevelCacheStores()) {
            try {
                final InputStream is = c.getInputStream(key);
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
    private static List<LowLevelCacheStore> getLowLevelCacheStores() {
        //TODO I'm assuming the list of stores doesn't change.. probably not a safe assumption      
        if (cacheStores != null) {
            return cacheStores;
        }

        List<LowLevelCacheStore> stores = new ArrayList<>();

        BinaryStore store = getBinaryStore();

        if (store == null) {
            return stores;
        }

        // if we have an Infinispan store, it may have multiple stores (or cluster nodes)
        if (store instanceof InfinispanBinaryStore) {
            InfinispanBinaryStore ispnStore = (InfinispanBinaryStore) store;

            //seems like we have to start it, not sure why.
            ispnStore.start();

            for (Cache<?, ?> c : ispnStore.getCaches()) {

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
                        stores.add(new LowLevelCacheStore(store, s));
                    }
                } else {
                    // just a nice, simple infinispan cache.
                    stores.add(new LowLevelCacheStore(store, cacheStore));
                }
            }
        } else {
            stores.add(new LowLevelCacheStore(store));
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

}
