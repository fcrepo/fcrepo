package org.fcrepo.utils;

import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public class LowLevelCacheStore {

    private static final String DATA_SUFFIX = "-data";
    private final BinaryStore store;
    private final CacheStore low_level_store;

    public LowLevelCacheStore(BinaryStore store, CacheStore low_level_store) {
        this.store = store;
        this.low_level_store = low_level_store;
    }

    public LowLevelCacheStore(BinaryStore store) {
        this.store = store;
        this.low_level_store = null;
    }

    public boolean equals(final Object other) {
        if(other instanceof LowLevelCacheStore) {
            final LowLevelCacheStore that = (LowLevelCacheStore)other;

            return this.store.equals(that.store) && this.low_level_store.equals(that.low_level_store);
        } else {
            return false;
        }
    }


    public InputStream getInputStream(BinaryKey key) throws BinaryStoreException {
        if(this.store instanceof InfinispanBinaryStore) {
            return new StoreChunkInputStream(low_level_store, key.toString() + DATA_SUFFIX);
        } else {
            return this.store.getInputStream(key);
        }
    }

    public String getExternalIdentifier() {

        if(this.store instanceof InfinispanBinaryStore) {

            CacheStoreConfig config = this.low_level_store.getCacheStoreConfig();

            String externalId = null;
            if(config instanceof AbstractCacheStoreConfig) {
                final Properties properties = ((AbstractCacheStoreConfig) config).getProperties();
                if(properties.containsKey("id")) {
                    return properties.getProperty("id");
                }

            }

            if(externalId == null && config instanceof FileCacheStoreConfig) {
                externalId = ((FileCacheStoreConfig)config).getLocation();
            }

            if(externalId == null) {
                externalId = config.toString();
            }

            return this.store.getClass().getName()
                    + ":" + this.low_level_store.getCacheStoreConfig().getCacheLoaderClassName()
                    + ":" + externalId;

        } else {
            return this.store.toString();
        }
    }
}
