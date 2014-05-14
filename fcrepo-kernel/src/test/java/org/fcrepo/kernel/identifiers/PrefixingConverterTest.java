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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * <p>PrefixingConverterTest class.</p>
 *
 * @author ajs6f
 */
public class PrefixingConverterTest {

    private static final String testPrefix = "info:";

    private static final String testId = "test1";

    private PrefixingConverter testTranslator = new PrefixingConverter();

    @Test
    public void testRoundTrip() {
        testTranslator.setPrefix(testPrefix);
        assertEquals("Didn't recover our original identifier!", testId, testTranslator.reverse().convert(
                testTranslator.convert(testId)));
    }

}
