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

/**
 * A convenient abstraction over JCR's integer-typed events.
 *
 * @author ajs6f
 * @date Feb 7, 2013
 */
public enum EventType {
    NODE_ADDED, NODE_REMOVED, PROPERTY_ADDED, PROPERTY_REMOVED,
    PROPERTY_CHANGED, NODE_MOVED, PERSIST;

    /**
     * @todo Add Documentation.
     */
    public static EventType getEventType(final Integer i) {
        switch (i) {
            case 0x1:
                return NODE_ADDED;
            case 0x2:
                return NODE_REMOVED;
            case 0x4:
                return PROPERTY_ADDED;
            case 0x8:
                return PROPERTY_REMOVED;
            case 0x10:
                return PROPERTY_CHANGED;
            case 0x20:
                return NODE_MOVED;
            case 0x40:
                return PERSIST;
                // no default
            default:
                throw new IllegalArgumentException("Invalid JCR event type: " +
                        i);
        }
    }

    /**
     * @param jcrEvent
     * @return A human-readable name for the type of this JCR event.
     */
    public static String getEventName(final Integer jcrEvent) {

        switch (getEventType(jcrEvent)) {
            case NODE_ADDED:
                return "node added";
            case NODE_REMOVED:
                return "node removed";
            case PROPERTY_ADDED:
                return "property added";
            case PROPERTY_CHANGED:
                return "property changed";
            case PROPERTY_REMOVED:
                return "property removed";
            case NODE_MOVED:
                return "node moved";
            case PERSIST:
                return "persist";
                // no default
            default:
                throw new IllegalArgumentException("Invalid JCR event type: " +
                        jcrEvent);
        }
    }
}
