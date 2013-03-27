
package org.fcrepo.services;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Sets.difference;
import static com.yammer.metrics.MetricRegistry.name;
import static java.security.MessageDigest.getInstance;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.fcrepo.utils.FixityResult.FixityState.REPAIRED;
import static org.fcrepo.utils.FixityResult.FixityState.SUCCESS;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.yammer.metrics.Counter;
import com.yammer.metrics.Timer;

public class LowLevelStorageService {

    private static final Logger logger = getLogger(LowLevelStorageService.class);

    final static Counter fixityCheckCounter = metrics.counter(name(
    		LowLevelStorageService.class, "fixity-check-counter"));

    final static Timer timer = metrics.timer(name(Datastream.class,
            "fixity-check-time"));

    final static Counter fixityRepairedCounter = metrics.counter(name(
    		LowLevelStorageService.class, "fixity-repaired-counter"));

    final static Counter fixityErrorCounter = metrics.counter(name(
    		LowLevelStorageService.class, "fixity-error-counter"));

    @Inject
    private Repository repo;

    /**
     * For use with non-mutating methods.
     */
    private Session readOnlySession;

    private JcrRepository getRepositoryInstance() {
        return (JcrRepository) readOnlySession.getRepository();
    }

    public Collection<FixityResult> getFixity(
            final Node resource, final MessageDigest digest,
            final URI dsChecksum, final long dsSize) throws RepositoryException {
        logger.debug("Checking resource: " + resource.getPath());

        return transformBinaryBlobs(
                resource,
                new Maps.EntryTransformer<LowLevelCacheEntry, InputStream, FixityResult>() {

                    public FixityResult transformEntry(LowLevelCacheEntry entry,
                                                       InputStream is) {
                        logger.debug("Checking fixity for resource in cache store " + entry.toString());
                        FixityResult result = null;
                        try {
                        	result = entry.checkFixity(dsChecksum, dsSize, digest);
                        } catch (BinaryStoreException e) {
                            e.printStackTrace();
                            throw new IllegalStateException(e);
						}
                        return result;
                    }

                }).values();
    }

    public <T> Map<LowLevelCacheEntry, T> transformBinaryBlobs(
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
    public Map<LowLevelCacheEntry, InputStream> getBinaryBlobs(
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
    public Map<LowLevelCacheEntry, InputStream> getBinaryBlobs(BinaryKey key) {

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
    private BinaryStore getBinaryStore() {
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
    private List<LowLevelCacheEntry> getLowLevelCacheStores(BinaryKey key) {

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
    
    public Collection<FixityResult> runFixityAndFixProblems(Datastream datastream)
            throws RepositoryException {
        Set<FixityResult> fixityResults;
        Set<FixityResult> goodEntries;
        final URI digestUri = datastream.getContentDigest();
        final long size = datastream.getContentSize();
        MessageDigest digest;

        fixityCheckCounter.inc();

        try {
            digest = getInstance(datastream.getContentDigestType());
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException(e.getMessage(), e);
        }

        final Timer.Context context = timer.time();

        try {
            fixityResults = copyOf(getFixity(datastream.getNode(), digest, digestUri, size));

            goodEntries =
                    copyOf(filter(fixityResults, new Predicate<FixityResult>() {

                        @Override
                        public boolean apply(FixityResult result) {
                            return result.computedChecksum.equals(digestUri) &&
                                    result.computedSize == size;
                        };
                    }));
        } finally {
            context.stop();
        }

        if (goodEntries.size() == 0) {
            logger.error("ALL COPIES OF " + datastream.getObject().getName() + "/" +
            		datastream.getDsId() + " HAVE FAILED FIXITY CHECKS.");
            return fixityResults;
        }

        final LowLevelCacheEntry anyGoodCacheEntry =
                goodEntries.iterator().next().getEntry();

        final Set<FixityResult> badEntries =
                difference(fixityResults, goodEntries);

        for (final FixityResult result : badEntries) {
            try {
                result.getEntry()
                        .storeValue(anyGoodCacheEntry.getInputStream());
                final FixityResult newResult =
                        result.getEntry().checkFixity(digestUri, size, digest);
                if (newResult.status.contains(SUCCESS)) {
                    result.status.add(REPAIRED);
                    fixityRepairedCounter.inc();
                } else {
                    fixityErrorCounter.inc();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fixityResults;
    }

}
