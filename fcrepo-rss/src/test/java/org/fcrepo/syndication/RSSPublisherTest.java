package org.fcrepo.syndication;

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.URI;

import javax.jcr.observation.Event;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

public class RSSPublisherTest {

    private RSSPublisher testObj;
    
    @Before
    public void setUp() {
        testObj = new RSSPublisher();
    }
    
    @Test
    public void testGetFeed() throws Exception {
        EventBus mockBus = mock(EventBus.class);
        setField("eventBus", testObj, mockBus);
        UriInfo mockUris = mock(UriInfo.class);
        URI mockUri = new URI("http://localhost.info");
        when(mockUris.getBaseUri()).thenReturn(mockUri);
        setField("uriInfo", AbstractResource.class,testObj, mockUris);
        testObj.initialize();
        testObj.getFeed();
    }
    
    @Test
    public void testInitialize() throws Exception {
        EventBus mockBus = mock(EventBus.class);
        setField("eventBus", testObj, mockBus);
        testObj.initialize();
        verify(mockBus).register(testObj);
    }
    
    @Test
    public void testNewEvent() {
        Event mockEvent = mock(Event.class);
        testObj.newEvent(mockEvent);
    }
    
    private static void setField(String name, Class<?> type, RSSPublisher obj, Object val)
    throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, val);
    }

    private static void setField(String name, RSSPublisher obj, Object val)
    throws Exception {
        setField(name, RSSPublisher.class, obj, val);
    }
}
