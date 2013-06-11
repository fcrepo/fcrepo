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
        testObj.setEventBus(mockBus);
        UriInfo mockUris = mock(UriInfo.class);
        URI mockUri = new URI("http://localhost.info");
        when(mockUris.getBaseUri()).thenReturn(mockUri);
        testObj.setUriInfo(mockUris);
        testObj.initialize();
        testObj.getFeed();
    }
    
    @Test
    public void testInitialize() throws Exception {
        EventBus mockBus = mock(EventBus.class);
        testObj.setEventBus(mockBus);
        testObj.initialize();
        verify(mockBus).register(testObj);
    }
    
    @Test
    public void testNewEvent() {
        Event mockEvent = mock(Event.class);
        testObj.newEvent(mockEvent);
    }

}
