
package org.fcrepo.observer;

import javax.jcr.observation.Event;

/**
 * Simple EventFilter that does no filtering.
 * 
 * @author ajs6f
 *
 */
public class NOOPFilter implements EventFilter {

    @Override
    public boolean apply(final Event event) {
        return true;
    }
}
