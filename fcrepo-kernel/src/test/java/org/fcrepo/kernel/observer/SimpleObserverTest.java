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

package org.fcrepo.kernel.observer;

import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.observer.SimpleObserver.EVENT_TYPES;
import static javax.jcr.observation.Event.NODE_ADDED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.lang.reflect.Field;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.modeshape.jcr.api.Repository;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({com.google.common.collect.Iterables.class})
public class SimpleObserverTest {

    private SimpleObserver testObj;

    @Mock
    ObservationManager mockOM;

    @Mock
    Repository mockRepository;

    @Mock
    Session mockSession;

    @Mock
    Workspace mockWS;

    @Mock
    EventBus mockBus;

    @Mock
    EventFilter mockFilterFactory;

    @Mock
    Function<Event, Event> mockFilter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mockWS.getObservationManager()).thenReturn(mockOM);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockRepository.login()).thenReturn(mockSession);
        when(mockFilterFactory.getFilter(mockSession)).thenReturn(mockFilter);
        testObj = new SimpleObserver();
        setField("repository", testObj, mockRepository);
    }

    @Test
    public void testBuildListener() throws Exception {
        testObj.buildListener();
        verify(mockOM).addEventListener(testObj, EVENT_TYPES, "/", true, null,
                null, false);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent() throws Exception {
        setField("eventBus", testObj, mockBus);
        setField("eventFilter", testObj, mockFilterFactory);
        setField("session", testObj, mockSession);
        final Event mockEvent = mock(Event.class);
        when(mockEvent.getPath()).thenReturn("/foo/bar");
        when(mockEvent.getType()).thenReturn(NODE_ADDED);
        final Node mockNode = mock(Node.class);
        when(mockNode.isNode()).thenReturn(true);
        when(mockSession.getNode(any(String.class))).thenReturn(mockNode);
        final EventIterator mockEvents = mock(EventIterator.class);
        final List<Event> iterable = asList(new Event[] {mockEvent});
        mockStatic(Iterables.class);
        when(transform(any(Iterable.class), eq(mockFilter))).thenReturn(iterable);
        testObj.onEvent(mockEvents);
        verify(mockBus).post(any(Event.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnEventAllFiltered() throws Exception {
        setField("eventBus", testObj, mockBus);
        setField("eventFilter", testObj, mockFilterFactory);
        final EventIterator mockEvents = mock(EventIterator.class);
        final List<Event> iterable = asList(new Event[0]);
        mockStatic(Iterables.class);
        when(transform(any(Iterable.class), eq(mockFilter))).thenReturn(iterable);
        testObj.onEvent(mockEvents);
        verify(mockBus, never()).post(any(Event.class));
    }

    private static void setField(final String name, final SimpleObserver obj,
            final Object val) throws Exception {
        final Field field = SimpleObserver.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, val);
    }

}
