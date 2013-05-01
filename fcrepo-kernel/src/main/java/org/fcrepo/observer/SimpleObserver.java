
package org.fcrepo.observer;

import static com.google.common.collect.Iterables.filter;
import static com.yammer.metrics.MetricRegistry.name;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import org.fcrepo.utils.EventIterator;
import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;
import com.yammer.metrics.Counter;

/**
 * Simple JCR EventListener that filters JCR Events through a Fedora
 * EventFilter and puts the resulting stream onto the internal
 * Fedora EventBus as a stream of FedoraEvents.
 * 
 * @author ajs6f
 *
 */
public class SimpleObserver implements EventListener {

    private static final Logger LOGGER = getLogger(SimpleObserver.class);

    /**
     * A simple counter of events that pass through this observer
     */
    static final Counter EVENT_COUNTER = metrics.counter(name(
            SimpleObserver.class, "onEvent"));

    static final Integer EVENT_TYPES = NODE_ADDED + NODE_REMOVED + NODE_MOVED +
            PROPERTY_ADDED + PROPERTY_CHANGED + PROPERTY_REMOVED;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Inject
    private EventFilter eventFilter;

    @PostConstruct
    public void buildListener() throws RepositoryException {
        final Session session = repository.login();
        session.getWorkspace().getObservationManager().addEventListener(this,
                EVENT_TYPES, "/", true, null, null, false);
        session.save();
        session.logout();
    }

    /**
     * Filter JCR events and transform them into our own FedoraEvents.
     *
     * @param events
     */
    @Override
    public void onEvent(final javax.jcr.observation.EventIterator events) {

        for (final Event e : filter(new EventIterator(events), eventFilter)) {

            EVENT_COUNTER.inc();

            LOGGER.debug("Putting event: " + e.toString() + " on the bus.");
            eventBus.post(new FedoraEvent(e));
        }
    }

}
