/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.observer;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.utils.EventType;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

/**
 * A very simple abstraction to prevent event-driven machinery downstream from the repository from relying directly
 * on a JCR interface {@link Event}. Can represent either a single JCR event or several.
 *
 * @author ajs6f
 * @since Feb 19, 2013
 */
public class FedoraEvent {

    public static final String NODE_TYPE_KEY = "fedora:nodeTypeKey";

    private Event e;

    private Set<Integer> eventTypes = new HashSet<>();

    /**
     * Wrap a JCR Event with our FedoraEvent decorators
     *
     * @param e
     */
    public FedoraEvent(final Event e) {
        checkArgument(e != null, "null cannot support a FedoraEvent!");
        this.e = e;
    }

    /**
     * @return the event types of the underlying JCR {@link Event}s
     */
    public Set<Integer> getTypes() {
        return eventTypes != null ? union(singleton(e.getType()), eventTypes) : singleton(e.getType());
    }

    /**
     * @param type
     * @return this object for continued use
     */
    public FedoraEvent addType(final Integer type) {
        eventTypes.add(type);
        return this;
    }

    /**
     * @return the path of the underlying JCR {@link Event}s
     */
    public String getPath() throws RepositoryException {
        return e.getPath();
    }

    /**
     * @return the user ID of the underlying JCR {@link Event}s
     */
    public String getUserID() {
        return e.getUserID();
    }

    /**
     * @return the node identifer of the underlying JCR {@link Event}s
     */
    public String getIdentifier() throws RepositoryException {
        return e.getIdentifier();
    }

    /**
     * @return the info map of the underlying JCR {@link Event}s
     */
    @SuppressWarnings("unchecked")
    public Map<Object, Object> getInfo() throws RepositoryException {
        return new HashMap<>(e.getInfo());
    }

    /**
     * @return the user data of the underlying JCR {@link Event}s
     */
    public String getUserData() throws RepositoryException {
        return e.getUserData();
    }

    /**
     * @return the date of the underlying JCR {@link Event}s
     */
    public long getDate() throws RepositoryException {
        return e.getDate();
    }

    @Override
    public String toString() {
        try {
            return toStringHelper(this).add("Event types:",
                    Joiner.on(',').join(Iterables.transform(getTypes(), new Function<Integer, String>() {

                        @Override
                        public String apply(final Integer type) {
                            return EventType.valueOf(type).getName();
                        }
                    }))).add("Path:", getPath()).add("Date: ", getDate()).add("Info:", getInfo()).toString();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }
}
