/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
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
