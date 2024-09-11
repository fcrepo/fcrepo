/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
        assertEquals(-1L, range.size());
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

        assertEquals(-1L, range.start());
        assertEquals(50L, range.end());
        assertEquals(50L, range.size());
        assertTrue(range.hasRange());

    }

    @Test
    public void testGarbageRangeParsing() {
        final Range range = Range.convert("something-thats-not-a-range");

        assertFalse(range.hasRange());
        assertEquals(-1L, range.start());
        assertEquals(-1L, range.end());
        assertEquals(-1L, range.size());

    }

    @Test
    public void testRangeIsSatisfiable1() {
        // If we specify a start point less than the length of the file, the range is satisfiable
        final Range range = Range.convert("bytes=0-100");
        assertTrue(range.rangeOfLength(101).isSatisfiable());
        assertTrue(range.rangeOfLength(5).isSatisfiable());
        assertTrue(range.rangeOfLength(10).isSatisfiable());
        assertTrue(range.rangeOfLength(200).isSatisfiable());
    }

    @Test
    public void testRangeIsSatisfiable2() {
        // If we specify a start point greater than the length of the file, the range is not satisfiable
        final Range range = Range.convert("bytes=101-200");
        assertFalse(range.rangeOfLength(100).isSatisfiable());
        assertFalse(range.rangeOfLength(5).isSatisfiable());
    }

    @Test
    public void testRangeIsSatisfiable3() {
        final Range range = Range.convert("bytes=0-8199");
        assertTrue(range.rangeOfLength(9040).isSatisfiable());
    }

    @Test
    public void testRangeOfContentLength1() {
        final Range range = Range.convert("bytes=0-100");
        assertEquals(101, range.size());
        final var rangeOfLength = range.rangeOfLength(200);
        assertEquals(0, rangeOfLength.start());
        assertEquals(100, rangeOfLength.end());
        assertEquals(101, rangeOfLength.size());
    }

    @Test
    public void testRangeOfContentLength2() {
        final Range range = Range.convert("bytes=5-");
        assertEquals(-1, range.size());
        final var rangeOfLength = range.rangeOfLength(200);
        assertEquals(5, rangeOfLength.start());
        assertEquals(199, rangeOfLength.end());
        assertEquals(195, rangeOfLength.size());
    }

    @Test
    public void testRangeOfContentLength3() {
        final Range range = Range.convert("bytes=-150");
        assertEquals(150, range.size());
        final var rangeOfLength = range.rangeOfLength(200);
        assertEquals(50, rangeOfLength.start());
        assertEquals(199, rangeOfLength.end());
        assertEquals(150, rangeOfLength.size());
    }

    /**
     * For a content size smaller than the requested range, we need to adjust the range.
     */
    @Test
    public void testRangeSizeIsValid() {
        final Range range = Range.convert("bytes=0-100");
        assertEquals(101, range.size());
        final var rangeOfLength = range.rangeOfLength(90);
        assertEquals(0, rangeOfLength.start());
        assertEquals(89, rangeOfLength.end());
        assertEquals(90, rangeOfLength.size());
    }

    @Test
    public void testSpecExamples() {
        final Range range1 = Range.convert("bytes=-500");
        final var rangeOfLength1 = range1.rangeOfLength(10000);
        assertEquals(9500, rangeOfLength1.start());
        assertEquals(9999, rangeOfLength1.end());
        assertEquals(500, rangeOfLength1.size());

        final Range range2 = Range.convert("bytes=9500-");
        final var rangeOfLength2 = range2.rangeOfLength(10000);
        assertEquals(9500, rangeOfLength2.start());
        assertEquals(9999, rangeOfLength2.end());
        assertEquals(500, rangeOfLength2.size());
    }
}
