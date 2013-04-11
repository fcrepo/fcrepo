
package org.fcrepo.services.functions;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ComponentRegistry.class})
public class GetCacheStoreTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testApply() throws LoginException, RepositoryException {
        Cache<?, ?> mockCache = mock(Cache.class);
        @SuppressWarnings("rawtypes")
        AdvancedCache mockAC = mock(AdvancedCache.class);
        ComponentRegistry mockCR = mock(ComponentRegistry.class);
        CacheLoaderManager mockCLM = mock(CacheLoaderManager.class);
        CacheStore mockStore = mock(CacheStore.class);

        when(mockCLM.getCacheStore()).thenReturn(mockStore);
        when(mockCR.getComponent(CacheLoaderManager.class)).thenReturn(mockCLM);
        when(mockAC.getComponentRegistry()).thenReturn(mockCR);
        when(mockCache.getAdvancedCache()).thenReturn(mockAC);

        GetCacheStore testObj = new GetCacheStore();
        testObj.apply(mockCache);
        verify(mockCR).getComponent(any(Class.class));
    }

}
