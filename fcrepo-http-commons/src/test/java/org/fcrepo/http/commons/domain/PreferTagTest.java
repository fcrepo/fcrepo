/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void testTrailingSemicolon() {
        final PreferTag preferTag = new PreferTag("foo=bar;");
        assertNotNull(preferTag.getParams());
    }

    @Test
    public void testEquals() {
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
