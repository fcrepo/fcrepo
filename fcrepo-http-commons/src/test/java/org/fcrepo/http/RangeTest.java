package org.fcrepo.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RangeTest {
    @Test
    public void testUnboundedRange() {
        final Range range = new Range(5);

        assertEquals(5L, range.start());
        assertEquals(-1L, range.end());

    }
    @Test
    public void testRangeParsing() {
        final Range range = Range.convert("bytes=50-100");

        assertEquals(50L, range.start());
        assertEquals(100L, range.end());
        assertEquals(51L, range.size());
        assertTrue(range.hasRange());

    }

    @Test
    public void testUnboundedUpperRangeParsing() {
        final Range range = Range.convert("bytes=50-");

        assertEquals(50L, range.start());
        assertEquals(-1L, range.end());
        assertEquals(-1L, range.size());
        assertTrue(range.hasRange());

    }

    @Test
    public void testUnboundedLowerRangeParsing() {
        final Range range = Range.convert("bytes=-50");

        assertEquals(0L, range.start());
        assertEquals(50L, range.end());
        assertEquals(51L, range.size());
        assertTrue(range.hasRange());

    }

    @Test
    public void testGarbageRangeParsing() {
        final Range range = Range.convert("something-thats-not-a-range");

        assertFalse(range.hasRange());
        assertEquals(0L, range.start());
        assertEquals(-1L, range.end());
        assertEquals(-1L, range.size());

    }
}
