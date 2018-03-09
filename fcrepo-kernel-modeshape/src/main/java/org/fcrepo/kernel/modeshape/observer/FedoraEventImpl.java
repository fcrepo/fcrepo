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
package org.fcrepo.kernel.modeshape.observer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_CREATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_DELETION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_MODIFICATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_RELOCATION;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.fcrepo.kernel.api.observer.OptionalValues.USER_AGENT;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.empty;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.modeshape.identifiers.HashConverter;
import org.fcrepo.kernel.modeshape.utils.FedoraSessionUserUtil;

import org.slf4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

/**
 * A very simple abstraction to prevent event-driven machinery downstream from the repository from relying directly
 * on a JCR interface {@link Event}. Can represent either a single JCR event or several.
 *
 * @author ajs6f
 * @since Feb 19, 2013
 */
public class FedoraEventImpl implements FedoraEvent {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final static Logger LOGGER = getLogger(FedoraEventImpl.class);

    private final String path;
    private final String userID;
    private final URI userURI;
    private final Instant date;
    private final Map<String, String> info;
    private final String eventID;
    private final Set<String> eventResourceTypes;

    private final Set<EventType> eventTypes = new HashSet<>();

    private static final List<Integer> PROPERTY_TYPES = asList(Event.PROPERTY_ADDED,
            Event.PROPERTY_CHANGED, Event.PROPERTY_REMOVED);

    /**
     * Create a new FedoraEvent
     * @param type the Fedora EventType
     * @param path the node path corresponding to this event
     * @param resourceTypes the rdf types of the corresponding resource
     * @param userID the acting user for this event
     * @param date the timestamp for this event
     * @param info supplementary information
     */
    public FedoraEventImpl(final EventType type, final String path, final Set<String> resourceTypes,
            final String userID, final URI userURI, final Instant date, final Map<String, String> info) {
        this(singleton(type), path, resourceTypes, userID, userURI, date, info);
    }

   /**
     * Create a new FedoraEvent
     * @param types a collection of Fedora EventTypes
     * @param path the node path corresponding to this event
     * @param resourceTypes the rdf types of the corresponding resource
     * @param userID the acting user for this event
     * @param date the timestamp for this event
     * @param info supplementary information
     */
    public FedoraEventImpl(final Collection<EventType> types, final String path, final Set<String> resourceTypes,
            final String userID, final URI userURI, final Instant date, final Map<String, String> info) {
        requireNonNull(types, "FedoraEvent requires a non-null event type");
        requireNonNull(path, "FedoraEvent requires a non-null path");

        this.eventTypes.addAll(types);
        this.path = path;
        this.eventResourceTypes = resourceTypes;
        this.userID = userID;
        this.userURI = userURI;
        this.date = date;
        this.info = isNull(info) ? emptyMap() : info;
        this.eventID = "urn:uuid:" + randomUUID().toString();
    }


    /**
     * @return the event types of the underlying JCR {@link Event}s
     */
    @Override
    public Set<EventType> getTypes() {
        return eventTypes;
    }

    /**
     * @return the RDF types of the underlying Fedora Resource
    **/
    @Override
    public Set<String> getResourceTypes() {
        return eventResourceTypes;
    }

    /**
     * @return the path of the underlying JCR {@link Event}s
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * @return the user ID of the underlying JCR {@link Event}s
     */
    @Override
    public String getUserID() {
        return userID;
    }

    /**
     * @return the user URI of the underlying JCR {@link Event}s
     */
    @Override
    public URI getUserURI() {
        return userURI;
    }

    /**
     * @return the date of the FedoraEvent
     */
    @Override
    public Instant getDate() {
        return date;
    }

    /**
     * Get the event ID.
     * @return Event identifier to use for building event URIs (e.g., in an external triplestore).
    **/
    @Override
    public String getEventID() {
        return eventID;
    }

    /**
     * Return a Map with any additional information about the event.
     * @return a Map of additional information.
     */
    @Override
    public Map<String, String> getInfo() {

        return info;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
            .add("Event types:", getTypes().stream()
                            .map(EventType::getName)
                            .collect(joining(", ")))
            .add("Event resource types:", String.join(",", eventResourceTypes))
            .add("Path:", getPath())
            .add("Date: ", getDate()).toString();
    }

