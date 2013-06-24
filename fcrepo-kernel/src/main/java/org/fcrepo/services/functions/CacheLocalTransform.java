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


public class CacheLocalTransform<K, V, T> 
implements DistributedCallable<K, V, T>, Serializable {

    /**
     * Because this class will be communicated between cache nodes,
     * it must be serializable
     */
    private static final long serialVersionUID = -7014104738830230123L;

    private static GetCacheStore TRANSFORM = new GetCacheStore();

    private BinaryKey key;
    private Function<LowLevelCacheEntry, T> entryTransform;
    private CacheStore store;
    private String cacheName = "";

    public CacheLocalTransform(final BinaryKey key,
            final Function<LowLevelCacheEntry, T> entryTransform) {

        this.key = key;
        this.entryTransform = entryTransform;
    }

    @Override
    public T call() throws Exception {
        LowLevelCacheEntry entry =
                (store instanceof ChainingCacheStore) ?
                        new ChainingCacheStoreEntry((ChainingCacheStore)store, cacheName, key) :
                            new CacheStoreEntry(store, cacheName, key);
        return this.entryTransform.apply(entry);
    }

    @Override
    public void setEnvironment(Cache<K, V> cache, Set<K> keys) {
        this.store = TRANSFORM.apply(cache);
        this.cacheName = ((CacheImpl<K, V>) cache).getName();
    }
}
