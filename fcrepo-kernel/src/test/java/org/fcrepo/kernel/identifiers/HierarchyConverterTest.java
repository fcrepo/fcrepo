/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.identifiers;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Strings.repeat;
import static java.util.Collections.nCopies;
import static java.util.regex.Pattern.compile;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * <p>HierarchyConverterTest class.</p>
 *
 * @author ajs6f
 */
public class HierarchyConverterTest {

    private static final String separator = "/";

    private HierarchyConverter testTranslator;

    private static final String[] testIdSegments = {"test1", "test2", "test3"};

    private static final String startingSegments = testIdSegments[0] + separator + testIdSegments[1];

    private static final String endingSegment = testIdSegments[2];

    private final String testId = on(separator).join(testIdSegments);

    private static final Logger log = getLogger(HierarchyConverterTest.class);

    @Before
    public void setUp() {
        testTranslator = new HierarchyConverter();
        testTranslator.setPrefix("");
        testTranslator.setLevels(0);
        testTranslator.setLength(1);
        testTranslator.setSeparator(separator);
    }

    @Test
    public void testNullForward() {
        assertNull("Should get null forward for null input!", testTranslator.convert(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadSegmentLength() {
        testTranslator.setLength(0);
    }

    @Test
    public void testNoOpForward() {
        assertEquals("Should not have altered input identifier!", testId, testTranslator.convert(testId));
    }

    @Test
    public void testVaryingSegments() {
        for (byte length = 1; length < 5; length++) {
            for (byte levels = 1; levels < 5; levels++) {
                testRoundTrip(levels, length);
            }
        }
    }

    public void testRoundTrip(final byte levels, final byte length) {
        testTranslator.setLevels(levels);
        testTranslator.setLength(length);
        final String result = testTranslator.reverse().convert(testId);
        final String testRegexp =
            startingSegments + separator + hierarchyRegexpSection(levels, length) + separator + endingSegment;
        final Matcher matches = compile(testRegexp).matcher(result);
        log.debug("Got result of translation: {}", result);
        log.debug("Matching against test pattern: {}", testRegexp);
        assertTrue("Did not find the appropriate modification to the input identifier!", matches.matches());
        final String shouldBeOriginal = testTranslator.convert(result);
        assertEquals("Didn't get original back!", testId, shouldBeOriginal);
    }

    private static String hierarchyRegexpSection(final byte levels, final byte length) {
        return on(separator).join(nCopies(levels, repeat("\\w", length)));
    }

    @Test
    public void testRecurse() {
        testTranslator.setLevels(3);
        testTranslator.setLength(3);
        final String firstPass = testTranslator.reverse().convert(testId);
        final String secondPass = testTranslator.reverse().convert(firstPass);
        assertEquals("Failed to retrieve original after two stages of translation!", testId, testTranslator
                .convert(testTranslator.convert(secondPass)));
    }

    @Test
    public void testWeirdIds() {
        testTranslator.setLevels(3);
        testTranslator.setLength(3);
        String result;

        result = testTranslator.reverse().convert("");
        log.debug("Empty identifier translated into {}.", "", result);
        assertEquals("Should not have altered empty identifier!", "", testTranslator.convert(result));

        result = testTranslator.reverse().convert(separator);
        log.debug("Separator identifier translated into {}.", separator, result);
        assertEquals("Should have altered separator identifier to empty identifier!", "", testTranslator
                .convert(result));
    }

    @Test
    public void testContentPaths() {
        testTranslator.setLevels(0);
        testTranslator.setLength(1);
        String result;

        final String externalTestId = on(separator).join(testId, FCR_CONTENT);
        final String internalTestId = on(separator).join(testId, JCR_CONTENT);

        result = testTranslator.reverse().convert(externalTestId);
        assertEquals("Should have swapped content suffix to JCR!", internalTestId, result);

        result = testTranslator.convert(internalTestId);
        assertEquals("Should have swapped content suffix to FCR!", externalTestId, result);
    }
}
