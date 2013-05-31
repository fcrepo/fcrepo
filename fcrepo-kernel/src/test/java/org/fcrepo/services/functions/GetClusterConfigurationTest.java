/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Before;
import org.junit.Test;
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

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() {
        testObj = new GetClusterConfiguration();
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
        when(mockStorage.getBinaryStore()).thenReturn(mockStore);
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
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testBad() {
        final JcrRepository mockRepo = mock(JcrRepository.class);
        final Map<String, String> actual = testObj.apply(mockRepo);
        assertTrue("", actual.isEmpty());
    }
}
