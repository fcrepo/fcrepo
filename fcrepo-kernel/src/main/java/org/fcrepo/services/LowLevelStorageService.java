
package org.fcrepo.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.builder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.difference;
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
import java.util.HashSet;
import java.util.Iterator;
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
import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.services.functions.GetCacheStore;
import org.fcrepo.services.functions.GetGoodFixityResults;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

public class LowLevelStorageService {

    private static final Logger logger =
            getLogger(LowLevelStorageService.class);

    static final Counter fixityCheckCounter = metrics.counter(name(
            LowLevelStorageService.class, "fixity-check-counter"));

    static final Timer timer = metrics.timer(name(Datastream.class,
            "fixity-check-time"));

    static final Counter fixityRepairedCounter = metrics.counter(name(
            LowLevelStorageService.class, "fixity-repaired-counter"));

    static final Counter fixityErrorCounter = metrics.counter(name(
            LowLevelStorageService.class, "fixity-error-counter"));

    @Inject
    private Repository repo;

    private GetBinaryStore getBinaryStore = new GetBinaryStore();

    private GetBinaryKey getBinaryKey = new GetBinaryKey();

    private GetCacheStore getCacheStore = new GetCacheStore();

    private GetGoodFixityResults getGoodFixityResults =
            new GetGoodFixityResults();

    /**
     * For use with non-mutating methods.
     */
    private Session readOnlySession;

    public Collection<FixityResult>
            getFixity(final Node resource, final MessageDigest digest,
                    final URI dsChecksum, final long dsSize)
                    throws RepositoryException {
        logger.debug("Checking resource: " + resource.getPath());

        return transformLowLevelCacheEntries(resource, ServiceHelpers
															   .getCheckCacheFixityFunction(digest, dsChecksum, dsSize));
    }

    public <T> Collection<T> transformLowLevelCacheEntries(final Node resource,
														   final Function<LowLevelCacheEntry, T> transform)
            throws RepositoryException {
        return transform(getLowLevelCacheEntries(resource), transform);
    }

    /**
     *
     * @param resource a JCR node that has a jcr:content/jcr:data child.
     * @return a map of binary stores and input streams
     * @throws RepositoryException
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final Node resource)
            throws RepositoryException {

        return getLowLevelCacheEntries(getBinaryKey.apply(resource));

    }

    /**
     *
     * @param key a Modeshape BinaryValue's key.
     * @return a set of binary stores
     */
    public Set<LowLevelCacheEntry> getLowLevelCacheEntries(final BinaryKey key) {

        final BinaryStore store = getBinaryStore.apply(repo);

		if (store == null) {
			return new HashSet<LowLevelCacheEntry>();
		}

		return getLowLevelCacheEntriesFromStore(store, key);
    }

	/**
	 *
	 * @param key a Modeshape BinaryValue's key.
	 * @return a set of binary stores
	 */
	public Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final BinaryStore store, final BinaryKey key) {

		if(store instanceof CompositeBinaryStore) {
			return getLowLevelCacheEntriesFromStore((CompositeBinaryStore) store, key);

		} else if (store instanceof InfinispanBinaryStore) {
			return getLowLevelCacheEntriesFromStore((InfinispanBinaryStore) store, key);

		} else {
			final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();
			blobs.add(new LowLevelCacheEntry(store, key));
			return blobs.build();
		}

	}

	/**
	 *
	 * @param key a Modeshape BinaryValue's key.
	 * @return a set of binary stores
	 */
	protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final CompositeBinaryStore compositeStore, final BinaryKey key) {

		final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

		Iterator<Map.Entry<String,BinaryStore>> it = compositeStore.getNamedStoreIterator();

		while(it.hasNext()) {
			Map.Entry<String,BinaryStore> entry = it.next();

			BinaryStore bs = entry.getValue();

			if(bs.hasBinary(key)) {

				final Set<LowLevelCacheEntry> binaryBlobs = getLowLevelCacheEntriesFromStore(bs, key);

				for(LowLevelCacheEntry e : binaryBlobs) {
					e.setExternalId(entry.getKey());
					blobs.add(e);
				}
			}
		}

		return blobs.build();
	}

	/**
	 *
	 * @param key a Modeshape BinaryValue's key.
	 * @return a set of binary stores
	 */
	protected Set<LowLevelCacheEntry> getLowLevelCacheEntriesFromStore(final InfinispanBinaryStore ispnStore, final BinaryKey key) {

		final ImmutableSet.Builder<LowLevelCacheEntry> blobs = builder();

		for (final Cache<?, ?> c : ImmutableSet.copyOf(ispnStore.getCaches())) {

			final CacheStore cacheStore = getCacheStore.apply(c);

			if (cacheStore == null) {
				continue;
			}

			// A ChainingCacheStore indicates we (may) have multiple CacheStores at play
			if (cacheStore instanceof ChainingCacheStore) {
				final ChainingCacheStore chainingCacheStore =
						(ChainingCacheStore) cacheStore;
				// the stores are a map of the cache store and the configuration; i'm just throwing the configuration away..
				for (final CacheStore s : chainingCacheStore.getStores()
												  .keySet()) {
					try {
						if (s.containsKey(key + "-data-0")) {
							blobs.add(new LowLevelCacheEntry(ispnStore, s, key));
						}
					} catch (CacheLoaderException e) {
						logger.warn("Cache loader raised exception: {}", e);
					}
				}
			} else {
				// just a nice, simple infinispan cache.
				try {
					if (cacheStore.containsKey(key + "-data-0")) {
						blobs.add(new LowLevelCacheEntry(ispnStore, cacheStore, key));
					}
				} catch (CacheLoaderException e) {
					logger.warn("Cache loader raised exception: {}", e);
				}
			}
		}

		return blobs.build();
	}

    @PostConstruct
    public final void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    @PreDestroy
    public final void logoutSession() {
        readOnlySession.logout();
    }

    public void setRepository(final Repository repository) {
        if (readOnlySession != null) {
            logoutSession();
        }
        repo = repository;

        getSession();
    }

    public Collection<FixityResult> runFixityAndFixProblems(
            final Datastream datastream) throws RepositoryException {
        Set<FixityResult> fixityResults;
        Set<FixityResult> goodEntries;
        final URI digestUri = datastream.getContentDigest();
        final long size = datastream.getContentSize();
        MessageDigest digest;

        fixityCheckCounter.inc();

        try {
            digest = getInstance(datastream.getContentDigestType());
        } catch (final NoSuchAlgorithmException e) {
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
            } catch (final IOException e) {
                logger.warn("Exception repairing low-level cache entry: {}", e);
            }
        }

        return fixityResults;
    }

    public void setGetBinaryStore(final GetBinaryStore getBinaryStore) {
        this.getBinaryStore = getBinaryStore;
    }

    public void setGetBinaryKey(final GetBinaryKey getBinaryKey) {
        this.getBinaryKey = getBinaryKey;
    }

    public void setGetCacheStore(final GetCacheStore getCacheStore) {
        this.getCacheStore = getCacheStore;
    }

    public void setGetGoodFixityResults(final GetGoodFixityResults res) {
        this.getGoodFixityResults = res;
    }

}
