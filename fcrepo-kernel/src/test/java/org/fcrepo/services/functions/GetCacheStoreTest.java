package org.fcrepo.services.functions;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.components.ComponentMetadata;
import org.infinispan.factories.components.ComponentMetadataRepo;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.junit.Test;

public class GetCacheStoreTest {
	
	@Test
	public void testApply() throws LoginException, RepositoryException {
		Cache<?, ?> mockCache = mock(Cache.class);
		AdvancedCache mockAC = mock(AdvancedCache.class);
		GlobalComponentRegistry mockG = mock(GlobalComponentRegistry.class);
		ComponentMetadataRepo mockCMR = mock(ComponentMetadataRepo.class);
		when(mockG.getComponentMetadataRepo()).thenReturn(mockCMR);
		ComponentRegistry mockCR = null; //new ComponentRegistry("foo", null, mockAC, mockG, getClass().getClassLoader());
		ComponentMetadata mockMD = mock(ComponentMetadata.class);
		when(mockCMR.findComponentMetadata(any(Class.class))).thenReturn(mockMD);
		CacheLoaderManager mockCLM = mock(CacheLoaderManager.class);
		CacheStore mockStore = mock(CacheStore.class);
		when(mockCLM.getCacheStore()).thenReturn(mockStore);
		//when(mockCR.getComponent(CacheLoaderManager.class)).thenReturn(mockCLM);
		when(mockAC.getComponentRegistry()).thenReturn(mockCR);
		when(mockCache.getAdvancedCache()).thenReturn(mockAC);
		GetCacheStore testObj = new GetCacheStore();
		//testObj.apply(mockCache);
	}

}
