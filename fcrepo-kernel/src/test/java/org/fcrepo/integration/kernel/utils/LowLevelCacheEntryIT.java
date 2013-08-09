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
package org.fcrepo.integration.kernel.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;

import javax.inject.Inject;
import javax.jcr.Repository;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.fcrepo.kernel.utils.impl.CacheStoreEntry;
import org.fcrepo.kernel.utils.impl.LocalBinaryStoreEntry;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class LowLevelCacheEntryIT {

    @Inject
    Repository repo;

    @Test
    public void testGetExternalIdentifier() throws Exception {
        final BinaryStore store =
                ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                        .getBinaryStore();

        final LowLevelCacheEntry cs =
                new LocalBinaryStoreEntry(store, new BinaryKey("asd"));
        assertEquals("/org.modeshape.jcr.value.binary.TransientBinaryStore", cs
                .getExternalIdentifier().split(":")[0]);
    }

    @Test
    public void testEquals() throws Exception {

        final BinaryStore store =
                ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                        .getBinaryStore();

        final LowLevelCacheEntry cs1 =
                new LocalBinaryStoreEntry(store, new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new LocalBinaryStoreEntry(store, new BinaryKey("asd"));

        assertEquals(cs1, cs2);
    }

    @Test
    public void testHashCode() throws Exception {

        final BinaryStore store =
                ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                        .getBinaryStore();

        final LowLevelCacheEntry cs1 =
                new LocalBinaryStoreEntry(store, new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new LocalBinaryStoreEntry(store, new BinaryKey("asd"));

        assertEquals(cs1.hashCode(), cs2.hashCode());
    }

    @Test
    public void testEqualsIspn() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager("config/infinispan/basic/infinispan.xml");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();

        final LowLevelCacheEntry cs1 =
                new CacheStoreEntry(ispn, "FedoraRepository", new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new CacheStoreEntry(ispn, "FedoraRepository", new BinaryKey("asd"));

        assertEquals(cs1, cs2);

    }

    @Test
    public void testHashCodeIspn() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager("config/infinispan/basic/infinispan.xml");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();

        final LowLevelCacheEntry cs1 =
                new CacheStoreEntry(ispn, "FedoraRepository", new BinaryKey("asd"));
        final LowLevelCacheEntry cs2 =
                new CacheStoreEntry(ispn, "FedoraRepository", new BinaryKey("asd"));

        assertEquals(cs1.hashCode(), cs2.hashCode());

    }

    @Test
    public void testGetExternalIdentifierWithInfinispan() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager("config/infinispan/basic/infinispan.xml");

        final CacheStore ispn =
                cm.getCache("FedoraRepository").getAdvancedCache()
                        .getComponentRegistry().getComponent(
                                CacheLoaderManager.class).getCacheStore();
        final LowLevelCacheEntry cs =
                new CacheStoreEntry(ispn, "FedoraRepository", new BinaryKey("asd"));
        assertEquals(
                "/org.infinispan.loaders.file." +
                "FileCacheStore:FedoraRepository:org.infinispan." +
                "loaders.file.FileCacheStore:target/FedoraRepository/storage",
                cs.getExternalIdentifier());
    }

    @Test
    public void testModifyingCacheStores() throws Exception {

        final EmbeddedCacheManager cm =
                new DefaultCacheManager(
                        "config/infinispan/chained/infinispan.xml");

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
                new CacheStoreEntry((CacheStore) stores.keySet()
                        .toArray()[0], "FedoraRepository", key);
        final LowLevelCacheEntry cs2 =
                new CacheStoreEntry((CacheStore) stores.keySet()
                        .toArray()[1], "FedoraRepository", key);

        cs.storeValue(new ByteArrayInputStream("123456".getBytes()));

        cs2.storeValue(new ByteArrayInputStream("asdfg".getBytes()));

        Thread.sleep(1000);

        final String v1 = IOUtils.toString(cs.getInputStream());
        final String v2 = IOUtils.toString(cs2.getInputStream());

        assertEquals("Found the wrong value in our cache store", "123456", v1);
        assertEquals("Found the wrong value in our cache store", "asdfg", v2);

    }
}
