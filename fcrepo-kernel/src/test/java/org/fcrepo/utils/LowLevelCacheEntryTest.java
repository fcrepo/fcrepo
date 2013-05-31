/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.fcrepo.utils.FixityResult.FixityState;
import org.fcrepo.utils.infinispan.StoreChunkInputStream;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 15, 2013
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({StoreChunkInputStream.class})
public class LowLevelCacheEntryTest {

    LowLevelCacheEntry testObj;
    LowLevelCacheEntry testIspnObj;
    BinaryStore mockStore;
    BinaryStore mockIspnStore;
    CacheStore mockLowLevelCacheStore;
    BinaryKey testKey;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws Exception {
        mockStore = mock(BinaryStore.class);
        testKey = new BinaryKey("test-key-123");
        testObj = new LowLevelCacheEntry(mockStore, testKey);
        mockIspnStore = mock(InfinispanBinaryStore.class);
        mockLowLevelCacheStore = mock(CacheStore.class);
        testIspnObj =
            new LowLevelCacheEntry(mockIspnStore, mockLowLevelCacheStore,
                                   testKey);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldBeEqualIfTheKeyAndStoreAreEqual() throws Exception {
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        when(otherObj.getStore()).thenReturn(mockStore);
        when(otherObj.getKey()).thenReturn(testKey);

        assertTrue(testObj.equals(otherObj));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldBeEqualIfTheKeyStoreAndCacheStoreAreEqual()
        throws Exception {
        CacheStore mockCacheStore = mock(CacheStore.class);
        LowLevelCacheEntry ispnObject =
            new LowLevelCacheEntry(mockStore, mockCacheStore, testKey);
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        when(otherObj.getStore()).thenReturn(mockStore);
        when(otherObj.getLowLevelStore()).thenReturn(mockCacheStore);
        when(otherObj.getKey()).thenReturn(testKey);

        assertTrue(ispnObject.equals(otherObj));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldNotBeEqualIfTheOtherObjectIsTotallyDifferent()
        throws Exception {
        assertFalse(testObj.equals(""));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldNotBeEqualIfTheBinaryKeyIsDifferent() throws Exception {
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        when(otherObj.getStore()).thenReturn(mockStore);
        when(otherObj.getKey()).thenReturn(new BinaryKey("321-yek-tset"));

        assertFalse(testObj.equals(otherObj));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldNotBeEqualIfTheStoreIsDifferent() throws Exception {
        LowLevelCacheEntry otherObj = mock(LowLevelCacheEntry.class);

        BinaryStore otherStore = mock(BinaryStore.class);

        when(otherObj.getStore()).thenReturn(otherStore);
        when(otherObj.getKey()).thenReturn(testKey);

        assertFalse(testObj.equals(otherObj));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetInputStream() throws Exception {
        InputStream mockIS = mock(InputStream.class);
        when(mockStore.getInputStream(testKey)).thenReturn(mockIS);
        assertEquals(mockIS, testObj.getInputStream());
        verify(mockStore).getInputStream(testKey);
    }


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetInputStreamWithAnInfinispanStore() throws Exception {

        mockStatic(StoreChunkInputStream.class);
        InputStream mockIS = mock(InputStream.class);
        when(mockStore.getInputStream(testKey)).thenReturn(mockIS);
        InputStream is = testIspnObj.getInputStream();

        assertTrue(is instanceof StoreChunkInputStream);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testStoreValue() throws Exception {
        InputStream mockIS = mock(InputStream.class);
        testObj.storeValue(mockIS);
        verify(mockStore).storeValue(mockIS);
        CacheStoreConfig mockConfig = mock(CacheStoreConfig.class);
        when(mockConfig.toString()).thenReturn("mockCacheStoreConfig");
        when(mockLowLevelCacheStore.getCacheStoreConfig())
            .thenReturn(mockConfig);
        LowLevelCacheEntry ispnEntry =
                new LowLevelCacheEntry(mockIspnStore, mockLowLevelCacheStore,
                                       testKey);
        byte[] bytes = new byte[]{0,1,2,3,4};
        ispnEntry.storeValue(new ByteArrayInputStream(bytes));
        verify(mockLowLevelCacheStore).store(any(InternalCacheEntry.class));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetExternalIdentifier() throws Exception {
        when(mockStore.toString()).thenReturn("i-am-a-mock-store");
        testObj.setExternalId("zyx");
        assertEquals("zyx/i-am-a-mock-store", testObj.getExternalIdentifier());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testFileSystemExternalIdentifier() throws Exception {
        FileSystemBinaryStore fsbs = mock(FileSystemBinaryStore.class);
        when(fsbs.getDirectory()).thenReturn(new File("/tmp/xyz"));
        LowLevelCacheEntry filesystemTestObj = new LowLevelCacheEntry(fsbs,
                                                                      testKey);

        filesystemTestObj.setExternalId("zyx");
        final String identifier = filesystemTestObj.getExternalIdentifier();
        assertTrue(identifier.startsWith("zyx/org.modeshape.jcr.value.binary" +
                                         ".FileSystemBinaryStore"));
        // some test junk in the middle
        assertTrue(identifier.endsWith("/tmp/xyz"));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetFixity()
        throws BinaryStoreException, IOException, NoSuchAlgorithmException {
        LowLevelCacheEntry ispnEntry =
                new LowLevelCacheEntry(mockStore, testKey);
        final byte[] bytes = new byte[]{0,1,2,3,4};
        when(mockStore.getInputStream(testKey)).thenAnswer(
                new Answer<InputStream>() {

                    @Override
                    public InputStream answer(InvocationOnMock invocation)
                            throws Throwable {
                        return new ByteArrayInputStream(bytes);
                    }

                }
        );
        final MessageDigest d = MessageDigest.getInstance("SHA-1");
        byte[] digested = d.digest(bytes);
        URI testCS = ContentDigest.asURI("SHA-1", digested);
        System.out.println(testCS);
        FixityResult actual = ispnEntry.checkFixity(testCS, bytes.length, d);
        assertEquals(1, actual.status.size());
        assertEquals(actual.status.iterator().next().toString(),
                true, actual.status.contains(FixityState.SUCCESS));

        // report the wrong size
        actual = ispnEntry.checkFixity(testCS, bytes.length + 1, d);
        assertEquals(1, actual.status.size());
        assertEquals(actual.status.iterator().next().toString(),
                true, actual.status.contains(FixityState.BAD_SIZE));
        // break the digest
        digested[0] += 9;
        testCS = ContentDigest.asURI("SHA-1", digested);
        actual = ispnEntry.checkFixity(testCS, bytes.length, d);
        assertEquals(1, actual.status.size());
        assertEquals(actual.status.iterator().next().toString(),
                true, actual.status.contains(FixityState.BAD_CHECKSUM));
        // report the wrong size and the wrong digest
        actual = ispnEntry.checkFixity(testCS, bytes.length + 1, d);
        assertEquals(2, actual.status.size());
        assertEquals(true, actual.status.contains(FixityState.BAD_CHECKSUM));
        assertEquals(true, actual.status.contains(FixityState.BAD_SIZE));
    }

}
