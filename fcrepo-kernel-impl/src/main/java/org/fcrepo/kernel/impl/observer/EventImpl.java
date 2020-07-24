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

package org.fcrepo.kernel.impl.observer;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.UUID.randomUUID;

/**
 * An event that describes one or more actions that a user preformed on a resource.
 *
 * @author pwinckles
 */
public class EventImpl implements Event {

    private final String eventId;
    private final FedoraId fedoraId;
    private final Set<EventType> types;
    private final Set<String> resourceTypes;
    private final String userID;
    private final URI userURI;
    private final String userAgent;
    private final String baseUrl;
    private final Instant date;

    /**
     * Create a new FedoraEvent
     *
     * @param fedoraId the FedoraId of the resource the event is on
     * @param types a collection of Fedora EventTypes
     * @param resourceTypes the rdf types of the corresponding resource
     * @param userID the acting user for this event
     * @param userURI the uri of the acting user for this event
     * @param userAgent the user-agent associated with the request
     * @param baseUrl the originating request's baseUrl
     * @param date the timestamp for this event
     */
    public EventImpl(final FedoraId fedoraId, final Set<EventType> types,
                     final Set<String> resourceTypes, final String userID,
                     final URI userURI, final String userAgent, final String baseUrl,
                     final Instant date) {
        this.eventId = "urn:uuid:" + randomUUID().toString();
        this.fedoraId = checkNotNull(fedoraId, "fedoraId cannot be null");
        this.types = Set.copyOf(checkNotNull(types, "types cannot be null"));
        this.resourceTypes = Set.copyOf(checkNotNull(resourceTypes, "resourceTypes cannot be null"));
        this.userID = userID;
        this.userURI = userURI;
        this.userAgent = userAgent;
        checkNotNull(baseUrl, "baseUrl cannot be null");
        // baseUrl is expected to not have a trailing slash
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.date = checkNotNull(date, "date cannot be null");
    }

    @Override
    public FedoraId getFedoraId() {
        return fedoraId;
    }

    @Override
    public Set<EventType> getTypes() {
        return types;
    }

    @Override
    public Set<String> getResourceTypes() {
        return resourceTypes;
    }

    @Override
    public String getPath() {
        return fedoraId.getFullIdPath();
    }

    @Override
    public String getUserID() {
        return userID;
    }

    @Override
    public Instant getDate() {
        return date;
    }

    @Override
    public String getEventID() {
        return eventId;
    }

    @Override
    public URI getUserURI() {
        return userURI;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String toString() {
        return "EventImpl{" +
                "eventId='" + eventId + '\'' +
                ", fedoraId=" + fedoraId +
                ", types=" + types +
                ", resourceTypes=" + resourceTypes +
                ", userID='" + userID + '\'' +
                ", userURI=" + userURI +
                ", userAgent=" + userAgent +
                ", baseUrl=" + baseUrl +
                ", date=" + date +
                '}';
    }

}
