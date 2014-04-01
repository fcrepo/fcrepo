
package org.fcrepo.kernel.identifiers;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Strings.repeat;
import static java.util.Collections.nCopies;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class HierarchyTranslatorTest {

    private static final String separator = "/";

    private HierarchyTranslator testHierarchyTranslator;

    private static final String[] testIdSegments = {"test1", "test2", "test3"};

    private static final String startingSegments = testIdSegments[0] + separator + testIdSegments[1];

    private static final String endingSegment = testIdSegments[2];

    private final String testId = on(separator).join(testIdSegments);

    private static final Logger log = getLogger(HierarchyTranslatorTest.class);

    @Before
    public void setUp() {
        testHierarchyTranslator = new HierarchyTranslator();
        testHierarchyTranslator.setPrefix("");
        testHierarchyTranslator.setLevels(0);
        testHierarchyTranslator.setLength(1);
        testHierarchyTranslator.setSeparator(separator);
    }

    @Test
    public void testNullForward() {
        assertNull("Should get null forward for null input!", testHierarchyTranslator.convert(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadSegmentLength() {
        testHierarchyTranslator.setLength(0);
    }

    @Test
    public void testNoOpForward() {
        assertEquals("Should not have altered input identifier!", testId, testHierarchyTranslator.convert(testId));
    }

    @Test
    public void testVaryingSegments() {
        for (byte length = 1; length < 5; length++) {
            for (byte levels = 1; levels < 5; levels++) {
                testNSegments(levels, length);
            }
        }
    }

    public void testNSegments(final byte levels, final byte length) {
        testHierarchyTranslator.setLevels(levels);
        testHierarchyTranslator.setLength(length);
        final String result = testHierarchyTranslator.convert(testId);
        final String testRegexp =
            startingSegments + separator + hierarchyRegexpSection(levels, length) + separator + endingSegment;
        final Matcher matches = compile(testRegexp).matcher(result);
        log.debug("Got result of translation: {}", result);
        log.debug("Matching against test pattern: {}", testRegexp);
        assertTrue("Did not find the appropriate modification to the input identifier!", matches.matches());
        final String shouldBeOriginal = testHierarchyTranslator.reverse().convert(result);
        assertEquals("Didn't get original back!", testId, shouldBeOriginal);
    }

    private static String hierarchyRegexpSection(final byte levels, final byte length) {
        return on(separator).join(nCopies(levels, repeat("\\w", length)));
    }
}
