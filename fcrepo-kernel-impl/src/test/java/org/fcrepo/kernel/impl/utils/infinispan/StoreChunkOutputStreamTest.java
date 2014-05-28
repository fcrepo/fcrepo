/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.utils.infinispan;

import static org.fcrepo.kernel.utils.TestHelpers.randomData;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>StoreChunkOutputStreamTest class.</p>
 *
 * @author awoods
 */
public class StoreChunkOutputStreamTest {

    private static final int DATA_SIZE = 1024;

    private StoreChunkOutputStream testObj;

    @Mock
    private CacheStore mockStore;

    @Mock
    private InternalCacheEntry mockEntry;

    private static final String MOCK_KEY = "key-to-a-mock-blob";

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new StoreChunkOutputStream(mockStore, MOCK_KEY);
    }

    @Test
    public void testWritingMultipleChunks() throws IOException,
            CacheLoaderException {
        final byte[] data = randomData(DATA_SIZE);
        for (int i = 0; i < 1025; i++) {
            testObj.write(data);
        }
        testObj.close();
        verify(mockStore, times(2)).store(any(InternalCacheEntry.class));
        assertEquals(2, testObj.getNumberChunks());
    }

    @Test
    public void testWritingMultipleChunksOnVersionedKey() throws IOException,
            CacheLoaderException {
        final byte[] data = randomData(DATA_SIZE);
        when(mockStore.load(MOCK_KEY + "-0")).thenReturn(mockEntry);
        for (int i = 0; i < 1025; i++) {
            testObj.write(data);
        }
        testObj.close();
        verify(mockStore).load(MOCK_KEY + "-0");
        verify(mockStore, times(2)).store(any(InternalCacheEntry.class));
        assertEquals(2, testObj.getNumberChunks());
    }
}
