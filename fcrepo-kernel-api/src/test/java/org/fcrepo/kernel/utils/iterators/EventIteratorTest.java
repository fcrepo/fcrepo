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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.observation.Event;

import org.fcrepo.kernel.utils.iterators.EventIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * @author ajs6f
 * @since 2013
 */
public class EventIteratorTest {

    @Mock
    javax.jcr.observation.EventIterator i;

    @Mock
    Event event1, event2;

    @InjectMocks
    EventIterator testIterator;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testHasNext() {
        when(i.hasNext()).thenReturn(true, false);
        assertTrue(testIterator.hasNext());
        assertFalse(testIterator.hasNext());
    }

    @Test
    public void testNext() {
        when(i.next()).thenReturn(event1, event2);
        assertEquals(event1, testIterator.next());
        assertEquals(event2, testIterator.next());
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
