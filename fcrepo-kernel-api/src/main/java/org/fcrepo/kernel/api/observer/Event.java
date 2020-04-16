/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

/**
 * A very simple abstraction to support downstream event-related machinery.
 *
 * @author ajs6f
 * @author acoburn
 * @since Feb 19, 2013
 */
public interface Event {

    /**
     * @return the FedoraId of the resource associated with this event.
     */
    FedoraId getFedoraId();

    /**
     * @return the event types associated with this event.
     */
    Set<EventType> getTypes();

    /**
     * @return the RDF Types of the resource associated with this event.
    **/
    Set<String> getResourceTypes();

    /**
     * @return the path to the {@link org.fcrepo.kernel.api.models.FedoraResource}
     */
    String getPath();

    /**
     * @return the user ID associated with this event.
     */
    String getUserID();

    /**
     * Get the user URI associated with this event.
     * @return user URI
     */
    URI getUserURI();

    /**
     * @return The user-agent associated to the request
     */
    String getUserAgent();

    /**
     * @return the date of this event.
     */
    Instant getDate();

    /**
     * Get the event ID.
     * @return Event identifier to use for building event URIs (e.g., in an external triplestore).
    **/
    String getEventID();

    /**
     * @return The originating request's baseUrl
     */
    String getBaseUrl();

}
