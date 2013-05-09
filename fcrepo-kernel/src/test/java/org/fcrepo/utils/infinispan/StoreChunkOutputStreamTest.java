package org.fcrepo.utils.infinispan;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.SecureRandom;

import org.fcrepo.utils.TestHelpers;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.junit.Before;
import org.junit.Test;

public class StoreChunkOutputStreamTest {

	private static final int DATA_SIZE = 1024;
		
	private StoreChunkOutputStream testObj;
	
	private CacheStore mockStore;
	
	private InternalCacheEntry mockEntry;
	
	private String mockKey = "key-to-a-mock-blob";

	
	@Before
	public void setUp() {
		mockStore = mock(CacheStore.class);
		mockEntry = mock(InternalCacheEntry.class);
		testObj = new StoreChunkOutputStream(mockStore, mockKey);
	}
	
	@Test
	public void testWritingMultipleChunks() throws IOException, CacheLoaderException {
		byte[] data = TestHelpers.randomData(DATA_SIZE);
		for (int i=0; i< 1025; i++) {
			testObj.write(data);
		}
		testObj.close();
		verify(mockStore, times(2)).store(any(InternalCacheEntry.class));
		assertEquals(2, testObj.getNumberChunks());
	}

	@Test
	public void testWritingMultipleChunksOnVersionedKey() throws IOException, CacheLoaderException {
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
