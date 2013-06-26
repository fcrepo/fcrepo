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
