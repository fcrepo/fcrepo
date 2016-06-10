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

import static org.fcrepo.kernel.api.RdfLexicon.EVENT_NAMESPACE;

/**
 * A collection of repository event types
 *
 * @author ajs6f
 * @since Feb 7, 2013
 */
public enum EventType {

    RESOURCE_CREATION("resource creation", "ResourceCreation"),
    RESOURCE_DELETION("resource deletion", "ResourceDeletion"),
    RESOURCE_MODIFICATION("resource modification", "ResourceModification"),
    RESOURCE_RELOCATION("resource relocation", "ResourceRelocation");

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
     * @return an rdf type for this event
     */
    public String getType() {
        return EVENT_NAMESPACE + eventType;
    }
}
