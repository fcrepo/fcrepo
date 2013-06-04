/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date Apr 3, 2013
 */
public class CheckCacheEntryFixityTest {

    /**
     * @throws BinaryStoreException, NoSuchAlgorithmException 
     * @todo Add Documentation.
     */
    @Test
    public void testApply() throws BinaryStoreException, BinaryStoreException, NoSuchAlgorithmException {
        final MessageDigest testDigest = MessageDigest.getInstance("SHA-1");
        final URI testUri = URI.create("urn:foo:bar");
        final long testSize = new SecureRandom().nextLong();
        final CheckCacheEntryFixity testObj =
                new CheckCacheEntryFixity(testDigest, testUri, testSize);
        final LowLevelCacheEntry mockEntry = mock(LowLevelCacheEntry.class);
        testObj.apply(mockEntry);
        verify(mockEntry).checkFixity(testUri, testSize, "SHA-1");
    }
}
