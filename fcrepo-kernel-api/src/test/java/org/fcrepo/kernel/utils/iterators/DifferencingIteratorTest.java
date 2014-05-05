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
package org.fcrepo.kernel.utils.iterators;

import static com.google.common.collect.Iterators.forArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class DifferencingIteratorTest {

    private static final String onlyInCollection = "only in collection";

    private static final String commonValue = "common";

    private static final String commonValue2 = "common2";

    private static final String onlyInIteratorValue = "only in iterator";

    private DifferencingIterator<String> testIterator;

    private Set<String> toBeCompared = ImmutableSet.of(onlyInCollection,
            commonValue);

    private Set<String> toBeCompared2 = ImmutableSet.of(onlyInCollection,
            commonValue, commonValue2);

    @Test
    public void testDifferencing() {

        final Iterator<String> i =
            forArray(new String[] {onlyInIteratorValue, commonValue});
        testIterator = new DifferencingIterator<>(toBeCompared, i);

        assertTrue("Didn't see a first value!", testIterator.hasNext());
        assertEquals("Retrieved final results too early!", null, testIterator
                .notCommon());
        assertEquals("Retrieved final results too early!", null, testIterator
                .common());
        assertEquals("Didn't get the only-in-iterator value!",
                onlyInIteratorValue, testIterator.next());
        assertFalse("Shouldn't see any more values!", testIterator.hasNext());
        assertTrue("Didn't find the common value in correct final result!",
                testIterator.common().contains(commonValue));
        assertFalse("Found the common value in wrong final result!",
                testIterator.notCommon().contains(commonValue));
        assertTrue("Didn't find the not-common value in correct final result!",
                testIterator.notCommon().contains(onlyInCollection));
        assertFalse("Found the not-common value in wrong final result!",
                testIterator.common().contains(onlyInCollection));
    }

    @Test
    public void testDifferencingWithMoreCommonValues() {

        final Iterator<String> i =
            forArray(new String[] {onlyInIteratorValue, commonValue,
                    commonValue, commonValue2});
        testIterator = new DifferencingIterator<>(toBeCompared2, i);

        assertTrue("Didn't see a first value!", testIterator.hasNext());
        assertEquals("Retrieved final results too early!", null, testIterator
                .notCommon());
        assertEquals("Retrieved final results too early!", null, testIterator
                .common());
        assertEquals("Didn't get the only-in-iterator value!",
                onlyInIteratorValue, testIterator.next());
        assertFalse("Shouldn't see any more values!", testIterator.hasNext());
        assertTrue("Didn't find the common value in correct final result!",
                testIterator.common().contains(commonValue));
        assertTrue("Didn't find the second common value in correct final result!",
                testIterator.common().contains(commonValue2));
        assertFalse("Found the common value in wrong final result!",
                testIterator.notCommon().contains(commonValue));
        assertFalse("Found the second common value in wrong final result!",
                testIterator.notCommon().contains(commonValue2));
        assertTrue("Didn't find the not-common value in correct final result!",
                testIterator.notCommon().contains(onlyInCollection));
        assertFalse("Found the not-common value in wrong final result!",
                testIterator.common().contains(onlyInCollection));
    }

    @Test
    public void testDifferencingWithNoValues() {

        final Iterator<String> i = forArray(new String[] {});
        testIterator = new DifferencingIterator<>(toBeCompared, i);
        assertFalse("Found a value where there should be none!", testIterator
                .hasNext());
    }

}
