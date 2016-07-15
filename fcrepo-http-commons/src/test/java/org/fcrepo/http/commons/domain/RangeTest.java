/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
