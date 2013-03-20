package org.fcrepo.integration.utils.infinispan;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Test;

public class StoreChunkInputStreamIT {

    @Test
    public void tryRetrievingContentFromInfinispanIT() throws IOException {
        EmbeddedCacheManager cm = new DefaultCacheManager("test_infinispan_configuration.xml");

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

        InputStream is = new StoreChunkInputStream(cs, "key-data");

        String data = IOUtils.toString(is);

        assertEquals("012345", data);
    }
}
