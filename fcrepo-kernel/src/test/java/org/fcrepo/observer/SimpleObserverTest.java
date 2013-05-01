package org.fcrepo.observer;

import static org.mockito.Mockito.*;

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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

@RunWith(PowerMockRunner.class)
@PrepareForTest({com.google.common.collect.Iterables.class})
public class SimpleObserverTest {

    private SimpleObserver testObj;
    
    ObservationManager mockOM;
    
    private static void setField(String name, SimpleObserver obj, Object val) throws Exception {
        Field field = SimpleObserver.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, val);
    }
    
    @Before
    public void setUp() throws Exception {
        testObj = new SimpleObserver();        
    }
    
    @Test
    public void testBuildListener() throws Exception {
        Repository mockRepository = mock(Repository.class);
        Session mockSession = mock(Session.class);
        Workspace mockWS = mock(Workspace.class);
        mockOM = mock(ObservationManager.class);
        when(mockWS.getObservationManager()).thenReturn(mockOM);
        when(mockSession.getWorkspace()).thenReturn(mockWS);
        when(mockRepository.login()).thenReturn(mockSession);
        setField("repository", testObj, mockRepository);
        testObj.buildListener();
        verify(mockOM).addEventListener(testObj, SimpleObserver.EVENT_TYPES , "/", true, null, null, false);
    }
    
    @Test
    public void testOnEvent() throws Exception {
        EventBus mockBus = mock(EventBus.class);
        EventFilter mockFilter = mock(EventFilter.class);
        setField("eventBus", testObj, mockBus);
        setField("eventFilter", testObj, mockFilter);
        Event mockEvent = mock(Event.class);
        EventIterator mockEvents = mock(EventIterator.class);
        List<Event> iterable = Arrays.asList(new Event[]{mockEvent});
        PowerMockito.mockStatic(Iterables.class);
        when(Iterables.filter(any(Iterable.class), eq(mockFilter))).thenReturn(iterable);
        testObj.onEvent(mockEvents);
        verify(mockBus).post(any(Event.class));
    }
    
    @Test
    public void testOnEventAllFiltered() throws Exception {
        EventBus mockBus = mock(EventBus.class);
        EventFilter mockFilter = mock(EventFilter.class);
        setField("eventBus", testObj, mockBus);
        setField("eventFilter", testObj, mockFilter);
        Event mockEvent = mock(Event.class);
        EventIterator mockEvents = mock(EventIterator.class);
        List<Event> iterable = Arrays.asList(new Event[0]);
        PowerMockito.mockStatic(Iterables.class);
        when(Iterables.filter(any(Iterable.class), eq(mockFilter))).thenReturn(iterable);
        testObj.onEvent(mockEvents);
        verify(mockBus, never()).post(any(Event.class));
    }

}
