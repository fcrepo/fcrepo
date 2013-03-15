package org.fcrepo.utils.infinispan;

import org.apache.commons.io.IOUtils;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Test;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class StoreChunkInputStreamTest {

    @Test
    public void tryRetrievingContentFromInfinispanTest() throws IOException {
        EmbeddedCacheManager cm = new DefaultCacheManager("test_infinispan_configuration.xml");
        BinaryStore store = new InfinispanBinaryStore(cm, false, "FedoraRepository", "FedoraRepository");

        Cache<String, byte[]> ispn = cm.getCache("FedoraRepository");


        CacheStore cs = ispn.getAdvancedCache().getComponentRegistry()
                .getComponent(CacheLoaderManager.class)
                .getCacheStore();

        ispn.put("key-data-0", "0".getBytes());
        ispn.put("key-data-1", "1".getBytes());
        ispn.put("key-data-2", "2".getBytes());
        ispn.put("key-data-3", "3".getBytes());
        ispn.put("key-data-4", "4".getBytes());
        ispn.put("key-data-5", "5".getBytes());


        Set s = ispn.keySet();
        InputStream is = new StoreChunkInputStream(cs, "key-data");

        String data = IOUtils.toString(is);

        assertEquals("012345", data);
    }
}
