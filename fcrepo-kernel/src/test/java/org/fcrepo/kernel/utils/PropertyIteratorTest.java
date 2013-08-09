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

package org.fcrepo.kernel.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Property;

import org.fcrepo.kernel.utils.PropertyIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PropertyIteratorTest {

    @Mock
    javax.jcr.PropertyIterator i;

    @Mock
    Property property1, property2;

    PropertyIterator testIterator;

    @Before
    public void setUp() {
        initMocks(this);
        testIterator = new PropertyIterator(i);
    }

    @Test
    public void testHasNext() {
        when(i.hasNext()).thenReturn(true, false);
        assertTrue(testIterator.hasNext());
        assertFalse(testIterator.hasNext());
    }

    @Test
    public void testNext() {
        when(i.nextProperty()).thenReturn(property1, property2);
        assertEquals(property1, testIterator.next());
        assertEquals(property2, testIterator.next());
    }

    @Test
    public void testRemove() {
        testIterator.remove();
        verify(i).remove();
    }

    @Test
    public void testIterator() {
        assertEquals(testIterator, testIterator.iterator());
    }

}
