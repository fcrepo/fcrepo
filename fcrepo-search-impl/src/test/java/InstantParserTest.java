/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
import org.fcrepo.search.impl.InstantParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author dbernstein
 */
public class InstantParserTest {
    @Test
    public void test() {
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("2020-01-01").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("20200101").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("2020-01-01T00:00:00Z").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("2020-01-01 00:00:00").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("20200101 00:00:00").toString());
        assertEquals("2020-01-01T07:00:00Z", InstantParser.parse("2020-01-01T00:00:00-07:00").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("20200101 00:00:00").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("Wed, 1 Jan 2020 00:00:00 GMT").toString());
    }

    @Test
    public void testInvalidString() {
        assertThrows(IllegalArgumentException.class, () -> InstantParser.parse("2020-01-01 24").toString());
    }
}
