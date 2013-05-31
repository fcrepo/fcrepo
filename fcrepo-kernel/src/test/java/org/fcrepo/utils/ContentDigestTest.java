/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Mar 7, 2013
 */
public class ContentDigestTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSHA_1() {
        assertEquals("Failed to produce a proper content digest URI!", URI
                .create("urn:sha1:fake"), ContentDigest.asURI("SHA-1", "fake"));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSHA1() {
        assertEquals("Failed to produce a proper content digest URI!", URI
                .create("urn:sha1:fake"), ContentDigest.asURI("SHA1", "fake"));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetAlgorithm() {
        assertEquals("Failed to produce a proper digest algorithm!", "SHA-1",
                     ContentDigest.getAlgorithm(ContentDigest.asURI("SHA-1",
                                                                    "fake")));
    }
}
