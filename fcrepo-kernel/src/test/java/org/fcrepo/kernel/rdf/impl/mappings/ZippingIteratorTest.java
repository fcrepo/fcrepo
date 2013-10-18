/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.rdf.impl.mappings;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class ZippingIteratorTest {

    private ZippingIterator<String, String> zip;

    private static final String from = "from";

    private static final String to = "to";

    /*
     * We test to see that a ZippingIterator will return results until one or
     * the other source iterator is exhausted.
     */

    @Test
    public void testMoreValuesThanFunctions() {
        final Iterator<String> values =
            Iterators.forArray(new String[] {from, from});
        final Iterator<Function<String, String>> functions =
            ImmutableList.of(f).iterator();
        zip = new ZippingIterator<String, String>(values, functions);
        while (zip.hasNext()) {
            assertEquals("Got wrong value!", to, zip.next());
        }

    }

    @Test
    public void testMoreFunctionsThanValues() {
        final Iterator<String> values = Iterators.forArray(new String[] {from});
        final Iterator<Function<String, String>> functions =
            ImmutableList.of(f, f).iterator();
        zip = new ZippingIterator<String, String>(values, functions);
        while (zip.hasNext()) {
            assertEquals("Got wrong value!", to, zip.next());
        }

    }

    private static final Function<String, String> f =
        new Function<String, String>() {

            @Override
            public String apply(final String input) {
                if (input == from) {
                    return to;
                } else {
                    throw new AssertionError("Received 'impossible' input!");
                }
            }

        };

}
