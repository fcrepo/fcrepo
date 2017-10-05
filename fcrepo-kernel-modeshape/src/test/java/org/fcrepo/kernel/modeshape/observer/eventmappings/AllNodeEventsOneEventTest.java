/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.observer.eventmappings;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.observer.FedoraEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <p>AllNodeEventsOneEventTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class AllNodeEventsOneEventTest {


    private static final String TEST_PATH1 = "/test/node1";

    private static final String TEST_PATH2 = TEST_PATH1 + "/dc:title";

    private static final String TEST_NODE_PATH3 = "/test/node2";
    private static final String TEST_PATH3 = TEST_NODE_PATH3 + "/dc:description";

    private static final String TEST_PATH4 = "/test/node3";

    private static final String TEST_PATH5 = "/test/node3/" + JCR_CONTENT;

    private final AllNodeEventsOneEvent testMapping = new AllNodeEventsOneEvent();

    @Mock
    private Event mockEvent1;

    @Mock
    private Event mockEvent2;

    @Mock
    private Event mockEvent3;

    @Mock
    private Event mockEvent4;

    @Mock
    private Event mockEvent5;

    private Stream<Event> mockStream;

    private Stream<Event> mockStream2;

    private Stream<Event> mockStream3;

    @Before
    public void setUp() throws RepositoryException {
        when(mockEvent1.getPath()).thenReturn(TEST_PATH1);
        when(mockEvent1.getType()).thenReturn(NODE_ADDED);
        when(mockEvent1.getDate()).thenReturn(1L);
        when(mockEvent2.getPath()).thenReturn(TEST_PATH2);
        when(mockEvent2.getType()).thenReturn(PROPERTY_ADDED);
        when(mockEvent1.getDate()).thenReturn(2L);
        when(mockEvent3.getPath()).thenReturn(TEST_PATH3);
        when(mockEvent3.getType()).thenReturn(PROPERTY_CHANGED);
        when(mockEvent1.getDate()).thenReturn(3L);
        mockStream = of(mockEvent1, mockEvent2, mockEvent3);

        when(mockEvent4.getPath()).thenReturn(TEST_PATH4);
        when(mockEvent4.getType()).thenReturn(NODE_ADDED);
        when(mockEvent1.getDate()).thenReturn(4L);
        when(mockEvent5.getPath()).thenReturn(TEST_PATH5);
        when(mockEvent5.getType()).thenReturn(NODE_ADDED);
        when(mockEvent1.getDate()).thenReturn(5L);
        mockStream2 = of(mockEvent4, mockEvent5);

        mockStream3 = of(mockEvent4, mockEvent5);
    }

    @Test
    public void testCardinality() {
        assertEquals("Didn't get 2 FedoraEvents for 3 input JCR Events, two of which were on the same node!", 2,
                testMapping.apply(mockStream).count());
    }

    @Test
    public void testCollapseContentEvents() {
        assertEquals("Didn't collapse content node and fcr:content events!", 1, testMapping.apply(mockStream2).count());
    }

    @Test(expected = RuntimeException.class)
    public void testBadEvent() throws RepositoryException {
        reset(mockEvent1);
        when(mockEvent1.getPath()).thenThrow(new RepositoryException("Expected."));
        testMapping.apply(mockStream);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testError() throws RepositoryException {
        when(mockEvent3.getPath()).thenThrow(new RepositoryException("expected"));

        final Stream<FedoraEvent> stream = testMapping.apply(mockStream);
        assertNotNull(stream);
        stream.collect(toList());
    }
}
