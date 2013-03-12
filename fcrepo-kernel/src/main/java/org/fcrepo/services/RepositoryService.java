package org.fcrepo.services;

import org.fcrepo.utils.BinaryCacheStore;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.AbstractBinaryStore;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public class RepositoryService {


    private static final Logger logger = LoggerFactory
            .getLogger(RepositoryService.class);


    @Inject
    private Repository repo;

    /**
     * For use with non-mutating methods.
     */
    private static Session readOnlySession;

    private static List<BinaryCacheStore> cacheStores;

    private static JcrRepository getRepositoryInstance() {
        return (JcrRepository)readOnlySession.getRepository();
    }

    /**
     *
     * @param resource a JCR node that has a jcr:content/jcr:data child.
     * @return a map of binary stores and input streams
     * @throws RepositoryException
     */
    public static HashMap<BinaryCacheStore, InputStream> getContentBlobs(Node resource) throws RepositoryException {

        BinaryValue v = (BinaryValue) resource.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();

        return getBlobs(v.getKey());

    }

    /**
     *
     * @param key a Modeshape BinaryValue's key.
     * @return a map of binary stores and input streams
     */
    public static HashMap<BinaryCacheStore, InputStream> getBlobs(BinaryKey key) {

        HashMap<BinaryCacheStore, InputStream> blobs = new LinkedHashMap<BinaryCacheStore, InputStream>();

        for( BinaryCacheStore c : getLowLevelCacheStores()) {
            try {
                blobs.put(c, c.getInputStream(key));
            } catch (BinaryStoreException e) {
                e.printStackTrace(); //uh oh, we didn't find anything!
                blobs.put(c, null);
            }
        }

        return blobs;
    }

    /**
     * Extract the BinaryStore out of Modeshape (infinspan, jdbc, file, transient, etc)
     * @return
     */
    private static BinaryStore getBinaryStore() {
        try {

            JcrRepository jcrRepository = getRepositoryInstance();

            return jcrRepository.getConfiguration().getBinaryStorage().getBinaryStore();

        } catch (Exception e) {      // boo, catching all exceptions. unfortunately, that's all getBinaryStore promises..
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Get the list of low-level cache stores at play. If it's an infinispan node, for instance, figure out exactly
     * which cachestores are being used.
     *
     * @return a list of "BinaryCacheStore", an abstraction over a plain BinaryStore or a specific Infinispan Cache
     */
    private static List<BinaryCacheStore> getLowLevelCacheStores() {
        // I'm assuming the list of stores doesn't change.. probably not a safe assumption
        if(cacheStores != null) {
            return cacheStores;
        }

        List<BinaryCacheStore> stores = new ArrayList<>();

        BinaryStore store = getBinaryStore();

        if(store == null) {
            return stores;
        }

        // if we have an Infinispan store, it may have multiple stores (or cluster nodes)
        if(store instanceof InfinispanBinaryStore) {
            InfinispanBinaryStore ispnStore = (InfinispanBinaryStore)store;

            //seems like we have to start it, not sure why.
            ispnStore.start();

            for(Cache c : ispnStore.getCaches()) {

                final CacheStore cacheStore = c.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore();

                // A ChainingCacheStore indicates we (may) have multiple CacheStores at play
                if(cacheStore instanceof ChainingCacheStore) {
                    final ChainingCacheStore chainingCacheStore = (ChainingCacheStore) cacheStore;

                    // the stores are a map of the cache store and the configuration; i'm just throwing the configuration away..
                    for( CacheStore s : chainingCacheStore.getStores().keySet()) {
                        stores.add(new BinaryCacheStore(store, s));
                    }

                }  else {
                    // just a nice, simple infinispan cache.
                    stores.add(new BinaryCacheStore(store, cacheStore));
                }

            }
        } else {
            stores.add(new BinaryCacheStore(store));
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
