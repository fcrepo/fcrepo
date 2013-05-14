package org.fcrepo.utils;

import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.infinispan.loaders.CacheStore;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StoreChunkInputStream.class})
public class LowLevelCacheEntryTest {

    LowLevelCacheEntry testObj;
    LowLevelCacheEntry testIspnObj;
    BinaryStore mockStore;
    BinaryStore mockIspnStore;
    CacheStore mockLowLevelCacheStore;
    BinaryKey key;

    @Before
    public void setUp() throws Exception {
        mockStore = mock(BinaryStore.class);
        key = new BinaryKey("test-key-123");
        testObj = new LowLevelCacheEntry(mockStore, key);
        mockIspnStore = mock(InfinispanBinaryStore.class);
        mockLowLevelCacheStore = mock(CacheStore.class);
        testIspnObj = new LowLevelCacheEntry(mockIspnStore, mockLowLevelCacheStore, key);
    }

    @Test
    public void shouldBeEqualIfTheKeyAndStoreAreEqual() throws Exception {
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        when(otherObj.getStore()).thenReturn(mockStore);
        when(otherObj.getKey()).thenReturn(new BinaryKey("test-key-123"));

        assertTrue(testObj.equals(otherObj));
    }

    @Test
    public void shouldBeEqualIfTheKeyStoreAndCacheStoreAreEqual() throws Exception {
        CacheStore mockCacheStore = mock(CacheStore.class);
        LowLevelCacheEntry ispnObject = new LowLevelCacheEntry(mockStore, mockCacheStore, key);
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        when(otherObj.getStore()).thenReturn(mockStore);
        when(otherObj.getLowLevelStore()).thenReturn(mockCacheStore);
        when(otherObj.getKey()).thenReturn(new BinaryKey("test-key-123"));

        assertTrue(ispnObject.equals(otherObj));
    }

    @Test
    public void shouldNotBeEqualIfTheOtherObjectIsTotallyDifferent() throws Exception {
        assertFalse(testObj.equals(""));
    }

    @Test
    public void shouldNotBeEqualIfTheBinaryKeyIsDifferent() throws Exception {
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        when(otherObj.getStore()).thenReturn(mockStore);
        when(otherObj.getKey()).thenReturn(new BinaryKey("321-yek-tset"));

        assertFalse(testObj.equals(otherObj));
    }

    @Test
    public void shouldNotBeEqualIfTheStoreIsDifferent() throws Exception {
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        BinaryStore otherStore = mock(BinaryStore.class);

        when(otherObj.getStore()).thenReturn(otherStore);
        when(otherObj.getKey()).thenReturn(new BinaryKey("test-key-123"));

        assertFalse(testObj.equals(otherObj));
    }

    @Test
    public void testGetInputStream() throws Exception {
        InputStream mockIS = mock(InputStream.class);
        when(mockStore.getInputStream(key)).thenReturn(mockIS);
        assertEquals(mockIS, testObj.getInputStream());
        verify(mockStore).getInputStream(key);
    }


    @Test
    public void testGetInputStreamWithAnInfinispanStore() throws Exception {

        mockStatic(StoreChunkInputStream.class);
        InputStream mockIS = mock(InputStream.class);
        when(mockStore.getInputStream(key)).thenReturn(mockIS);
        InputStream is = testIspnObj.getInputStream();

        assertTrue(is instanceof StoreChunkInputStream);
    }

    @Test
    @Ignore("Sure, it does this.. but we actually don't replace values in the store")
    public void testStoreValue() throws Exception {
        InputStream mockIS = mock(InputStream.class);
        testObj.storeValue(mockIS);
        verify(mockStore).storeValue(mockIS);
    }

    @Test
    public void testGetExternalIdentifier() throws Exception {
        when(mockStore.toString()).thenReturn("i-am-a-mock-store");
        testObj.setExternalId("zyx");
        assertEquals("zyx/i-am-a-mock-store", testObj.getExternalIdentifier());
    }

    @Test
    public void testFileSystemExternalIdentifier() throws Exception {
        FileSystemBinaryStore fsbs = mock(FileSystemBinaryStore.class);
        when(fsbs.getDirectory()).thenReturn(new File("/tmp/xyz"));
        LowLevelCacheEntry filesystemTestObj = new LowLevelCacheEntry(fsbs, key);

        filesystemTestObj.setExternalId("zyx");
        final String identifier = filesystemTestObj.getExternalIdentifier();
        assertTrue(identifier.startsWith("zyx/org.modeshape.jcr.value.binary.FileSystemBinaryStore"));
        // some test junk in the middle
        assertTrue(identifier.endsWith("/tmp/xyz"));
    }

}