    private static final Map<Integer, EventType> translation = ImmutableMap.<Integer, EventType>builder()
            .put(NODE_ADDED, RESOURCE_CREATION)
            .put(NODE_REMOVED, RESOURCE_DELETION)
            .put(PROPERTY_ADDED, RESOURCE_MODIFICATION)
            .put(PROPERTY_REMOVED, RESOURCE_MODIFICATION)
            .put(PROPERTY_CHANGED, RESOURCE_MODIFICATION)
            .put(NODE_MOVED, RESOURCE_RELOCATION).build();

    /**
     * Get the Fedora event type for a JCR type
     *
     * @param i the integer value of a JCR type
     * @return EventType
     */
    public static EventType valueOf(final Integer i) {
        final EventType type = translation.get(i);
        if (isNull(type)) {
            throw new IllegalArgumentException("Invalid event type: " + i);
        }
        return type;
    }


    /**
     * Convert a JCR Event to a FedoraEvent
     * @param event the JCR Event
     * @return a FedoraEvent
     */
    public static FedoraEvent from(final Event event) {
        requireNonNull(event);
        try {
            @SuppressWarnings("unchecked")
            final Map<String, String> info = new HashMap<>(event.getInfo());

            final String userdata = event.getUserData();
            try {
                if (userdata != null && !userdata.isEmpty()) {
                    final JsonNode json = MAPPER.readTree(userdata);
                    if (json.has(BASE_URL)) {
                        String url = json.get(BASE_URL).asText();
                        while (url.endsWith("/")) {
                            url = url.substring(0, url.length() - 1);
                        }
                        info.put(BASE_URL, url);
                    }
                    if (json.has(USER_AGENT)) {
                        info.put(USER_AGENT, json.get(USER_AGENT).asText());
                    }
                } else {
                    LOGGER.debug("Event UserData is empty!");
                }
            } catch (final IOException ex) {
                LOGGER.warn("Error extracting user data: " + userdata, ex.getMessage());
            }

            final Set<String> resourceTypes = getResourceTypes(event).collect(toSet());

            return new FedoraEventImpl(valueOf(event.getType()), cleanPath(event), resourceTypes,
                    event.getUserID(), FedoraSessionUserUtil.getUserURI(event.getUserID()), ofEpochMilli(event
                            .getDate()), info);

        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException("Error converting JCR Event to FedoraEvent", ex);
        }
    }

    /**
     * Get the RDF Types of the resource corresponding to this JCR Event
     * @param event the JCR event
     * @return the types recorded on the resource associated to this event
     */
    public static Stream<String> getResourceTypes(final Event event) {
        if (event instanceof org.modeshape.jcr.api.observation.Event) {
            try {
                final org.modeshape.jcr.api.observation.Event modeEvent =
                        (org.modeshape.jcr.api.observation.Event) event;
                final Stream.Builder<NodeType> types = Stream.builder();
                for (final NodeType type : modeEvent.getMixinNodeTypes()) {
                    types.add(type);
                }
                types.add(modeEvent.getPrimaryNodeType());
                return types.build().map(NodeType::getName);
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
        return empty(); // wasn't a ModeShape event, so we have no access to resource types
    }

    /**
     * The JCR-based Event::getPath contains some Modeshape artifacts that must be removed or modified in
     * order to correspond to the public resource path. For example, JCR Events will contain a trailing
     * /jcr:content for Binaries, a trailing /propName for properties, and /#/ notation for URI fragments.
     */
    private static String cleanPath(final Event event) throws RepositoryException {
        // remove any trailing data for property changes
        final String path = PROPERTY_TYPES.contains(event.getType()) ?
            event.getPath().substring(0, event.getPath().lastIndexOf("/")) : event.getPath();

        // reformat any hash URIs and remove any trailing /jcr:content
        final HashConverter converter = new HashConverter();
        return converter.reverse().convert(path.replaceAll("/" + JCR_CONTENT, ""));
    }
}
