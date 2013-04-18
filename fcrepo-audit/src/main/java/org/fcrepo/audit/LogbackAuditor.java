
package org.fcrepo.audit;

import static org.fcrepo.utils.EventType.getEventName;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    @Subscribe
    public void recordEvent(final Event e) throws RepositoryException {
        logger.info(e.getUserID() + " " + getEventName(e.getType()) + " " +
                e.getPath());
    }
}
