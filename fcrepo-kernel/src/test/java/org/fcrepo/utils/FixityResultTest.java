package org.fcrepo.utils;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FixityResultTest {
    @Test
    public void testEquals() throws Exception {

        assertEquals(new FixityResult(100L, new URI("urn:123")), new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(99L, new URI("urn:123")), new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(100L, new URI("urn:321")), new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(99L, new URI("urn:321")), new FixityResult(100L, new URI("urn:123")));
    }
}
