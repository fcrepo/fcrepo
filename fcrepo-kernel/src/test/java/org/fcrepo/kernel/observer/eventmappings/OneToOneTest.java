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
