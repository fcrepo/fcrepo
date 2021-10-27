/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
