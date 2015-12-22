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

import static com.google.common.base.Functions.forMap;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.union;
import static org.fcrepo.kernel.api.observer.EventType.NODE_ADDED;
import static org.fcrepo.kernel.api.observer.EventType.NODE_MOVED;
import static org.fcrepo.kernel.api.observer.EventType.NODE_REMOVED;
import static org.fcrepo.kernel.api.observer.EventType.PERSIST;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_ADDED;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_CHANGED;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_REMOVED;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.api.services.functions.HierarchicalIdentifierSupplier;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;

import com.google.common.collect.ImmutableMap;

import org.fcrepo.kernel.api.observer.EventType;

/**
 * A very simple abstraction to prevent event-driven machinery downstream from the repository from relying directly
 * on a JCR interface {@link Event}. Can represent either a single JCR event or several.
 *
 * @author ajs6f
 * @since Feb 19, 2013
 */
public class FedoraEventImpl implements FedoraEvent {

    private final Event e;
    private final String eventID;

    private final Set<EventType> eventTypes = new HashSet<>();
    private final Set<String> eventProperties = new HashSet<>();

    private static final List<Integer> PROPERTY_TYPES = asList(javax.jcr.observation.Event.PROPERTY_ADDED,
            javax.jcr.observation.Event.PROPERTY_CHANGED,
            javax.jcr.observation.Event.PROPERTY_REMOVED);

    private static final UniqueValueSupplier pidMinter = new DefaultPathMinter();

    /**
     * Wrap a JCR Event with our FedoraEvent decorators
     *
     * @param e the JCR event
     */
    public FedoraEventImpl(final Event e) {
        checkArgument(e != null, "null cannot support a FedoraEvent!");
        eventID = pidMinter.get();
        this.e = e;
    }

    /**
     * Create a FedoraEvent from an existing FedoraEvent object
     * Note: Only the wrapped JCR event is passed on to the new object.
     *
     * @param e the given fedora event
     */
    public FedoraEventImpl(final FedoraEvent e) {
        checkArgument(e != null, "null cannot support a FedoraEvent!");
        eventID = e.getEventID();
        this.e = ((FedoraEventImpl)e).e;
    }

    /**
     * @return the event types of the underlying JCR {@link Event}s
     */
    @Override
    public Set<EventType> getTypes() {
        final EventType type = valueOf(e.getType());
        return eventTypes != null ? union(singleton(type), eventTypes) : singleton(type);
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

    /**
     * @return the property names of the underlying JCR property {@link Event}s
    **/
    @Override
    public Set<String> getProperties() {
        return eventProperties;
    }

    /**
     * Add a property name to this event
     * @param property property name
     * @return this object for continued use
    **/
    @Override
    public FedoraEvent addProperty( final String property ) {
        eventProperties.add(property);
        return this;
    }

    /**
     * @return the path of the underlying JCR {@link Event}s
     */
    @Override
    public String getPath() {
        return getPath(e);
    }

    /**
     * Get the path of the node related to this event (removing property names
     * from the end of property nodes).
     * @param e JCR Event
     * @return the node path for this event
    **/
    public static String getPath(final Event e) {
        try {
            final String path;
            if (PROPERTY_TYPES.contains(e.getType())) {
                path = e.getPath().substring(0, e.getPath().lastIndexOf("/"));
            } else {
                path = e.getPath();
            }
            return path.replaceAll("/" + JCR_CONTENT, "");
        } catch (final RepositoryException e1) {
            throw new RepositoryRuntimeException("Error getting event path!", e1);
        }
    }

    /**
     * @return the user ID of the underlying JCR {@link Event}s
     */
    @Override
    public String getUserID() {
        return e.getUserID();
    }

    /**
     * @return the user data of the underlying JCR {@link Event}s
     */
    @Override
    public String getUserData() {
        try {
            return e.getUserData();
        } catch (final RepositoryException e1) {
            throw new RepositoryRuntimeException("Error getting event userData!", e1);
        }
    }

    /**
     * @return the date of the underlying JCR {@link Event}s
     */
    @Override
    public long getDate() {
        try {
            return e.getDate();
        } catch (final RepositoryException e1) {
            throw new RepositoryRuntimeException("Error getting event date!", e1);
        }
    }

    /**
     * Get the event ID.
     * @return Event identifier to use for building event URIs (e.g., in an external triplestore).
    **/
    @Override
    public String getEventID() {
        return eventID;
    }

    @Override
    public String toString() {

        return toStringHelper(this)
            .add("Event types:", String.join(",", getTypes().stream()
                            .map(EventType::getName)
                            .collect(toList())))
            .add("Event properties:", String.join(",", eventProperties))
            .add("Path:", getPath())
            .add("Date: ", getDate()).toString();
    }

    private static final Map<Integer, EventType> translation = ImmutableMap.<Integer, EventType>builder()
            .put(javax.jcr.observation.Event.NODE_ADDED, NODE_ADDED)
            .put(javax.jcr.observation.Event.NODE_REMOVED, NODE_REMOVED)
            .put(javax.jcr.observation.Event.PROPERTY_ADDED, PROPERTY_ADDED)
            .put(javax.jcr.observation.Event.PROPERTY_REMOVED, PROPERTY_REMOVED)
            .put(javax.jcr.observation.Event.PROPERTY_CHANGED, PROPERTY_CHANGED)
            .put(javax.jcr.observation.Event.NODE_MOVED, NODE_MOVED)
            .put(javax.jcr.observation.Event.PERSIST, PERSIST).build();

    /**
     * Get the Fedora event type for a JCR type
     *
     * @param i the integer value of a JCR type
     * @return EventType
     */
    public static EventType valueOf(final Integer i) {
        return forMap(translation).apply(i);
    }

    private static class DefaultPathMinter implements HierarchicalIdentifierSupplier { }
}
