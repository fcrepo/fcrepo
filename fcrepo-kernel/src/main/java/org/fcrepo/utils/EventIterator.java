
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.observation.Event;

public class EventIterator implements Iterator<Event>, Iterable<Event> {

    javax.jcr.observation.EventIterator i;

    public EventIterator(final javax.jcr.observation.EventIterator i) {
        this.i = i;
    }

    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    @Override
    public Event next() {
        return i.nextEvent();
    }

    @Override
    public void remove() {
        i.remove();
    }

    @Override
    public Iterator<Event> iterator() {
        return this;
    }

}
