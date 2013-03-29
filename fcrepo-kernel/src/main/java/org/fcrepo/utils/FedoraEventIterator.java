
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

public class FedoraEventIterator implements Iterator<Event>, Iterable<Event> {

    EventIterator i;

    public FedoraEventIterator(final EventIterator i) {
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
