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
package org.fcrepo.http.commons.domain;

import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Test;

/**
 * @author cabeer
 */
public class PreferTagTest {

    @Test
    public void testEmpty() {
        final PreferTag preferTag = PreferTag.emptyTag();
        assertEquals("", preferTag.getTag());
        assertEquals("", preferTag.getValue());
        assertTrue(preferTag.getParams().isEmpty());
    }

    @Test
    public void testTrailingSemicolon() throws ParseException {
        final PreferTag preferTag = new PreferTag("foo=bar;");
        assertNotNull(preferTag.getParams());
    }

    @Test
    public void testEquals() throws ParseException {
        final PreferTag preferTag1 = new PreferTag("handling=lenient; received=\"minimal\"");
        final PreferTag preferTag2 = new PreferTag("handling=lenient; received=\"minimal\"");
        final PreferTag preferTag3 = PreferTag.emptyTag();
        assertTrue(preferTag1.equals(preferTag2));
        assertTrue(preferTag1.equals(preferTag1));  // ensure consistency
        assertTrue(preferTag2.equals(preferTag1));
        assertFalse(preferTag1.equals(preferTag3));
        assertFalse(preferTag1.equals(null));
        assertFalse(preferTag1.equals("some string"));
    }

    @Test
    public void testHashCode() throws ParseException {
        doTestHashCode(new PreferTag("handling=lenient; received=\"minimal\""),
                new PreferTag("handling=lenient; received=\"minimal\""),
                true);

        doTestHashCode(new PreferTag("handling=lenient; received=\"minimal\""),
                new PreferTag("return=representation; include=\"" + LDP_NAMESPACE + "PreferMinimalContainer\""),
                false);

        doTestHashCode(new PreferTag("handling=lenient; received=\"minimal\""),
                PreferTag.emptyTag(),
                false);
    }

    private static void doTestHashCode(final PreferTag tag0, final PreferTag tag1, final boolean expectEqual)    {
        assertEquals(expectEqual, tag0.equals(tag1));
        assertEquals(expectEqual, tag0.hashCode() == tag1.hashCode());
    }

}
