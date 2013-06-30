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

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ComponentRegistry.class})
public class GetCacheStoreTest {

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
