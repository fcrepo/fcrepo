
package org.fcrepo.audit;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import com.google.common.eventbus.Subscribe;

/**
 * An interface for recording auditable Fedora events.
 * 
 * @author Edwin Shin
 */
public interface Auditor {

    /**
     * @param e
     *        The {@Event} to record.
     * @throws RepositoryException
     */
    @Subscribe
    void recordEvent(final Event e) throws RepositoryException;
}
