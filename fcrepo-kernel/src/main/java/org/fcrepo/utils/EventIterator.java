/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.observation.Event;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Apr 20, 2013
 */
public class EventIterator implements Iterator<Event>, Iterable<Event> {

    private javax.jcr.observation.EventIterator i;

    /**
     * @todo Add Documentation.
     */
    public EventIterator(final javax.jcr.observation.EventIterator i) {
        this.i = i;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Event next() {
        return i.nextEvent();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public void remove() {
        i.remove();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Iterator<Event> iterator() {
        return this;
    }

}
