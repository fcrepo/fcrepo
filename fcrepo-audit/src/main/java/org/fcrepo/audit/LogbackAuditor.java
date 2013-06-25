
package org.fcrepo.audit;

import static org.fcrepo.utils.EventType.getEventName;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * A proof-of-concept Auditor implementation that uses Logback.
 * 
 * @author Edwin Shin
 */
public class LogbackAuditor implements Auditor {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(LogbackAuditor.class);

    @Inject
    private EventBus eventBus;

    /**
     * Register with the EventBus to receive events.
     */
    @PostConstruct
    public void register() {
        logger.debug("Initializing: " + this.getClass().getCanonicalName());
        eventBus.register(this);
    }

    @Override
    @Subscribe
    public void recordEvent(final Event e) throws RepositoryException {
        logger.info(e.getUserID() + " " + getEventName(e.getType()) + " " +
                e.getPath());
    }
}
