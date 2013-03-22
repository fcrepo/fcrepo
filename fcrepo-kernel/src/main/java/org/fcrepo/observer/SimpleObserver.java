
package org.fcrepo.observer;

import static com.google.common.collect.Collections2.filter;
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
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList.Builder;
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

    final private Integer eventTypes = NODE_ADDED + NODE_REMOVED + NODE_MOVED +
            PROPERTY_ADDED + PROPERTY_CHANGED + PROPERTY_REMOVED;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Inject
    private EventFilter eventFilter;

    static final private Logger logger = getLogger(SimpleObserver.class);

    static final Counter eventCounter = metrics.counter(name(
            SimpleObserver.class, "onEvent"));

    @PostConstruct
    public void buildListener() throws RepositoryException {
        Session session = repository.login();
        session.getWorkspace().getObservationManager().addEventListener(this,
                eventTypes, "/", true, null, null, false);
        session.save();
        session.logout();
    }

    // it's okay to suppress type-safety warning here,
    // because we know that EventIterator only produces
    // Events, like an Iterator<Event>
    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(EventIterator events) {

        for (Event e : filter(new Builder<Event>().addAll(events).build(),
                eventFilter)) {

            eventCounter.inc();

            logger.debug("Putting event: " + e.toString() + " on the bus.");
            eventBus.post(new FedoraEvent(e));
        }
    }

}
