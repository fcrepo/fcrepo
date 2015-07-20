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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author cabeer
 */
@RunWith(Parameterized.class)
public class HashConverterTest {

    @Parameterized.Parameter(value = 0)
    public String internalId;

    @Parameterized.Parameter(value = 1)
    public String externalId;

    private HashConverter testObj;

    @Parameterized.Parameters
    public static Iterable<String[]> data() {
        return Arrays.asList(new String[][]{
                { "/some/path", "/some/path" },
                { "/some/path/#/with-a-hash-uri", "/some/path#with-a-hash-uri" },
                { "/some/path/#/with%2Fa%2Fhash%2Furi", "/some/path#with/a/hash/uri" }
        });
    }

    @Before
    public void setUp() {
        testObj = new HashConverter();
    }

    @Test
    public void testDoForward() {
        assertEquals(internalId, testObj.convert(externalId));
    }

    @Test
    public void testDoBackwards() {
        assertEquals(externalId, testObj.reverse().convert(internalId));
    }

}
