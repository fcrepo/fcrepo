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
package org.fcrepo.kernel.modeshape.identifiers;

import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * @author ajs6f
 * @since Apr 2, 2014
 */
public class NamespaceConverterTest {

    private NamespaceConverter testTranslator;

    private static final String testId1 = JCR_NAMESPACE + "test1";

    private static final String testId2 = JCR_NAMESPACE + "test2/" + JCR_CONTENT;

    private static final Logger log = getLogger(NamespaceConverterTest.class);

    @Before
    public void setUp() {
        testTranslator = new NamespaceConverter();
    }

    @Test
    public void testRoundTrip() {
        final String result = testTranslator.convert(testId1);
        log.debug("Received translated identifier: {}", result);
        assertEquals("Didn't get our original identifier back!", testId1, testTranslator.reverse().convert(result));
    }

    @Test
    public void testRoundTrip2() {
        final String result = testTranslator.convert(testId2);
        log.debug("Received translated identifier: {}", result);
        assertEquals("Didn't get our original identifier back!", testId2, testTranslator.reverse().convert(result));
    }
}
