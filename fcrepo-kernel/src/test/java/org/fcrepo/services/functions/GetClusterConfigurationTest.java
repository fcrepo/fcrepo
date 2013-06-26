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
package org.fcrepo.services.functions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.BinaryStorage;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 13, 2013
 */
public class GetClusterConfigurationTest {

    private GetClusterConfiguration testObj;
    
    private GetBinaryStore mockGetBinaryStore;

    /**
     * @throws Exception 
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws Exception {
        testObj = new GetClusterConfiguration();
        Field field = GetClusterConfiguration.class.getDeclaredField("getBinaryStore");
        field.setAccessible(true);
        mockGetBinaryStore = mock(GetBinaryStore.class);
        field.set(testObj, mockGetBinaryStore);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGood() throws Exception {
        final JcrRepository mockRepo = mock(JcrRepository.class);
        final RepositoryConfiguration mockConfig =
                mock(RepositoryConfiguration.class);
        when(mockRepo.getConfiguration()).thenReturn(mockConfig);
        final BinaryStorage mockStorage = mock(BinaryStorage.class);
        when(mockConfig.getBinaryStorage()).thenReturn(mockStorage);
        final InfinispanBinaryStore mockStore =
                mock(InfinispanBinaryStore.class);
        when(mockGetBinaryStore.apply(any(Repository.class))).thenReturn(mockStore);
        @SuppressWarnings("unchecked")
        final Cache<Object, Object> mockCache = mock(Cache.class);
        final List<Cache<?, ?>> mockCaches =
                Arrays.asList(new Cache<?, ?>[] {mockCache});
        when(mockStore.getCaches()).thenReturn(mockCaches);
        final DefaultCacheManager mockCM = mock(DefaultCacheManager.class);
        when(mockCM.getCache()).thenReturn(mockCache);
        final Configuration mockCC = mock(Configuration.class);
        when(mockCache.getCacheConfiguration()).thenReturn(mockCC);
        final ClusteringConfiguration mockClustering =
                mock(ClusteringConfiguration.class);
        when(mockCC.clustering()).thenReturn(mockClustering);
        when(mockClustering.cacheMode()).thenReturn(CacheMode.LOCAL);
        when(mockCache.getCacheManager()).thenReturn(mockCM);
        final Map<String, String> actual = testObj.apply(mockRepo);
        assertNotNull(actual);
        assertFalse(actual.isEmpty());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testBad() {
        final JcrRepository mockRepo = mock(JcrRepository.class);
        final Map<String, String> actual = testObj.apply(mockRepo);
        assertTrue(actual.isEmpty());
    }
}
