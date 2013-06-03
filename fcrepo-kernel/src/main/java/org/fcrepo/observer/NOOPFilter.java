/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.observer;

import javax.jcr.observation.Event;

/**
 * Simple EventFilter that does no filtering.
 *
 * @author eddies
 * @date Feb 7, 2013
 */
public class NOOPFilter implements EventFilter {

    /**
     * A no-op filter that passes every Event through.
     * @param event
     * @return true under all circumstances
     */
    @Override
    public boolean apply(final Event event) {
        return true;
    }
}
