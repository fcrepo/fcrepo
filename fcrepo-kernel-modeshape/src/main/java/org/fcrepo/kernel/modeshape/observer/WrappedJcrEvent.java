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

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.modeshape.jcr.api.observation.Event;

/**
 * Wrap a Modeshape JCR Event so we can modify its event type.
 *
 * @author whikloj
 * @since 2018-09-13
 */
public class WrappedJcrEvent implements Event {

    private final Event wrappedEvent;

    private final int eventType;

    /**
     * Construct a wrapped Modeshape JCR Event
     *
     * @param event The original event.
     * @param type The new event type.
     */
    public WrappedJcrEvent(final Event event, final int type) {
        this.wrappedEvent = event;
        this.eventType = type;
    }

    @Override
    public int getType() {
        return this.eventType;
    }

    @Override
    public String getPath() throws RepositoryException {
        return wrappedEvent.getPath();
    }

    @Override
    public String getUserID() {
        return wrappedEvent.getUserID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return wrappedEvent.getIdentifier();
    }

    @Override
    public Map getInfo() throws RepositoryException {
        return wrappedEvent.getInfo();
    }

    @Override
    public String getUserData() throws RepositoryException {
        return wrappedEvent.getUserData();
    }

    @Override
    public long getDate() throws RepositoryException {
        return wrappedEvent.getDate();
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return wrappedEvent.getPrimaryNodeType();
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return wrappedEvent.getMixinNodeTypes();
    }

}
