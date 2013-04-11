
package org.fcrepo.services;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.builder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.difference;
import static com.yammer.metrics.MetricRegistry.name;
import static java.security.MessageDigest.getInstance;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.fcrepo.utils.FixityResult.FixityState.REPAIRED;
import static org.fcrepo.utils.FixityResult.FixityState.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.services.functions.GetBinaryStore;
import org.fcrepo.services.functions.GetCacheStore;
import org.fcrepo.services.functions.GetGoodFixityResults;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.yammer.metrics.Counter;
import com.yammer.metrics.Timer;

public class LowLevelStorageService {

    private static final Logger logger =
            getLogger(LowLevelStorageService.class);

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

    GetBinaryStore getBinaryStore = new GetBinaryStore();

    GetBinaryKey getBinaryKey = new GetBinaryKey();

    GetCacheStore getCacheStore = new GetCacheStore();

    GetGoodFixityResults getGoodFixityResults = new GetGoodFixityResults();

    /**
     * For use with non-mutating methods.
     */
    private Session readOnlySession;

    public Collection<FixityResult>
            getFixity(final Node resource, final MessageDigest digest,
                    final URI dsChecksum, final long dsSize)
                    throws RepositoryException {
        logger.debug("Checking resource: " + resource.getPath());

        return transformBinaryBlobs(resource, ServiceHelpers
                .getCheckCacheFixityFunction(digest, dsChecksum, dsSize));
    }

    public <T> Collection<T> transformBinaryBlobs(final Node resource,
            final Function<LowLevelCacheEntry, T> transform)
            throws RepositoryException {
        return transform(getBinaryBlobs(resource), transform);
    }

    /**
     *
     * @param resource a JCR node that has a jcr:content/jcr:data child.
     * @return a map of binary stores and input streams
     * @throws RepositoryException
     */
    public Set<LowLevelCacheEntry> getBinaryBlobs(final Node resource)
            throws RepositoryException {

        return getBinaryBlobs(getBinaryKey.apply(resource));

    }

    /**
     *
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getBinaryBlobs(BinaryKey key) {

        ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

        BinaryStore store = getBinaryStore.apply(repo);

        if (store == null) {
            return blobs.build();
        }

        // if we have an Infinispan store, it may have multiple stores (or cluster nodes)
        if (store instanceof InfinispanBinaryStore) {
            InfinispanBinaryStore ispnStore = (InfinispanBinaryStore) store;

            //seems like we have to start it, not sure why.
            ispnStore.start();

            for (Cache<?, ?> c : ImmutableSet.copyOf(ispnStore.getCaches())) {

                final CacheStore cacheStore = getCacheStore.apply(c);

                // A ChainingCacheStore indicates we (may) have multiple CacheStores at play
                if (cacheStore instanceof ChainingCacheStore) {
                    final ChainingCacheStore chainingCacheStore =
                            (ChainingCacheStore) cacheStore;
                    // the stores are a map of the cache store and the configuration; i'm just throwing the configuration away..
                    for (CacheStore s : chainingCacheStore.getStores().keySet()) {
                        blobs.add(new LowLevelCacheEntry(store, s, key));
                    }
                } else {
                    // just a nice, simple infinispan cache.
                    blobs.add(new LowLevelCacheEntry(store, cacheStore, key));
                }
            }
        } else {
            blobs.add(new LowLevelCacheEntry(store, key));
        }

        return blobs.build();
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
        if (readOnlySession != null) {
            logoutSession();
        }
        repo = repository;

        getSession();
    }

    public Collection<FixityResult> runFixityAndFixProblems(
            Datastream datastream) throws RepositoryException {
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
            fixityResults =
                    copyOf(getFixity(datastream.getNode(), digest, digestUri,
                            size));

            goodEntries = getGoodFixityResults.apply(fixityResults);
        } finally {
            context.stop();
        }

        if (goodEntries.size() == 0) {
            logger.error("ALL COPIES OF " + datastream.getObject().getName() +
                    "/" + datastream.getDsId() + " HAVE FAILED FIXITY CHECKS.");
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
                logger.warn("Exception repairing low-level cache entry: {}", e);
            }
        }

        return fixityResults;
    }

}
