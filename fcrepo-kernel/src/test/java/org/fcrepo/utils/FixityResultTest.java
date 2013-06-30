/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

public class FixityResultTest {

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

    @Test
    public void testIsSuccess() throws Exception {
        FixityResult result = new FixityResult(100L, new URI("urn:123"));
        result.status.add(FixityResult.FixityState.SUCCESS);
        assertTrue("expected fixity to be a success", result.isSuccess());


        result = new FixityResult(100L, new URI("urn:123"));
        assertFalse("expected fixity to not be a success", result.isSuccess());
    }

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
