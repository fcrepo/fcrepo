package org.fcrepo.utils.impl;

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.poi.util.IOUtils;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.fcrepo.utils.infinispan.StoreChunkOutputStream;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.slf4j.Logger;


public class CacheStoreEntry extends LowLevelCacheEntry {

    private static final Logger LOGGER = getLogger(CacheStoreEntry.class);

    private final CacheStore store;
    private final String cacheName;

    /**
     * @todo Document.
     */
    public CacheStoreEntry(final CacheStore store,
            final String cacheName,
            final BinaryKey key) {
        super(key);
        this.store = store;
        this.cacheName = cacheName;
    }

    /**
     * @todo Add Documentation.
     */
    public CacheStore getLowLevelStore() {
        return store;
    }

    /**
     * Get a raw input stream from the underlying store
     * @return the content for this entry
     * @throws BinaryStoreException
     */
    public InputStream getInputStream() throws BinaryStoreException {
        return new StoreChunkInputStream(store, key.toString() +
                                             DATA_SUFFIX);
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
        LOGGER.info("Doing a low-level write to store {} for key {}",
                    getExternalIdentifier(), key);

        final OutputStream outputStream =
                new StoreChunkOutputStream(store, key.toString() +
                                           DATA_SUFFIX);
        IOUtils.copy(stream, outputStream);
        outputStream.close();
    }

    /**
     * Generate a human-readable identifier for the location of this entry
     *
     * @return
     */
    public String getExternalIdentifier() {

        final CacheStoreConfig config = store.getCacheStoreConfig();

        String ispnExternalId = null;

        if (config instanceof AbstractCacheStoreConfig) {
            ispnExternalId = getStoreIdentifier((AbstractCacheStoreConfig)config);
            if (ispnExternalId != null) {
                return ispnExternalId;
            }
        }

        if (config instanceof FileCacheStoreConfig) {
            ispnExternalId = getStoreIdentifier((FileCacheStoreConfig) config);
        }

        // ChainingCacheStore's have a null config
        if (config == null) {
            return getExternalId() + "/" + store.getClass().getName() + ":" +
                    cacheName + ":" +
                    AbstractCacheStoreConfig.class.getPackage() +
                    "ChainingCacheStoreConfig:" + ispnExternalId;
        }

        if (ispnExternalId == null) {
            ispnExternalId = config.toString();
        }

        return getExternalId() + "/" + store.getClass().getName() + ":" +
            cacheName + ":" +
            config.getCacheLoaderClassName() +
            ":" + ispnExternalId;
    }

    private static String getStoreIdentifier(AbstractCacheStoreConfig config) {
        final Properties properties =
                config.getProperties();
        if (properties.containsKey("id")) {
            return properties.getProperty("id");
        }
        return null;
    }

    private static String getStoreIdentifier(FileCacheStoreConfig config) {
        return config.getLocation();
    }


    /**
     * Two LowLevelCacheEntries are the same if they have the same key,
     * come from the same CacheStore, with the same cache name,
     * and have the same underlying store configuration
     * @param other
     * @return
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof CacheStoreEntry) {
            final CacheStoreEntry that = (CacheStoreEntry) other;

            return key.equals(that.getKey()) &&
                           cacheName.equals(that.cacheName) &&
                           ((store == null && that.store == null) ||
                                    (store != null && store.equals(that.store)));
        } else {
            return false;
        }
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public int hashCode() {
        return hash(store, key);
    }

}
