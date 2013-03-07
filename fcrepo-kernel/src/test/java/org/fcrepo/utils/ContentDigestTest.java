
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

public class ContentDigestTest {

    @Test
    public void testSHA_1() {
        assertEquals("Failed to produce a proper content digest URI!", URI
                .create("urn:sha1:fake"), ContentDigest.asURI("SHA-1", "fake"));
    }

}
