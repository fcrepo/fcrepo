/**
 * Copyright 2013 DuraSpace, Inc.
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
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.utils.EventType;

/**
 * A very simple abstraction to prevent event-driven machinery downstream from the repository from relying directly
 * on a JCR interface {@link Event). Can represent either a single JCR event or several.
 *
 * @author ajs6f
 * @date Feb 19, 2013
 */
public class FedoraEvent implements Event {

    public static final String NODE_TYPE_KEY = "fedora:nodeTypeKey";

    private Event e;

    private String nodeType;

    private Integer eventType = null;

    private Map<Object, Object> memoizedInfo;

    /**
     * Wrap a JCR Event with our FedoraEvent decorators
     *
     * @param e
     */
    public FedoraEvent(final Event e) {
        this(e, null);
    }

    /**
     * Wrap a JCR Event with our FedoraEvent decorators and include the type
     * given in the info map
     *
     * @param e
     * @param wrappedNodeType type of node for the event
     */
    public FedoraEvent(final Event e, final String wrappedNodeType) {
        checkArgument(e != null, "null cannot support a FedoraEvent!");
        this.e = e;
        this.nodeType = wrappedNodeType;
    }

    @Override
    public int getType() {
        return eventType != null ? eventType : e.getType();
    }

    /**
     * @param type
     * @return this object for continued use
     */
    public FedoraEvent setType(final Integer type) {
        eventType = type;
        return this;
    }

    @Override
    public String getPath() throws RepositoryException {
        return e.getPath();
    }

    @Override
    public String getUserID() {
        return e.getUserID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return e.getIdentifier();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Object, Object> getInfo() throws RepositoryException {
        if (memoizedInfo == null) {
            memoizedInfo = new HashMap<>(e.getInfo());
            memoizedInfo.put(NODE_TYPE_KEY, this.nodeType);
        }
        return memoizedInfo;
    }

    @Override
    public String getUserData() throws RepositoryException {
        return e.getUserData();
    }

    @Override
    public long getDate() throws RepositoryException {
        return e.getDate();
    }

    @Override
    public String toString() {
        try {
            return toStringHelper(this).add("Event type:", REPOSITORY_NAMESPACE + EventType.valueOf(getType())).add(
                    "Path:", getPath()).add("Date: ", getDate()).add("Info:", getInfo()).toString();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

}
