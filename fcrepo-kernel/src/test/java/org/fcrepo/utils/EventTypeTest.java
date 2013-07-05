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

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PERSIST;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.utils.EventType.getEventName;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventTypeTest {

    @Test
    public void testGetEventName() throws Exception {
        assertEquals("node added", getEventName(NODE_ADDED));
        assertEquals("node removed", getEventName(NODE_REMOVED));
        assertEquals("property added", getEventName(PROPERTY_ADDED));
        assertEquals("property removed", getEventName(PROPERTY_REMOVED));
        assertEquals("node moved", getEventName(NODE_MOVED));
        assertEquals("persist", getEventName(PERSIST));
    }
}
