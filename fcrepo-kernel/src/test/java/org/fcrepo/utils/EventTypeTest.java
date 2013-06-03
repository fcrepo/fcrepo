/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;

import javax.jcr.observation.Event;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 14, 2013
 */
public class EventTypeTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetEventType() throws Exception {
        // assertEquals(javax.jcr.observation.Event.NODE_ADDED,
        // EventType.getEventType(0x1));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetEventName() throws Exception {
        assertEquals("node added", EventType.getEventName(Event.NODE_ADDED));
        assertEquals("node removed", EventType.getEventName(Event.NODE_REMOVED));
        assertEquals("property added", EventType
                .getEventName(Event.PROPERTY_ADDED));
        assertEquals("property removed", EventType
                .getEventName(Event.PROPERTY_REMOVED));
        assertEquals("node moved", EventType.getEventName(Event.NODE_MOVED));
        assertEquals("persist", EventType.getEventName(Event.PERSIST));
    }
}
