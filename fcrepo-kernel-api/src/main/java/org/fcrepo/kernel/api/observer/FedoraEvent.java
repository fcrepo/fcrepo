/*
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.api.observer;

import java.util.Map;
import java.util.Set;

/**
 * A very simple abstraction to support downstream event-related machinery.
 *
 * @author ajs6f
 * @author acoburn
 * @since Feb 19, 2013
 */
public interface FedoraEvent {

    /**
     * @return the event types associated with this event.
     */
    Set<EventType> getTypes();

    /**
     * @param type the type
     * @return this object for continued use
     */
    FedoraEvent addType(final EventType type);

    /**
     * @return the path to the {@link org.fcrepo.kernel.api.models.FedoraResource}
     */
    String getPath();

    /**
     * @return the user ID associated with this event.
     */
    String getUserID();

    /**
     * @return the user data associated with this event.
     */
    String getUserData();

    /**
     * @return the date of this event.
     */
    long getDate();

    /**
     * Get the event ID.
     * @return Event identifier to use for building event URIs (e.g., in an external triplestore).
    **/
    String getEventID();

    /**
     * Get auxiliary information about the event
     * @return Event information as a Map
     */
    Map<String, String> getInfo();
}
