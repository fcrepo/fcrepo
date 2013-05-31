/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 14, 2013
 */
public class FixityResultTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testEquals() throws Exception {

        assertEquals(new FixityResult(100L, new URI("urn:123")),
                new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(99L, new URI("urn:123")),
                new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(100L, new URI("urn:321")),
                new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(99L, new URI("urn:321")),
                new FixityResult(100L, new URI("urn:123")));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testHashCode() throws Exception {

        assertEquals(new FixityResult(100L, new URI("urn:123")).hashCode(),
                new FixityResult(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResult(99L, new URI("urn:123")).hashCode(),
                new FixityResult(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResult(100L, new URI("urn:321")).hashCode(),
                new FixityResult(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResult(99L, new URI("urn:321")).hashCode(),
                new FixityResult(100L, new URI("urn:123")).hashCode());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testIsSuccess() throws Exception {
        FixityResult result = new FixityResult(100L, new URI("urn:123"));
        result.status.add(FixityResult.FixityState.SUCCESS);
        assertTrue("expected fixity to be a success", result.isSuccess());


        result = new FixityResult(100L, new URI("urn:123"));
        assertFalse("expected fixity to not be a success", result.isSuccess());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testMatchesArguments() throws Exception {
        FixityResult result = new FixityResult(100L, new URI("urn:123"));
        assertTrue("expected fixity to match",
                   result.matches(100L, new URI("urn:123")));
        assertFalse("unexpected match when size differs",
                    result.matches(99L, new URI("urn:123")));
        assertFalse("unexpected match when checksum differs",
                    result.matches(100L, new URI("urn:312")));
        assertFalse("unexpected match when size and checksum differs",
                    result.matches(99L, new URI("urn:312")));
    }
}
