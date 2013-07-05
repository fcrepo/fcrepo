
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Property;

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
