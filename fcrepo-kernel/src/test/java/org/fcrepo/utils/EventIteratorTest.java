
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class EventIteratorTest {

    @Mock
    javax.jcr.observation.EventIterator i;

    @Mock
    Event event1, event2;

    EventIterator testIterator;

    @Before
    public void setUp() {
        initMocks(this);
        testIterator = new EventIterator(i);
    }

    @Test
    public void testHasNext() {
        when(i.hasNext()).thenReturn(true, false);
        assertTrue(testIterator.hasNext());
        assertFalse(testIterator.hasNext());
    }

    @Test
    public void testNext() {
        when(i.nextEvent()).thenReturn(event1, event2);
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
