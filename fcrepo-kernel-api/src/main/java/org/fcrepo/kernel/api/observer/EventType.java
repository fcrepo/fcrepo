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

import static org.fcrepo.kernel.api.RdfLexicon.ACTIVITY_STREAMS_NAMESPACE;

/**
 * A collection of repository event types
 *
 * @author ajs6f
 * @since Feb 7, 2013
 */
public enum EventType {

    RESOURCE_CREATION("create resource", "Create"),
    RESOURCE_DELETION("delete resource", "Delete"),
    RESOURCE_MODIFICATION("update resource", "Update"),
    RESOURCE_RELOCATION("move resource", "Move"),
    INBOUND_REFERENCE("refer to resource", "Follow"),
    RESOURCE_PURGE("remove resource tombstone", "Purge");

    private final String eventName;
    private final String eventType;

    EventType(final String eventName, final String eventType) {
        this.eventName = eventName;
        this.eventType = eventType;
    }

    /**
     * @return a human-readable name for this event
     */
    public String getName() {
        return this.eventName;
    }

    /**
     * @return  type for this event without the namespace.
     */
    public String getTypeAbbreviated() {
        return eventType;
    }

    /**
     * @return an rdf type for this event
     */
    public String getType() {
        return ACTIVITY_STREAMS_NAMESPACE + eventType;
    }

}
