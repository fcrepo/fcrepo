package org.fcrepo.utils.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStoreException;

public class ChainingCacheStoreEntry extends LowLevelCacheEntry {

    private final ChainingCacheStore store;
    private final String cacheName;

    public ChainingCacheStoreEntry(final ChainingCacheStore store,
            final String cacheName,
            final BinaryKey key) {
        super(key);
        this.store = store;
        this.cacheName = cacheName;
    }

    @Override
    public InputStream getInputStream() throws BinaryStoreException {
        throw new UnsupportedOperationException("getInputStream must be called on chained entries");
    }

    @Override
    public void storeValue(InputStream stream) throws BinaryStoreException,
            IOException {
        throw new UnsupportedOperationException("storeValue must be called on chained entries");
    }

    @Override
    public String getExternalIdentifier() {
        return null;
    }

    public Set<LowLevelCacheEntry> chainedEntries() {
        Set<CacheStore> stores = this.store.getStores().keySet();
        HashSet<LowLevelCacheEntry> result = new HashSet<LowLevelCacheEntry>(stores.size());
        for (CacheStore store: stores) {
            String cacheName = null;
            CacheStoreConfiguration config = this.store.getStores().get(store);
            if (config instanceof FileCacheStoreConfiguration) {
                cacheName = ((FileCacheStoreConfiguration)config).location();
            }
            if (config instanceof AbstractStoreConfiguration) {
                Object name = ((AbstractStoreConfiguration)config).properties().get("id");
                if (name != null) cacheName = name.toString();
            }
            if (cacheName == null) cacheName = this.cacheName;

            result.add(new CacheStoreEntry(store, cacheName, key));
        }
        return result;
    }
}
