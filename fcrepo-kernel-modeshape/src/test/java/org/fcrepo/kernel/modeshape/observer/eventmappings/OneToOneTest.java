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

import static java.util.stream.Stream.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <p>OneToOneTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class OneToOneTest {

    final private OneToOne testMapping = new OneToOne();

    @Mock
    private Event mockEvent1;

    @Mock
    private Event mockEvent2;

    @Mock
    private Event mockEvent3;

    private Stream<Event> mockStream;

    @Before
    public void setUp() throws RepositoryException {
        mockStream = of(mockEvent1, mockEvent2, mockEvent3);
        when(mockEvent1.getPath()).thenReturn("/foo");
        when(mockEvent2.getPath()).thenReturn("/foo");
        when(mockEvent3.getPath()).thenReturn("/foo");
        when(mockEvent1.getType()).thenReturn(1);
        when(mockEvent2.getType()).thenReturn(1);
        when(mockEvent3.getType()).thenReturn(1);
        when(mockEvent1.getDate()).thenReturn(1L);
        when(mockEvent2.getDate()).thenReturn(1L);
        when(mockEvent3.getDate()).thenReturn(1L);
    }

    @Test
    public void testCardinality() {
        assertEquals("Didn't get a FedoraEvent for every input JCR Event!", 3, testMapping
                .apply(mockStream).count());
    }
}
