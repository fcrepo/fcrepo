package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.URI;

import org.junit.Test;

public class FixityResultTest {
    @Test
    public void testEquals() throws Exception {

        assertEquals(new FixityResult(100L, new URI("urn:123")), new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(99L, new URI("urn:123")), new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(100L, new URI("urn:321")), new FixityResult(100L, new URI("urn:123")));

        assertNotEquals(new FixityResult(99L, new URI("urn:321")), new FixityResult(100L, new URI("urn:123")));
    }

    @Test
    public void testHashCode() throws Exception {

        assertEquals(new FixityResult(100L, new URI("urn:123")).hashCode(), new FixityResult(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResult(99L, new URI("urn:123")).hashCode(), new FixityResult(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResult(100L, new URI("urn:321")).hashCode(), new FixityResult(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResult(99L, new URI("urn:321")).hashCode(), new FixityResult(100L, new URI("urn:123")).hashCode());
    }
}
