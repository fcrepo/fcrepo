/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.fcrepo.kernel.api.utils.FixityResult;
import org.junit.Test;

/**
 * <p>FixityResultImplTest class.</p>
 *
 * @author ksclarke
 */
public class FixityResultImplTest {

    @Test
    public void testEquals() throws Exception {

        assertEquals(new FixityResultImpl(100L, new URI("urn:123")),
                new FixityResultImpl(100L, new URI("urn:123")));

        assertNotEquals(new FixityResultImpl(99L, new URI("urn:123")),
                new FixityResultImpl(100L, new URI("urn:123")));

        assertNotEquals(new FixityResultImpl(100L, new URI("urn:321")),
                new FixityResultImpl(100L, new URI("urn:123")));

        assertNotEquals(new FixityResultImpl(99L, new URI("urn:321")),
                new FixityResultImpl(100L, new URI("urn:123")));
    }

    @Test
    public void testHashCode() throws Exception {

        assertEquals(new FixityResultImpl(100L, new URI("urn:123")).hashCode(),
                new FixityResultImpl(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResultImpl(99L, new URI("urn:123")).hashCode(),
                new FixityResultImpl(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResultImpl(100L, new URI("urn:321")).hashCode(),
                new FixityResultImpl(100L, new URI("urn:123")).hashCode());

        assertNotEquals(new FixityResultImpl(99L, new URI("urn:321")).hashCode(),
                new FixityResultImpl(100L, new URI("urn:123")).hashCode());
    }

    @Test
    public void testMatchesArguments() throws Exception {
        final FixityResult result = new FixityResultImpl(100L, new URI("urn:123"));
        assertTrue("expected fixity to match", result.matches(100L, new URI(
                "urn:123")));
        assertFalse("unexpected match when size differs", result.matches(99L,
                new URI("urn:123")));
        assertFalse("unexpected match when checksum differs", result.matches(
                100L, new URI("urn:312")));
        assertFalse("unexpected match when size and checksum differs", result
                .matches(99L, new URI("urn:312")));
    }
}
