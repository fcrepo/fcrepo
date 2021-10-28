/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.observer;

import org.fcrepo.kernel.api.observer.Event;

import java.util.Set;

/**
 * Stores details about an Event.
 *
 * @author pwinckles
 */
public interface EventBuilder {

    /**
     * Merges another EventBuilder into this EventBuilder. This should be used to combine multiple events on the same
     * resource.
     *
     * @param other another EventBuilder
     * @return this builder
     */
    EventBuilder merge(EventBuilder other);

    /**
     * Sets the resource's RDF Types on the event
     *
     * @param resourceTypes RDF Types
     * @return this builder
     */
    EventBuilder withResourceTypes(Set<String> resourceTypes);

    /**
     * Sets the baseUrl of the requests
     *
     * @param baseUrl the base url
     * @return this builder
     */
    EventBuilder withBaseUrl(String baseUrl);

    /**
     * Sets the user's user-agent
     *
     * @param userAgent the user's user-agent
     * @return this builder
     */
    EventBuilder withUserAgent(String userAgent);

    /**
     * Constructs a new Event object from the details in the builder.
     *
     * @return new event
     */
    Event build();

}
