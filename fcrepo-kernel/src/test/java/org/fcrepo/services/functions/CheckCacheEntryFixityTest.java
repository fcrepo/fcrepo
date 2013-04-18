
package org.fcrepo.services.functions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.modeshape.jcr.value.binary.BinaryStoreException;

public class CheckCacheEntryFixityTest {

    @Test
    public void testApply() throws BinaryStoreException {
        final MessageDigest mockDigest = mock(MessageDigest.class);
        final URI testUri = URI.create("urn:foo:bar");
        final long testSize = new SecureRandom().nextLong();
        final CheckCacheEntryFixity testObj =
                new CheckCacheEntryFixity(mockDigest, testUri, testSize);
        final LowLevelCacheEntry mockEntry = mock(LowLevelCacheEntry.class);
        testObj.apply(mockEntry);
        verify(mockEntry).checkFixity(testUri, testSize, mockDigest);
    }
}
