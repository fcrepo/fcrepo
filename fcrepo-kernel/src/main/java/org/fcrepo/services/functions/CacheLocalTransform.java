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

package org.fcrepo.services.functions;

import java.io.Serializable;
import java.util.Set;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.fcrepo.utils.impl.CacheStoreEntry;
import org.fcrepo.utils.impl.ChainingCacheStoreEntry;
import org.infinispan.Cache;
import org.infinispan.CacheImpl;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.modeshape.jcr.value.BinaryKey;

import com.google.common.base.Function;

/**
 * Apply a Function on a BinaryKey in a LOCAL CacheStore
 * 
 * @param <K> Cache key class
 * @param <V> Cache value class
 * @param <T> Output of the transform
 */
public class CacheLocalTransform<K, V, T> implements
        DistributedCallable<K, V, T>, Serializable {

    /**
     * Because this class will be communicated between cache nodes, it must be
     * serializable
     */
    private static final long serialVersionUID = -7014104738830230123L;

    private static GetCacheStore transform = new GetCacheStore();

    private BinaryKey key;

    private Function<LowLevelCacheEntry, T> entryTransform;

    private CacheStore store;

    private String cacheName = "";

    /**
     * @param key the BinaryKey to transform
     * @param entryTransform a Function from LowLevelCacheEntries to T
     */
    public CacheLocalTransform(final BinaryKey key,
            final Function<LowLevelCacheEntry, T> entryTransform) {

        this.key = key;
        this.entryTransform = entryTransform;
    }

    @Override
    public T call() {
        final LowLevelCacheEntry entry =
                (store instanceof ChainingCacheStore)
                        ? new ChainingCacheStoreEntry(
                                (ChainingCacheStore) store, cacheName, key)
                        : new CacheStoreEntry(store, cacheName, key);
        return this.entryTransform.apply(entry);
    }

    @Override
    public void setEnvironment(final Cache<K, V> cache, final Set<K> keys) {
        this.store = transform.apply(cache);
        this.cacheName = ((CacheImpl<K, V>) cache).getName();
    }
}
