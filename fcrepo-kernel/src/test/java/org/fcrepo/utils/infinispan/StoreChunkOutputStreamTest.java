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
package org.fcrepo.utils.infinispan;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.fcrepo.utils.TestHelpers;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.junit.Before;
import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 9, 2013
 */
public class StoreChunkOutputStreamTest {

    private static final int DATA_SIZE = 1024;

    private StoreChunkOutputStream testObj;

    private CacheStore mockStore;

    private InternalCacheEntry mockEntry;

    private String mockKey = "key-to-a-mock-blob";


    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() {
        mockStore = mock(CacheStore.class);
        mockEntry = mock(InternalCacheEntry.class);
        testObj = new StoreChunkOutputStream(mockStore, mockKey);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testWritingMultipleChunks()
        throws IOException, CacheLoaderException {
        byte[] data = TestHelpers.randomData(DATA_SIZE);
        for (int i=0; i< 1025; i++) {
            testObj.write(data);
        }
        testObj.close();
        verify(mockStore, times(2)).store(any(InternalCacheEntry.class));
        assertEquals(2, testObj.getNumberChunks());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testWritingMultipleChunksOnVersionedKey()
        throws IOException, CacheLoaderException {
        byte[] data = TestHelpers.randomData(DATA_SIZE);
        when(mockStore.load(mockKey + "-0")).thenReturn(mockEntry);
        for (int i=0; i< 1025; i++) {
            testObj.write(data);
        }
        testObj.close();
        verify(mockStore).load(mockKey + "-0");
        verify(mockStore, times(2)).store(any(InternalCacheEntry.class));
        assertEquals(2, testObj.getNumberChunks());
    }
}
