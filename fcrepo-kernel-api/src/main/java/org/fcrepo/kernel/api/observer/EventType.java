/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
