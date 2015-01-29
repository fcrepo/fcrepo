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
package org.fcrepo.http.commons.domain;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}