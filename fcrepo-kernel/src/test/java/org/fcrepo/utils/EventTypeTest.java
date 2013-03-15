package org.fcrepo.utils;

import org.junit.Test;

import javax.jcr.observation.Event;

import static org.junit.Assert.assertEquals;

public class EventTypeTest {
    @Test
    public void testGetEventType() throws Exception {
        //  assertEquals(javax.jcr.observation.Event.NODE_ADDED, EventType.getEventType(0x1));
    }

    @Test
    public void testGetEventName() throws Exception {
        assertEquals("node added", EventType.getEventName(Event.NODE_ADDED));
        assertEquals("node removed", EventType.getEventName(Event.NODE_REMOVED));
        assertEquals("property added", EventType.getEventName(Event.PROPERTY_ADDED));
        assertEquals("property removed", EventType.getEventName(Event.PROPERTY_REMOVED));
        assertEquals("node moved", EventType.getEventName(Event.NODE_MOVED));
        assertEquals("persist", EventType.getEventName(Event.PERSIST));
    }
}
