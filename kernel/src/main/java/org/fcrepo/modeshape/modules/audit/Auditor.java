package org.fcrepo.modeshape.modules.audit;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import com.google.common.eventbus.Subscribe;

/**
 * An interface for recording auditable Fedora events.
 *
 * @author Edwin Shin
 */
public interface Auditor {

    @Subscribe
    public void recordEvent(Event e) throws RepositoryException;
}
