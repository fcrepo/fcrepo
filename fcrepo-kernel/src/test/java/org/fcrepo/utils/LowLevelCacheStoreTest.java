package org.fcrepo.utils;

import org.apache.commons.io.IOUtils;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class LowLevelCacheStoreTest {

    @Inject
    Repository repo;

    @Test
    public void testGetExternalIdentifier() throws Exception {
        BinaryStore store = ((JcrRepository)repo).getConfiguration()
                .getBinaryStorage().getBinaryStore();

        LowLevelCacheStore cs = new LowLevelCacheStore(store);
        assertEquals("org.modeshape.jcr.value.binary.TransientBinaryStore", cs.getExternalIdentifier().split("@")[0]);
    }

    @Test
    public void testGetExternalIdentifierWithInfinispan() throws Exception {

        EmbeddedCacheManager cm = new DefaultCacheManager("test_infinispan_configuration.xml");
        BinaryStore store = new InfinispanBinaryStore(cm, false, "FedoraRepository", "FedoraRepository");

        CacheStore ispn = cm.getCache("FedoraRepository").getAdvancedCache().getComponentRegistry()
                .getComponent(CacheLoaderManager.class)
                .getCacheStore();
        LowLevelCacheStore cs = new LowLevelCacheStore(store, ispn);
        assertEquals("org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore:org.infinispan.loaders.file.FileCacheStore:target/FedoraRepository/storage", cs.getExternalIdentifier());
    }

    @Test
    public void testModifyingCacheStores() throws Exception {

        EmbeddedCacheManager cm = new DefaultCacheManager("config/infinispan_configuration_chained.xml");
        BinaryStore store = new InfinispanBinaryStore(cm, false, "FedoraRepository", "FedoraRepository");

        CacheStore ispn = cm.getCache("FedoraRepository").getAdvancedCache().getComponentRegistry()
                .getComponent(CacheLoaderManager.class)
                .getCacheStore();

        assert(ispn instanceof ChainingCacheStore);

        final BinaryKey key = new BinaryKey("123");

        ChainingCacheStore chained_store = (ChainingCacheStore)ispn;

        final LinkedHashMap<CacheStore,CacheStoreConfiguration> stores = chained_store.getStores();


        LowLevelCacheStore cs = new LowLevelCacheStore(store, (CacheStore)stores.keySet().toArray()[0]);
        LowLevelCacheStore cs2 = new LowLevelCacheStore(store, (CacheStore)stores.keySet().toArray()[1]);

        cs.storeValue(key, new ByteArrayInputStream("123456".getBytes()));

        cs2.storeValue(key, new ByteArrayInputStream("asdfg".getBytes()));

        Thread.sleep(1000);

        String v1 = IOUtils.toString(cs.getInputStream(key));
        String v2 = IOUtils.toString(cs2.getInputStream(key));

        assertEquals("Found the wrong value in our cache store", "123456", v1);
        assertEquals("Found the wrong value in our cache store", "asdfg", v2);


    }
}
