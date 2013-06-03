/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.integration.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;

import javax.inject.Inject;
import javax.jcr.Repository;

import org.apache.commons.io.IOUtils;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date Mar 20, 2013
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class LowLevelCacheEntryIT {

    @Inject
    Repository repo;

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetExternalIdentifier() throws Exception {
        final BinaryStore store =
                ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                        .getBinaryStore();

        final LowLevelCacheEntry cs =
                new LowLevelCacheEntry(store, new BinaryKey("asd"));
        assertEquals("/org.modeshape.jcr.value.binary.TransientBinaryStore", cs
                .getExternalIdentifier().split(":")[0]);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testEquals() throws Exception {

        final BinaryStore store =
                ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                        .getBinaryStore();

        final LowLevelCacheEntry cs1 =
                new LowLevelCacheEntry(store, new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new LowLevelCacheEntry(store, new BinaryKey("asd"));

        assertEquals(cs1, cs2);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testHashCode() throws Exception {

        final BinaryStore store =
                ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                        .getBinaryStore();

        final LowLevelCacheEntry cs1 =
                new LowLevelCacheEntry(store, new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new LowLevelCacheEntry(store, new BinaryKey("asd"));

        assertEquals(cs1.hashCode(), cs2.hashCode());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testEqualsIspn() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager("test_infinispan_configuration.xml");
        final BinaryStore store =
                new InfinispanBinaryStore(cm, false, "FedoraRepository",
                        "FedoraRepository");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();

        final LowLevelCacheEntry cs1 =
                new LowLevelCacheEntry(store, ispn, new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new LowLevelCacheEntry(store, ispn, new BinaryKey("asd"));

        assertEquals(cs1, cs2);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testHashCodeIspn() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager("test_infinispan_configuration.xml");
        final BinaryStore store =
                new InfinispanBinaryStore(cm, false, "FedoraRepository",
                        "FedoraRepository");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();

        final LowLevelCacheEntry cs1 =
                new LowLevelCacheEntry(store, ispn, new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new LowLevelCacheEntry(store, ispn, new BinaryKey("asd"));

        assertEquals(cs1.hashCode(), cs2.hashCode());

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetExternalIdentifierWithInfinispan() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager("test_infinispan_configuration.xml");
        final BinaryStore store =
                new InfinispanBinaryStore(cm, false, "FedoraRepository",
                        "FedoraRepository");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();
        final LowLevelCacheEntry cs =
                new LowLevelCacheEntry(store, ispn, new BinaryKey("asd"));
        assertEquals(
                "/org.modeshape.jcr.value.binary.infinispan." +
                "InfinispanBinaryStore:FedoraRepository:org.infinispan." +
                "loaders.file.FileCacheStore:target/FedoraRepository/storage",
                cs.getExternalIdentifier());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testModifyingCacheStores() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager(
                        "config/infinispan_configuration_chained.xml");
        final BinaryStore store =
                new InfinispanBinaryStore(cm, false, "FedoraRepository",
                        "FedoraRepository");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();

        assert ispn instanceof ChainingCacheStore;

        final BinaryKey key = new BinaryKey("123");

        final ChainingCacheStore chained_store = (ChainingCacheStore) ispn;

        final LinkedHashMap<CacheStore, CacheStoreConfiguration> stores =
                chained_store.getStores();

        final LowLevelCacheEntry cs =
                new LowLevelCacheEntry(store, (CacheStore) stores.keySet()
                        .toArray()[0], key);
        final LowLevelCacheEntry cs2 =
                new LowLevelCacheEntry(store, (CacheStore) stores.keySet()
                        .toArray()[1], key);

        cs.storeValue(new ByteArrayInputStream("123456".getBytes()));

        cs2.storeValue(new ByteArrayInputStream("asdfg".getBytes()));

        Thread.sleep(1000);

        final String v1 = IOUtils.toString(cs.getInputStream());
        final String v2 = IOUtils.toString(cs2.getInputStream());

        assertEquals("Found the wrong value in our cache store", "123456", v1);
        assertEquals("Found the wrong value in our cache store", "asdfg", v2);

    }
}
