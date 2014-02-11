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
package org.fcrepo.kernel.utils.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Set;

import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.CacheStoreConfiguration;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.infinispan.util.TypedProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStoreException;

public class ChainingCacheStoreEntryTest {

    private static final String DUMMY_CACHE_NAME = "dummy-cache-name";

    @Mock
    private ChainingCacheStore mockStore;

    @Mock
    private BinaryKey mockKey;

    private ChainingCacheStoreEntry testObj;

    @Before
    public void setUp(){
        initMocks(this);
        testObj = new ChainingCacheStoreEntry(mockStore, DUMMY_CACHE_NAME, mockKey);
    }

    @Test
    public void testNullAndUnimplementedMethodImpls()
        throws BinaryStoreException, IOException{

        try {
            testObj.getInputStream();
            fail("Unexpected completion of " +
                 "ChainingCacheStoreEntry#getInputStream");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
        try {
            final InputStream mockStream = mock(InputStream.class);
            testObj.storeValue(mockStream);
            fail("Unexpected completion of " +
                 "ChainingCacheStoreEntry#storeValue");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
        assertNull("Unexpected non-null return from ChainingCacheStoreEntry#" +
                   "getExternalIdentifier",
                testObj.getExternalIdentifier());
    }

    @Test
    public void testChainedEntries() {
        final CacheStore mockChainedFileStore = mock(CacheStore.class);
        final CacheStore mockChainedAbstractStore = mock(CacheStore.class);
        final CacheStore mockUnknownStore = mock(CacheStore.class);
        final FileCacheStoreConfiguration mockFileConfig =
            mock(FileCacheStoreConfiguration.class);
        when(mockFileConfig.location()).thenReturn("/foo/bar");
        final AbstractStoreConfiguration mockAbstractConfig =
                mock(AbstractStoreConfiguration.class);
        final CacheStoreConfiguration mockUnknownConfig =
                mock(CacheStoreConfiguration.class);
        final TypedProperties mockProps = mock(TypedProperties.class);
        final TypedProperties mockNoProps = mock(TypedProperties.class);
        when(mockProps.get("id")).thenReturn("dummy-abstract");
        when(mockAbstractConfig.properties()).thenReturn(mockProps);
        when(mockFileConfig.properties()).thenReturn(mockNoProps);
        when(mockUnknownConfig.properties()).thenReturn(mockNoProps);
        final LinkedHashMap<CacheStore, CacheStoreConfiguration> mockStores =
                new LinkedHashMap<>(3);
        mockStores.put(mockChainedFileStore, mockFileConfig);
        mockStores.put(mockChainedAbstractStore, mockAbstractConfig);
        mockStores.put(mockUnknownStore, mockUnknownConfig);
        when(mockStore.getStores()).thenReturn(mockStores);
        final Set<LowLevelCacheEntry> actual = testObj.chainedEntries();
        assertEquals(3, actual.size());
        verify(mockProps).get("id");
        verify(mockFileConfig).location();
        verify(mockNoProps, times(0)).get("id");
    }
}
