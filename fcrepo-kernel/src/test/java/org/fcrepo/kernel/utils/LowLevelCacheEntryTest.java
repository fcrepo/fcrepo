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
package org.fcrepo.kernel.utils;

import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.utils.impl.LocalBinaryStoreEntry;
import org.fcrepo.kernel.utils.infinispan.StoreChunkInputStream;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({StoreChunkInputStream.class})
public class LowLevelCacheEntryTest {

    private static final Logger LOGGER =
            getLogger(LowLevelCacheEntryTest.class);

    private CacheEntry testObj;

    @Mock
    private InputStream mockIS;

    @Mock
    private BinaryStore mockStore;

    @Mock
    private BinaryStore otherStore;

    @Mock
    private CacheStore mockLowLevelCacheStore;

    @Mock
    private CacheStoreConfig mockConfig;

    @Mock
    private FileSystemBinaryStore fsbs;

    @Mock
    private Property mockProperty;

    @Mock
    private BinaryValue mockBinary;

    private BinaryKey testKey;

    @Before
    public void setUp() throws Exception {
        testKey = new BinaryKey("test-key-123");
        testObj = new LocalBinaryStoreEntry(mockStore, mockProperty);
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockBinary.getKey()).thenReturn(testKey);
    }

    @Test
    public void shouldBeEqualIfTheKeyAndStoreAreEqual() throws Exception {
        final CacheEntry otherObj =
                new LocalBinaryStoreEntry(mockStore, mockProperty);
        assertTrue(testObj.equals(otherObj));
    }

    @Test
    public void shouldNotBeEqualIfTheOtherObjectIsTotallyDifferent()
            throws Exception {
        assertFalse(testObj.equals(""));
    }


    @Test
    public void shouldNotBeEqualIfTheStoreIsDifferent() throws Exception {

        final CacheEntry otherObj =
                new LocalBinaryStoreEntry(otherStore, mockProperty);
        assertFalse(testObj.equals(otherObj));
    }

    @Test
    public void testGetInputStream() throws Exception {
        when(mockStore.getInputStream(testKey)).thenReturn(mockIS);
        assertEquals(mockIS, testObj.getInputStream());
        verify(mockStore).getInputStream(testKey);
    }

    @Test
    public void testGetFixity() throws RepositoryException, NoSuchAlgorithmException {
        final CacheEntry ispnEntry =
                new LocalBinaryStoreEntry(mockStore, mockProperty);
        final byte[] bytes = new byte[] {0, 1, 2, 3, 4};
        when(mockStore.getInputStream(testKey)).thenAnswer(
                new Answer<InputStream>() {

                    @Override
                    public InputStream
                            answer(final InvocationOnMock invocation)
                                    throws Throwable {
                        return new ByteArrayInputStream(bytes);
                    }

                });
        final MessageDigest d = MessageDigest.getInstance("SHA-1");
        final byte[] digested = d.digest(bytes);
        URI testCS = ContentDigest.asURI("SHA-1", digested);
        LOGGER.debug(testCS.toString());
        FixityResult actual = ispnEntry.checkFixity(testCS, bytes.length).iterator().next();
        assertEquals(1, actual.getStatus().size());
        assertEquals(actual.getStatus().iterator().next().toString(), true,
                actual.getStatus().contains(SUCCESS));

        // report the wrong size
        actual = ispnEntry.checkFixity(testCS, bytes.length + 1).iterator().next();
        assertEquals(1, actual.getStatus().size());
        assertEquals(actual.getStatus().iterator().next().toString(), true,
                actual.getStatus().contains(BAD_SIZE));
        // break the digest
        digested[0] += 9;
        testCS = ContentDigest.asURI("SHA-1", digested);
        actual = ispnEntry.checkFixity(testCS, bytes.length).iterator().next();
        assertEquals(1, actual.getStatus().size());
        assertEquals(actual.getStatus().iterator().next().toString(), true,
                actual.getStatus().contains(BAD_CHECKSUM));
        // report the wrong size and the wrong digest
        actual = ispnEntry.checkFixity(testCS, bytes.length + 1).iterator().next();
        assertEquals(2, actual.getStatus().size());
        assertEquals(true, actual.getStatus().contains(BAD_CHECKSUM));
        assertEquals(true, actual.getStatus().contains(BAD_SIZE));
    }

}
