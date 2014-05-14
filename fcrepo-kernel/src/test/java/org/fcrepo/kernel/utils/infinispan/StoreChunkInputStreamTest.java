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
package org.fcrepo.kernel.utils.infinispan;

import static org.fcrepo.kernel.utils.TestHelpers.randomData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import org.fcrepo.kernel.utils.TestHelpers;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>StoreChunkInputStreamTest class.</p>
 *
 * @author awoods
 */
public class StoreChunkInputStreamTest {

    private static final int DATA_SIZE = 1024;

    private StoreChunkInputStream testObj;

    @Mock
    private CacheStore mockStore;

    @Mock
    private InternalCacheEntry mockEntry;

    private static final String MOCK_KEY = "key-to-a-mock-blob";

    private static final String MOCK_FIRST_CHUNK = MOCK_KEY + "-0";

    @Before
    public void setUp() throws CacheLoaderException {
        initMocks(this);
        when(mockStore.containsKey(MOCK_FIRST_CHUNK)).thenReturn(true);
        testObj = new StoreChunkInputStream(mockStore, MOCK_KEY);
    }

    @Test
    public void testRead() throws IOException {
        testObj.read();
    }

    @Test
    public void testBufferedRead() throws IOException, CacheLoaderException {
        final byte[] data = randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(anyString())).thenReturn(mockEntry).thenReturn(
                mockEntry).thenReturn(null);
        final int partition = 234;
        final int expected = (DATA_SIZE - partition);
        final byte[] buffer = new byte[DATA_SIZE];
        long actual = testObj.read(buffer, 0, expected);
        // can read less than a block of data
        assertEquals(expected, actual);
        // will not load the next chunk if more data is available
        actual = testObj.read(buffer, 0, DATA_SIZE);
        assertEquals(partition, actual);
        actual = testObj.read(buffer, 0, DATA_SIZE);
        // will load the next chunk if no data is available
        assertEquals(DATA_SIZE, actual);
        // and will report the end of the data accurately
        actual = testObj.read(buffer, 0, DATA_SIZE);
        assertEquals(-1, actual);

    }

    @Test
    public void testAvailable() throws IOException, CacheLoaderException {
        final byte[] data = TestHelpers.randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(MOCK_FIRST_CHUNK)).thenReturn(mockEntry);
        assertEquals(0, testObj.available());
        final int partition = 435;
        testObj.skip(partition);
        // part of the first buffer remains
        assertEquals(DATA_SIZE - partition, testObj.available());
        testObj.skip(DATA_SIZE - partition);
        // none of the first buffer remains
        assertEquals(0, testObj.available());
        testObj.skip(1);
        // no buffers remain
        assertEquals(-1, testObj.available());
    }

    @Test
    public void testSkip() throws IOException, CacheLoaderException {
        final long expected = (DATA_SIZE - 1);
        final byte[] data = randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(MOCK_FIRST_CHUNK)).thenReturn(mockEntry);
        final long actual = testObj.skip(expected);
        assertEquals(expected, actual);
        verify(mockStore).load(anyString());
        verify(mockStore).load(MOCK_FIRST_CHUNK);
        verify(mockEntry).getValue();
        assertTrue(testObj.read() > -1);
        assertEquals(-1, testObj.read());
    }

    @Test
    public void testSkipMultipleBuffers() throws IOException,
            CacheLoaderException {
        final byte[] data = randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(anyString())).thenReturn(mockEntry).thenReturn(
                mockEntry).thenReturn(null);
        long expected = (DATA_SIZE);
        // ask for more than the buffer
        long actual = testObj.skip(DATA_SIZE + 1);
        // we should skip only one complete buffer
        assertEquals(expected, actual);
        // ok, skip all but the last byte remaining
        expected = (DATA_SIZE - 1);
        actual = testObj.skip(expected);
        // new buffer, mostly skipped
        assertEquals(expected, actual);
        // we should still have 1 more byte
        assertTrue(testObj.read() > -1);
        // but only the one
        assertEquals(-1, testObj.read());
        // and we only had two cacheEntries
        verify(mockEntry, times(2)).getValue();
    }

    @Test
    public void testNextChunk() throws IOException {
        testObj.nextChunk();
    }

}
