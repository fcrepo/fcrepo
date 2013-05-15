package org.fcrepo.services.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

public class GetClusterConfigurationTest {

	private GetClusterConfiguration testObj;
	
	@Before
	public void setUp(){
		testObj = new GetClusterConfiguration();
	}
	
	@Test
	public void testGood() throws Exception {
		JcrRepository mockRepo = mock(JcrRepository.class);
		RepositoryConfiguration mockConfig = mock(RepositoryConfiguration.class);
		when(mockRepo.getConfiguration()).thenReturn(mockConfig);
		BinaryStorage mockStorage = mock(BinaryStorage.class);
		when(mockConfig.getBinaryStorage()).thenReturn(mockStorage);
		InfinispanBinaryStore mockStore = mock(InfinispanBinaryStore.class);
		when(mockStorage.getBinaryStore()).thenReturn(mockStore);
		Cache<Object,Object> mockCache = mock(Cache.class);
		List<Cache<?,?>> mockCaches = Arrays.asList(new Cache<?,?>[]{mockCache});
		when(mockStore.getCaches()).thenReturn(mockCaches);
		DefaultCacheManager mockCM = mock(DefaultCacheManager.class);
		when(mockCM.getCache()).thenReturn(mockCache);
		Configuration mockCC = mock(Configuration.class);
		when(mockCache.getCacheConfiguration()).thenReturn(mockCC);
		ClusteringConfiguration mockClustering = mock(ClusteringConfiguration.class);
		when(mockCC.clustering()).thenReturn(mockClustering);
		when(mockClustering.cacheMode()).thenReturn(CacheMode.LOCAL);
		when(mockCache.getCacheManager()).thenReturn(mockCM);
		Map<String, String> actual = testObj.apply(mockRepo);
		assertNotNull(actual);
	}
	
	@Test
	public void testBad() {
		JcrRepository mockRepo = mock(JcrRepository.class);
		Map<String, String> actual = testObj.apply(mockRepo);
		assertEquals(null, actual);
	}
}
