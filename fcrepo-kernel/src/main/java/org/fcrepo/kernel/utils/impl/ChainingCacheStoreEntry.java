/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.utils.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * A LowLevelCacheEntry that is aware of Infinispan ChainingCacheStores
 *
 * @author awoods
 */
public class ChainingCacheStoreEntry extends LowLevelCacheEntry {

    private final ChainingCacheStore store;
    private final String cacheName;

    /**
     * @param store the ChainingCacheStore
     * @param cacheName the cache name to use by default
     * @param key the binary key we're interested in
     */
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
    public void storeValue(final InputStream stream) throws BinaryStoreException,
            IOException {
        throw new UnsupportedOperationException("storeValue must be called on chained entries");
    }

    @Override
    public String getExternalIdentifier() {
        return null;
    }

    /**
     * Get the set of LowLevelCacheEntries for each of the Chained cache stores
     */
    public Set<LowLevelCacheEntry> chainedEntries() {
        final Set<CacheStore> stores = this.store.getStores().keySet();
        final Set<LowLevelCacheEntry> result = new HashSet<>(stores.size());

        for (final CacheStore store: stores) {
            final CacheStoreConfiguration config = this.store.getStores().get(store);

            String cacheStoreName = null;

            if (config instanceof FileCacheStoreConfiguration) {
                cacheStoreName = ((FileCacheStoreConfiguration)config).location();
            }

            if (config instanceof AbstractStoreConfiguration && cacheStoreName == null) {
                final Object name = ((AbstractStoreConfiguration)config).properties().get("id");
                if (name != null) {
                    cacheStoreName = name.toString();
                }
            }

            if (cacheStoreName == null) {
                cacheStoreName = this.cacheName;
            }

            result.add(new CacheStoreEntry(store, cacheStoreName, key));
        }
        return result;
    }
}
