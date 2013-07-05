
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class NodeIteratortest {

    @Mock
    javax.jcr.NodeIterator i;

    @Mock
    Node node1, node2;

    NodeIterator testIterator;

    @Before
    public void setUp() {
        initMocks(this);
        testIterator = new NodeIterator(i);
    }

    @Test
    public void testHasNext() {
        when(i.hasNext()).thenReturn(true, false);
        assertTrue(testIterator.hasNext());
        assertFalse(testIterator.hasNext());
    }

    @Test
    public void testNext() {
        when(i.nextNode()).thenReturn(node1, node2);
        assertEquals(node1, testIterator.next());
        assertEquals(node2, testIterator.next());
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
