
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
        MessageDigest mockDigest = mock(MessageDigest.class);
        URI testUri = URI.create("urn:foo:bar");
        long testSize = new SecureRandom().nextLong();
        CheckCacheEntryFixity testObj =
                new CheckCacheEntryFixity(mockDigest, testUri, testSize);
        LowLevelCacheEntry mockEntry = mock(LowLevelCacheEntry.class);
        testObj.apply(mockEntry);
        verify(mockEntry).checkFixity(testUri, testSize, mockDigest);
    }
}
