
package org.fcrepo.utils;

import static com.google.common.base.Throwables.propagate;
import static java.util.Objects.hash;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.utils.FixityResult.FixityState.SUCCESS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Properties;

import org.apache.poi.util.IOUtils;
import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.fcrepo.utils.infinispan.StoreChunkOutputStream;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

/**
 * Manage low-level I/O from a cache store (or, for an ISPN store, a cache loader) in order
 * to report on e.g. fixity.
 */
public class LowLevelCacheEntry {

	private static final Logger logger = getLogger(LowLevelCacheEntry.class);

	private static final String DATA_SUFFIX = "-data";

	private final BinaryStore store;

	private final CacheStore cacheStore;

	private String externalId;

	private final BinaryKey key;

	public LowLevelCacheEntry(final BinaryStore store,
							  final CacheStore lowLevelStore, final BinaryKey key) {
		this.store = store;
		cacheStore = lowLevelStore;
		this.key = key;
		this.externalId = "";
	}

	public LowLevelCacheEntry(final BinaryStore store, final BinaryKey key) {
		this.store = store;
		cacheStore = null;
		this.key = key;
		this.externalId = "";
	}

	/**
	 * Two LowLevelCacheEntries are the same if they have the same key, come from the same BinaryStore,
	 * and have the same underlying store configuration
	 * @param other
	 * @return
	 */
	@Override
	public boolean equals(final Object other) {
		if (other instanceof LowLevelCacheEntry) {
			final LowLevelCacheEntry that = (LowLevelCacheEntry) other;

			return key.equals(that.key) &&
						   store.equals(that.store) &&
						   (cacheStore == null && that.cacheStore == null || cacheStore != null &&
																					 cacheStore.equals(that.cacheStore));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return hash(store, cacheStore, key);
	}

	/**
	 * Get a raw input stream from the underlying store
	 * @return the content for this entry
	 * @throws BinaryStoreException
	 */
	public InputStream getInputStream() throws BinaryStoreException {
		if (store instanceof InfinispanBinaryStore) {
			return new StoreChunkInputStream(cacheStore, key.toString() +
																 DATA_SUFFIX);
		} else {
			return store.getInputStream(key);
		}
	}

	/**
	 * Send a raw input stream to the underlying store for this entry; used for
	 * fixing e.g. fixity failures.
	 *
	 * @param stream binary content to REPLACE the content in the store
	 * @throws BinaryStoreException
	 * @throws IOException
	 */
	public void storeValue(final InputStream stream)
			throws BinaryStoreException, IOException {
		// TODO: this is probably an auditable action.
		logger.info("Doing a low-level write to store {} for key {}", getExternalIdentifier(), key);

		if (store instanceof InfinispanBinaryStore) {
			final OutputStream outputStream =
					new StoreChunkOutputStream(cacheStore, key.toString() +
																   DATA_SUFFIX);
			IOUtils.copy(stream, outputStream);
			outputStream.close();
		} else {
			// the BinaryStore will calculate a new key for us.
			store.storeValue(stream);
		}
	}

	/**
	 * Generate a human-readable identifier for the location of this entry
	 *
	 * @return
	 */
	public String getExternalIdentifier() {

		// TODO : I wonder if this could/should be a JSON blob or something machine parsable as well?

		if (store instanceof InfinispanBinaryStore) {

			final InfinispanBinaryStore ispnStore = (InfinispanBinaryStore)store;

			final CacheStoreConfig config = cacheStore.getCacheStoreConfig();

			String ispnExternalId = null;
			if (config instanceof AbstractCacheStoreConfig) {
				final Properties properties =
						((AbstractCacheStoreConfig) config).getProperties();
				if (properties.containsKey("id")) {
					return properties.getProperty("id");
				}

			}

			if (config instanceof FileCacheStoreConfig) {
				ispnExternalId = ((FileCacheStoreConfig) config).getLocation();
			}

			if (ispnExternalId == null) {
				ispnExternalId = config.toString();
			}

			String blobCacheName = "";
			try {
				Field blobCacheNameField = ispnStore.getClass().getDeclaredField("blobCacheName");
				blobCacheNameField.setAccessible(true);
				blobCacheName = (String)blobCacheNameField.get(ispnStore);
			} catch (IllegalAccessException e) {
				logger.warn("Got exception doing some questionable reflection to get the blob cache name", e);
			} catch (NoSuchFieldException e) {
				logger.warn("Got exception doing some questionable reflection  to get the blob cache name", e);
			}


			return getExternalId() + "/" + store.getClass().getName() + ":" + blobCacheName + ":" +
						   config.getCacheLoaderClassName() +
						   ":" + ispnExternalId;
		} else if ( store instanceof FileSystemBinaryStore) {
			final FileSystemBinaryStore fsStore = (FileSystemBinaryStore)store;
			return getExternalId() + "/" + store.getClass().getName() + ":" + ((FileSystemBinaryStore) store).getDirectory().toPath();
		} else {
			return getExternalId() + "/" + store.toString();
		}
	}

	/**
	 * Check the entry's InputStream against the checksum and size.
	 *
	 * @param checksum
	 * @param size
	 * @param digest
	 * @return
	 * @throws BinaryStoreException
	 */
	public FixityResult checkFixity(final URI checksum, final long size,
									final MessageDigest digest) throws BinaryStoreException {
		final FixityInputStream ds;

		try {
			ds = new FixityInputStream(getInputStream(), (MessageDigest) digest.clone());
		} catch (CloneNotSupportedException e) {
			logger.warn("Could not clone MessageDigest: {}", e);
			throw propagate(e);
		}

		try {
			final FixityResult result = new FixityResult(this);

			while (ds.read() != -1) {
				// noop; we're just reading the stream for the checksum and size
			}

			result.computedChecksum = ContentDigest.asURI(digest.getAlgorithm(), ds
																	   .getMessageDigest().digest());
			result.computedSize = ds.getByteCount();
			result.dsChecksum = checksum;
			result.dsSize = size;

			if (!result.computedChecksum.equals(result.dsChecksum)) {
				result.status.add(BAD_CHECKSUM);
			}

			if (result.dsSize != result.computedSize) {
				result.status.add(BAD_SIZE);
			}

			if (result.status.isEmpty()) {
				result.status.add(SUCCESS);
			}

			logger.debug("Got " + result.toString());

			return result;
		} catch (final IOException e) {
			throw propagate(e);
		} finally {
			try {
				ds.close();
			} catch (IOException e) {
				logger.debug("Got error closing input stream: {}", e);
			}
		}

	}

	/**
	 * A meaningful identifier at some higher level that we should
	 * dutifully pass through.
	 *
	 * @param externalId some identifier for the cache store
	 */
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getExternalId() {
		return externalId;
	}
}
