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
package org.fcrepo.kernel.impl.services.functions;

import static org.infinispan.configuration.cache.CacheMode.LOCAL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.BinaryStorage;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

/**
 * @author ?
**/
public class GetClusterConfigurationTest {

    private GetClusterConfiguration testObj;

    @Mock
    private GetBinaryStore mockGetBinaryStore;

    @Mock
    private RepositoryConfiguration mockConfig;

    @Mock
    private JcrRepository mockRepo;

    @Mock
    private BinaryStorage mockStorage;

    @Mock
    private Cache<Object, Object> mockCache;

    @Mock
    private Configuration mockCC;

    @Mock
    private DefaultCacheManager mockCM;

    @Mock
    private ClusteringConfiguration mockClustering;

    @Mock
    private TransactionConfiguration mockTransactionConfiguration;

    @Mock
    private CacheContainer mockCacheContainer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new GetClusterConfiguration();
        final Field field =
                GetClusterConfiguration.class
                        .getDeclaredField("getBinaryStore");
        field.setAccessible(true);
        field.set(testObj, mockGetBinaryStore);

        when(mockRepo.getConfiguration()).thenReturn(mockConfig);
        when(mockConfig.getBinaryStorage()).thenReturn(mockStorage);
        when(mockCacheContainer.getCache(any(String.class))).thenReturn(mockCache);
        when(mockCache.getCacheConfiguration()).thenReturn(mockCC);
        when(mockCC.clustering()).thenReturn(mockClustering);
        when(mockCC.transaction()).thenReturn(mockTransactionConfiguration);
        when(mockCC.transaction().transactionMode()).thenReturn(null);
        when(mockClustering.cacheMode()).thenReturn(LOCAL);
        when(mockCM.getCache()).thenReturn(mockCache);

        final InfinispanBinaryStore mockStore = new InfinispanBinaryStore(mockCacheContainer, false, "x", "y");

        when(mockGetBinaryStore.apply(mockRepo)).thenReturn(mockStore);
        mockStore.start();
    }

    @Test
    public void testGood() {
        when(mockCache.getCacheManager()).thenReturn(mockCM);
        final Map<String, String> actual = testObj.apply(mockRepo);
        assertNotNull(actual);
        assertFalse(actual.isEmpty());
    }

    @Test
    public void testBad() {
        final Map<String, String> actual = testObj.apply(mockRepo);
        assertTrue(actual.isEmpty());
    }
}
