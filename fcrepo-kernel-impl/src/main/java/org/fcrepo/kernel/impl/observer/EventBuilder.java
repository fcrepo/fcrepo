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
