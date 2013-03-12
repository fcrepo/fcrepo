package org.fcrepo.utils;

import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.infinispan.loaders.CacheStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

import java.io.InputStream;

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


    public InputStream getInputStream(BinaryKey key) throws BinaryStoreException {
        if(this.store instanceof InfinispanBinaryStore) {
            return new StoreChunkInputStream(low_level_store, key.toString() + DATA_SUFFIX);
        } else {
            return this.store.getInputStream(key);
        }
    }
}
