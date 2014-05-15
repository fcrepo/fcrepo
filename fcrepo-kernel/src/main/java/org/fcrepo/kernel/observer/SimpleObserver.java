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

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterators.filter;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import org.fcrepo.kernel.observer.eventmappings.InternalExternalEventMapper;
import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.eventbus.EventBus;

/**
 * Simple JCR EventListener that filters JCR Events through a Fedora EventFilter
 * and puts the resulting stream onto the internal Fedora EventBus as a stream
 * of FedoraEvents.
 *
 * @author eddies
 * @author ajs6f
 * @since Feb 7, 2013
 */
public class SimpleObserver implements EventListener {

    private static final Logger LOGGER = getLogger(SimpleObserver.class);

    /**
     * A simple counter of events that pass through this observer
     */
    static final Counter EVENT_COUNTER = getMetrics().counter(name(SimpleObserver.class, "onEvent"));

    static final Integer EVENT_TYPES = NODE_ADDED + NODE_REMOVED + NODE_MOVED + PROPERTY_ADDED + PROPERTY_CHANGED
            + PROPERTY_REMOVED;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Inject
    private InternalExternalEventMapper eventMapper;

    @Inject
    private EventFilter eventFilter;

    // THIS SESSION SHOULD NOT BE USED TO LOOK UP NODES
    // it is used only to register and deregister this observer to the JCR
    private Session session;

    /**
     * Register this observer with the JCR event listeners
     *
     * @throws RepositoryException
     */
    @PostConstruct
    public void buildListener() throws RepositoryException {
        LOGGER.debug("Constructing an observer for JCR events...");
        session = repository.login();
        session.getWorkspace().getObservationManager()
                .addEventListener(this, EVENT_TYPES, "/", true, null, null, false);
        session.save();
    }

    /**
     * logout of the session
     *
     * @throws RepositoryException
     */
    @PreDestroy
    public void stopListening() throws RepositoryException {
        LOGGER.debug("Destroying an observer for JCR events...");
        session.getWorkspace().getObservationManager().removeEventListener(this);
        session.logout();
    }

    /**
     * Filter JCR events and transform them into our own FedoraEvents.
     *
     * @param events
     */
    @Override
    public void onEvent(final javax.jcr.observation.EventIterator events) {
        Session lookupSession = null;
        try {
            lookupSession = repository.login();
            @SuppressWarnings("unchecked")
            final Iterator<Event> filteredEvents = filter(events, eventFilter.getFilter(lookupSession));
            final Iterator<FedoraEvent> publishableEvents = eventMapper.apply(filteredEvents);
            while (publishableEvents.hasNext()) {
                eventBus.post(publishableEvents.next());
                EVENT_COUNTER.inc();
            }
        } catch (final RepositoryException ex) {
            throw propagate(ex);
        } finally {
            if (lookupSession != null) {
                lookupSession.logout();
            }
        }
    }
}
