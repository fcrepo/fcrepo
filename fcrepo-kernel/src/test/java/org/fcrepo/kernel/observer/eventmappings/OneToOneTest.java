
package org.fcrepo.kernel.observer.eventmappings;

import static com.google.common.collect.Iterators.size;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.observation.Event;

import org.fcrepo.kernel.utils.iterators.EventIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OneToOneTest {

    final private OneToOne testMapping = new OneToOne();

    @Mock
    private Event mockEvent1, mockEvent2, mockEvent3;

    @Mock
    private javax.jcr.observation.EventIterator mockIterator;

    private EventIterator testInput;

    @Before
    public void setUp() {
        initMocks(this);
        when(mockIterator.next()).thenReturn(mockEvent1, mockEvent2, mockEvent3);
        when(mockIterator.hasNext()).thenReturn(true, true, true, false);
        testInput = new EventIterator(mockIterator);
    }

    @Test
    public void testCardinality() {
        assertEquals("Didn't get a FedoraEvent for every input JCR Event!", 3, size(testMapping
                .apply(testInput)));
    }

}
