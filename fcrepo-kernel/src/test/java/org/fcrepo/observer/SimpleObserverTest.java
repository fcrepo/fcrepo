/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.observer;

import static com.google.common.collect.Iterables.filter;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.Repository;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

/**
 * @todo Add Documentation.
 * @author Edwin Shin
 * @date Feb 7, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({com.google.common.collect.Iterables.class})
public class SimpleObserverTest {

    private SimpleObserver testObj;

    ObservationManager mockOM;

    private static void setField(final String name, final SimpleObserver obj,
            final Object val) throws Exception {
        final Field field = SimpleObserver.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, val);
    }

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws Exception {
        testObj = new SimpleObserver();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testBuildListener() throws Exception {
        final Repository mockRepository = mock(Repository.class);
        final Session mockSession = mock(Session.class);
        final Workspace mockWS = mock(Workspace.class);
        mockOM = mock(ObservationManager.class);
        when(mockWS.getObservationManager()).thenReturn(mockOM);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockRepository.login()).thenReturn(mockSession);
        setField("repository", testObj, mockRepository);
        testObj.buildListener();
        verify(mockOM).addEventListener(testObj, SimpleObserver.EVENT_TYPES,
                "/", true, null, null, false);
    }

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOnEvent() throws Exception {
        final EventBus mockBus = mock(EventBus.class);
        final EventFilter mockFilter = mock(EventFilter.class);
        setField("eventBus", testObj, mockBus);
        setField("eventFilter", testObj, mockFilter);
        final Event mockEvent = mock(Event.class);
        final EventIterator mockEvents = mock(EventIterator.class);
        final List<Event> iterable = Arrays.asList(new Event[] {mockEvent});
        mockStatic(Iterables.class);
        when(filter(any(Iterable.class), eq(mockFilter))).thenReturn(iterable);
        testObj.onEvent(mockEvents);
        verify(mockBus).post(any(Event.class));
    }

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOnEventAllFiltered() throws Exception {
        final EventBus mockBus = mock(EventBus.class);
        final EventFilter mockFilter = mock(EventFilter.class);
        setField("eventBus", testObj, mockBus);
        setField("eventFilter", testObj, mockFilter);
        final EventIterator mockEvents = mock(EventIterator.class);
        final List<Event> iterable = Arrays.asList(new Event[0]);
        PowerMockito.mockStatic(Iterables.class);
        when(filter(any(Iterable.class), eq(mockFilter))).thenReturn(iterable);
        testObj.onEvent(mockEvents);
        verify(mockBus, never()).post(any(Event.class));
    }

}
