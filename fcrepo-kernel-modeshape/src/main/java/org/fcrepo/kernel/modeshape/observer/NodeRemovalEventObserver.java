/**
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

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;
import org.fcrepo.metrics.RegistryService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.eventbus.EventBus;


/**
 * This is a listener that fakes a single event to signify object updates.
 * The batch of changes is broadcast to the Fedora event listeners as a
 * change to the jcr:lastModified property.
 *
 * @author armintor@gmail.com
 * @since Dec 5, 2013
 */
public class NodeRemovalEventObserver implements EventListener {

    private static final Logger LOGGER = getLogger(NodeRemovalEventObserver.class);

    /**
     * A simple counter of events that pass through this observer
     */
    static final Counter EVENT_COUNTER = RegistryService.getInstance().getMetrics().counter(
            name(
            NodeRemovalEventObserver.class, "onEvent"));

    static final int EVENT_TYPES = NODE_REMOVED;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    // THIS SESSION SHOULD NOT BE USED TO LOOK UP NODES
    private Session session;

    /**
     * Register this observer with the JCR event listeners
     * @throws RepositoryException if repository exception occurred
     */
    @PostConstruct
    public void buildListener() throws RepositoryException {
        session = repository.login();
        session.getWorkspace().getObservationManager().addEventListener(this,
                EVENT_TYPES, null, true, null, null, false);
        session.save();
    }

    /**
     * logout of the session
     * @throws RepositoryException if repository exception occurred
     */
    @PreDestroy
    public void stopListening() throws RepositoryException {
        session.getWorkspace().getObservationManager().removeEventListener(this);
        session.logout();
    }

    /**
     * Filter JCR events and transform them into our own FedoraEvents.
     *
     * @param events the JCR events
     */
    @Override
    public void onEvent(final javax.jcr.observation.EventIterator events) {

        // emit node removal events
        while (events.hasNext()) {
            final Event e = events.nextEvent();
            try {
                final String ePath = e.getPath();
                final int ls = ePath.lastIndexOf('/');
                // only propagate non-jcr node removals
                if (!ePath.startsWith("jcr:", ls + 1)) {
                    LOGGER.debug("Putting event: {} on the bus", e);
                    EVENT_COUNTER.inc();
                    eventBus.post(e);
                }
            } catch (final RepositoryException ex) {
                throw propagate(ex);
            }
        }
    }

}
