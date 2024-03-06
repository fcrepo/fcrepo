/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * <p>RangeTest class.</p>
 *
 * @author awoods
 */
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
