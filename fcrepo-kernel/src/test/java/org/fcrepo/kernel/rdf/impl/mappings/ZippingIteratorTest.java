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

import static com.google.common.base.Functions.forMap;
import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.singletonIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;
import org.junit.Test;

import com.google.common.base.Function;

/**
 * @author ajs6f
 * @date Oct 2013
 */
public class ZippingIteratorTest {

    private ZippingIterator<Object, Object> zip;

    /*
     * We test to see that a ZippingIterator will return correct results until
     * one or the other source iterator is exhausted.
     */

    @Test
    public void testMoreValuesThanFunctions() {
        values = forArray(from1, from2);
        functions = singletonIterator(f);
        zip = new ZippingIterator<>(values, functions);
        assertEquals("Got wrong value!", to1, zip.next());
        assertFalse("Too many values!", zip.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMoreFunctionsThanValues() {
        values = singletonIterator(from1);
        functions = forArray(f, f);
        zip = new ZippingIterator<>(values, functions);
        assertEquals("Got wrong value!", to1, zip.next());
        assertFalse("Too many values!", zip.hasNext());
    }

    Iterator<Object> values;

    Iterator<Function<Object, Object>> functions;

    private static Object from1 = new Object();

    private static Object from2 = new Object();

    private static Object to1 = new Object();

    private static Object to2 = new Object();

    private static final Function<Object, Object> f = forMap(of(from1, to1,
            from2, to2));

}
