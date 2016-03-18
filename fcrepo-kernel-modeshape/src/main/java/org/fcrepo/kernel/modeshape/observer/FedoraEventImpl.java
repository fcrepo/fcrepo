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

package org.fcrepo.kernel.modeshape.observer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.fcrepo.kernel.api.observer.EventType.NODE_ADDED;
import static org.fcrepo.kernel.api.observer.EventType.NODE_MOVED;
import static org.fcrepo.kernel.api.observer.EventType.NODE_REMOVED;
import static org.fcrepo.kernel.api.observer.EventType.PERSIST;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_ADDED;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_CHANGED;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_REMOVED;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.empty;
import java.util.Collection;
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
import org.fcrepo.kernel.api.services.functions.HierarchicalIdentifierSupplier;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;
import org.fcrepo.kernel.modeshape.identifiers.HashConverter;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * A very simple abstraction to prevent event-driven machinery downstream from the repository from relying directly on
 * a JCR interface {@link Event}. Can represent either a single JCR event or several.
 *
 * @author ajs6f
 * @since Feb 19, 2013
 */
public class FedoraEventImpl implements FedoraEvent {

    private final String path;

    private final String userID;

    private final String userData;

    private final long date;

    private final Map<String, String> info;

    private final String eventID;

    private final Set<EventType> eventTypes = new HashSet<>();

    private final Set<Resource> resourceTypes = new HashSet<>();

    private static final List<Integer> PROPERTY_TYPES = asList(Event.PROPERTY_ADDED,
            Event.PROPERTY_CHANGED, Event.PROPERTY_REMOVED);

    private static final UniqueValueSupplier pidMinter = new DefaultPathMinter();

    /**
     * Create a new FedoraEvent
     *
     * @param type the Fedora EventType
     * @param path the node path corresponding to this event
     * @param userID the acting user for this event
     * @param userData any user data for this event
     * @param date the timestamp for this event
     * @param info supplementary information
     */
    public FedoraEventImpl(final EventType type, final String path, final String userID,
            final String userData, final long date, final Map<String, String> info) {
        this(singleton(type), path, userID, userData, date, info);
    }

    /**
     * Create a new FedoraEvent
     *
     * @param types a collection of Fedora EventTypes
     * @param path the node path corresponding to this event
     * @param userID the acting user for this event
     * @param userData any user data for this event
     * @param date the timestamp for this event
     * @param info supplementary information
     */
    public FedoraEventImpl(final Collection<EventType> types, final String path, final String userID,
            final String userData, final long date, final Map<String, String> info) {
        requireNonNull(types, "FedoraEvent requires a non-null event type");
        requireNonNull(path, "FedoraEvent requires a non-null path");

        this.eventTypes.addAll(types);
        this.path = path;
        this.userID = userID;
        this.userData = userData;
        this.date = date;
        this.info = isNull(info) ? emptyMap() : info;
        this.eventID = pidMinter.get();
    }

    @Override
    public Set<EventType> getTypes() {
        return eventTypes;
    }

    /**
     * @param type the type
     * @return this object for continued use
     */
    @Override
    public FedoraEvent addType(final EventType type) {
        eventTypes.add(type);
        return this;
    }

    @Override
    public Set<Resource> getResourceTypes() {
        return resourceTypes;
    }

    @Override
    public FedoraEvent addResourceType(final Resource type) {
        resourceTypes.add(type);
        return this;
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
     * @return the user data of the underlying JCR {@link Event}s
     */
    @Override
    public String getUserData() {
        return userData;
    }

    /**
     * @return the date of the FedoraEvent
     */
    @Override
    public long getDate() {
        return date;
    }

    /**
     * Get the event ID.
     *
     * @return Event identifier to use for building event URIs (e.g., in an external triplestore).
     **/
    @Override
    public String getEventID() {
        return eventID;
    }

    /**
     * Return a Map with any additional information about the event.
     *
     * @return a Map of additional information.
     */
    @Override
    public Map<String, String> getInfo() {
        return info;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("Event types:", getTypes().stream().map(EventType::getName).collect(joining(", ")))
                .add("Path:", getPath())
                .add("Date: ", getDate()).toString();
    }

    private static final Map<Integer, EventType> translation = ImmutableMap.<Integer, EventType>builder()
            .put(Event.NODE_ADDED, NODE_ADDED)
            .put(Event.NODE_REMOVED, NODE_REMOVED)
            .put(Event.PROPERTY_ADDED, PROPERTY_ADDED)
            .put(Event.PROPERTY_REMOVED, PROPERTY_REMOVED)
            .put(Event.PROPERTY_CHANGED, PROPERTY_CHANGED)
            .put(Event.NODE_MOVED, NODE_MOVED)
            .put(Event.PERSIST, PERSIST).build();

    /**
     * Get the Fedora event type for a JCR type
     *
     * @param i the integer value of a JCR type
     * @return EventType
     */
    public static EventType valueOf(final Integer i) {
        final EventType type = translation.get(i);
        if (type == null) {
            throw new IllegalArgumentException("Invalid event type: " + i);
        }
        return type;
    }

    /**
     * Convert a JCR Event to a FedoraEvent
     *
     * @param event the JCR Event
     * @return a FedoraEvent
     */
    public static FedoraEvent from(final Event event) {
        requireNonNull(event);
        try {
            final Map<String, String> info = event.getInfo();
            final FedoraEventImpl fedoraEvent = new FedoraEventImpl(valueOf(event.getType()), cleanPath(event),
                    event.getUserID(), event.getUserData(), event.getDate(), info);
            getResourceTypes(event).map(ResourceFactory::createResource).forEach(fedoraEvent::addResourceType);
            return fedoraEvent;
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException("Error converting JCR Event to FedoraEvent", ex);
        }
    }

    /**
     * @param event
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
     * The JCR-based Event::getPath contains some Modeshape artifacts that must be removed or modified in order to
     * correspond to the public resource path. For example, JCR Events will contain a trailing /jcr:content for
     * Binaries, a trailing /propName for properties, and /#/ notation for URI fragments.
     */
    private static String cleanPath(final Event event) throws RepositoryException {
        // remove any trailing data for property changes
        final String path = PROPERTY_TYPES.contains(event.getType()) ? event.getPath().substring(0, event.getPath()
                .lastIndexOf("/")) : event.getPath();

        // reformat any hash URIs and remove any trailing /jcr:content
        final HashConverter converter = new HashConverter();
        return converter.reverse().convert(path.replaceAll("/" + JCR_CONTENT, ""));
    }

    private static class DefaultPathMinter implements HierarchicalIdentifierSupplier {
    }
}
