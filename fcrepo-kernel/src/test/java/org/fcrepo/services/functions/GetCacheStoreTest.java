/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date Apr 3, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ComponentRegistry.class})
public class GetCacheStoreTest {

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testApply() throws LoginException, RepositoryException {
        final Cache<?, ?> mockCache = mock(Cache.class);
        @SuppressWarnings("rawtypes")
        final AdvancedCache mockAC = mock(AdvancedCache.class);
        final ComponentRegistry mockCR = mock(ComponentRegistry.class);
        final CacheLoaderManager mockCLM = mock(CacheLoaderManager.class);
        final CacheStore mockStore = mock(CacheStore.class);

        when(mockCLM.getCacheStore()).thenReturn(mockStore);
        when(mockCR.getComponent(CacheLoaderManager.class)).thenReturn(mockCLM);
        when(mockAC.getComponentRegistry()).thenReturn(mockCR);
        when(mockCache.getAdvancedCache()).thenReturn(mockAC);

        final GetCacheStore testObj = new GetCacheStore();
        testObj.apply(mockCache);
        verify(mockCR).getComponent(any(Class.class));
    }

}
