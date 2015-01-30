/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.modeshape.jcr.value.binary.infinispan;

import static org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Test;
import org.modeshape.jcr.value.BinaryKey;


/**
 * Test of InfinispanUtils
 * @author escowles
 * @since 2014-08-07
**/
public class InfinispanUtilsTest {

    @Test
    public void testDataKeyFrom() {
        final DefaultCacheManager mgr = mock(DefaultCacheManager.class);
        final InfinispanBinaryStore store = new InfinispanBinaryStore(mgr, false, "foo", "bar" );
        final BinaryKey key = new BinaryKey("foo");
        assertEquals( "foo-data", InfinispanUtils.dataKeyFrom(store,key) );
    }

    @Test
    public void testGetMetadata() {
        final DefaultCacheManager mgr = mock(DefaultCacheManager.class);
        final InfinispanBinaryStore store = new InfinispanBinaryStore(mgr, false, "foo", "bar" );
        final BinaryKey key = new BinaryKey("foo");

        final Cache<Object, Object> cache = mock(Cache.class);
        final Metadata meta = new Metadata(0L, 1024L, 8, 128);
        when(mgr.getCache(anyString())).thenReturn(cache);
        doReturn(meta).when(cache).get(anyObject());
        final Configuration cacheConfig = mock(Configuration.class);
        when(cache.getCacheConfiguration()).thenReturn(cacheConfig);
        final TransactionConfiguration txConfig = mock(TransactionConfiguration.class);

        when(cacheConfig.transaction()).thenReturn(txConfig);
        when(txConfig.transactionMode()).thenReturn(NON_TRANSACTIONAL);
        final ClusteringConfiguration clConfig = mock(ClusteringConfiguration.class);
        when(cacheConfig.clustering()).thenReturn(clConfig);
        when(clConfig.cacheMode()).thenReturn(CacheMode.LOCAL);

        store.start();
        final ChunkBinaryMetadata chunk = InfinispanUtils.getMetadata( store, key );
        assertEquals( 1024L, chunk.getLength() );
        assertEquals( 128, chunk.getChunkSize() );
    }

}
