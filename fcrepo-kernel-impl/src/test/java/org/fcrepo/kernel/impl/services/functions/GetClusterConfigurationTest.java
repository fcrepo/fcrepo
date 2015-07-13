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
import static org.mockito.Mockito.when;

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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

/**
 * @author acoburn
**/
@RunWith(MockitoJUnitRunner.class)
public class GetClusterConfigurationTest {

    private GetClusterConfiguration testObj;

    @Mock
    private JcrRepository mockRepo;

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

    @Mock
    private GetCacheManager mockGetCacheManager;

    @Mock
    private RepositoryConfiguration mockConfig;

    @Mock
    private RepositoryConfiguration.BinaryStorage mockStorage;

    @Before
    public void setUp() throws Exception {
        testObj = new GetClusterConfiguration();
        final Field cmField = GetClusterConfiguration.class.getDeclaredField("getCacheManager");
        cmField.setAccessible(true);
        cmField.set(testObj, mockGetCacheManager);

        when(mockRepo.getConfiguration()).thenReturn(mockConfig);
        when(mockConfig.getBinaryStorage()).thenReturn(mockStorage);
        when(mockCM.getCache()).thenReturn(mockCache);
        when(mockCache.getCacheConfiguration()).thenReturn(mockCC);
        when(mockCC.clustering()).thenReturn(mockClustering);
        when(mockCC.transaction()).thenReturn(mockTransactionConfiguration);
        when(mockCC.transaction().transactionMode()).thenReturn(null);
        when(mockClustering.cacheMode()).thenReturn(LOCAL);

        final InfinispanBinaryStore testStore = new InfinispanBinaryStore(mockCacheContainer, false, "x", "y");
        when(mockStorage.getBinaryStore()).thenReturn(testStore);
    }

    @Test
    public void testGood() {
        when(mockGetCacheManager.apply(mockRepo)).thenReturn(mockCM);
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
