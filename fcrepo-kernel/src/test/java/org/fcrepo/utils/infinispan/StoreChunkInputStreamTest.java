/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utils.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
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
 * @author Chris Beer
 * @date Mar 14, 2013
 */
public class StoreChunkInputStreamTest {

    private static final int DATA_SIZE = 1024;

    private StoreChunkInputStream testObj;

    private CacheStore mockStore;

    private InternalCacheEntry mockEntry;

    private String mockKey = "key-to-a-mock-blob";

    private String mockFirstChunk = mockKey + "-0";


    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws CacheLoaderException {
        mockStore = mock(CacheStore.class);
        when(mockStore.containsKey(mockFirstChunk)).thenReturn(true);
        mockEntry = mock(InternalCacheEntry.class);
        testObj = new StoreChunkInputStream(mockStore, mockKey);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testRead() throws IOException {
        testObj.read();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testBufferedRead() throws IOException, CacheLoaderException {
        InternalCacheEntry mockEntry = mock(InternalCacheEntry.class);
        byte[] data = TestHelpers.randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(anyString())).thenReturn(mockEntry)
            .thenReturn(mockEntry).thenReturn(null);
        int partition = 234;
        int expected = (DATA_SIZE - partition);
        byte [] buffer = new byte[DATA_SIZE];
        long actual = testObj.read(buffer, 0, expected);
        // can read less than a block of data
        assertEquals(expected, actual);
        // will not load the next chunk if more data is available
        actual = testObj.read(buffer,0, DATA_SIZE);
        assertEquals(partition, actual);
        actual = testObj.read(buffer,0, DATA_SIZE);
        // will load the next chunk if no data is available
        assertEquals(DATA_SIZE, actual);
        // and will report the end of the data accurately
        actual = testObj.read(buffer,0, DATA_SIZE);
        assertEquals(-1, actual);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testAvailable() throws IOException, CacheLoaderException {
        byte[] data = TestHelpers.randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(mockFirstChunk)).thenReturn(mockEntry);
        assertEquals(0, testObj.available());
        int partition = 435;
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

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSkip() throws IOException, CacheLoaderException {
        long expected = (DATA_SIZE - 1);
        byte[] data = TestHelpers.randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(mockFirstChunk)).thenReturn(mockEntry);
        long actual = testObj.skip(expected);
        assertEquals(expected, actual);
        verify(mockStore).load(anyString());
        verify(mockStore).load(mockFirstChunk);
        verify(mockEntry).getValue();
        assertTrue(testObj.read() > -1);
        assertEquals(-1, testObj.read());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSkipMultipleBuffers()
        throws IOException, CacheLoaderException {
        InternalCacheEntry mockEntry = mock(InternalCacheEntry.class);
        byte[] data = TestHelpers.randomData(DATA_SIZE);
        when(mockEntry.getValue()).thenReturn(data);
        when(mockStore.load(anyString())).thenReturn(mockEntry)
            .thenReturn(mockEntry).thenReturn(null);

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

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testNextChunk() throws IOException {
        testObj.nextChunk();
    }

}
