/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
    public void testTrailingSemicolon() {
        final PreferTag preferTag = new PreferTag("foo=bar;");
        assertNotNull(preferTag.getParams());
    }

    @Test
    public void testEquals() {
        final PreferTag preferTag1 = new PreferTag("handling=lenient; received=\"minimal\"");
        final PreferTag preferTag2 = new PreferTag("handling=lenient; received=\"minimal\"");
        final PreferTag preferTag3 = PreferTag.emptyTag();
        assertEquals(preferTag1, preferTag2);
        assertEquals(preferTag1, preferTag1);  // ensure consistency
        assertEquals(preferTag2, preferTag1);
        assertNotEquals(preferTag1, preferTag3);
        assertNotEquals(null, preferTag1);
        assertNotEquals("some string", preferTag1);
    }

    @Test
    public void testHashCode() {
        doTestHashCode(new PreferTag("handling=lenient; received=\"minimal\""),
                new PreferTag("handling=lenient; received=\"minimal\""),
                true);

        doTestHashCode(new PreferTag("handling=lenient; received=\"minimal\""),
                new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + "\""),
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
