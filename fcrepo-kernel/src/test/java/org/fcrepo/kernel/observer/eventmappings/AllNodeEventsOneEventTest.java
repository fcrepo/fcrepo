
package org.fcrepo.kernel.observer.eventmappings;

import static com.google.common.collect.Iterators.size;
import static org.jgroups.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.utils.iterators.EventIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AllNodeEventsOneEventTest {

    private static final String TEST_IDENTIFIER1 = randomUUID().toString();

    private static final String TEST_PATH1 = "/test/node1";

    private static final String TEST_IDENTIFIER2 = TEST_IDENTIFIER1;

    private static final String TEST_PATH2 = TEST_PATH1 + "/property";

    private static final String TEST_IDENTIFIER3 = randomUUID().toString();

    private static final String TEST_PATH3 = "/test/node2";

    final private AllNodeEventsOneEvent testMapping = new AllNodeEventsOneEvent();

    @Mock
    private Event mockEvent1, mockEvent2, mockEvent3;

    @Mock
    private javax.jcr.observation.EventIterator mockIterator;

    private EventIterator testInput;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockEvent1.getIdentifier()).thenReturn(TEST_IDENTIFIER1);
        when(mockEvent1.getPath()).thenReturn(TEST_PATH1);
        when(mockEvent2.getIdentifier()).thenReturn(TEST_IDENTIFIER2);
        when(mockEvent2.getPath()).thenReturn(TEST_PATH2);
        when(mockEvent3.getIdentifier()).thenReturn(TEST_IDENTIFIER3);
        when(mockEvent3.getPath()).thenReturn(TEST_PATH3);
        when(mockIterator.next()).thenReturn(mockEvent1, mockEvent2, mockEvent3);
        when(mockIterator.hasNext()).thenReturn(true, true, true, false);
        testInput = new EventIterator(mockIterator);
    }

    @Test
    public void testCardinality() {
        assertEquals("Didn't get 2 FedoraEvents for 3 input JCR Events, two of which were on the same node!", 2,
                size(testMapping.apply(testInput)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadOPeration() {
        testMapping.apply(testInput).remove();
    }

    @Test(expected = RuntimeException.class)
    public void testBadEvent() throws RepositoryException {
        reset(mockEvent1);
        when(mockEvent1.getIdentifier()).thenThrow(new RepositoryException("Expected."));
        testMapping.apply(testInput);
    }
}
