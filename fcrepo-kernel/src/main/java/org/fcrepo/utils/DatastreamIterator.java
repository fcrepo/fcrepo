
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.NodeIterator;

import org.fcrepo.Datastream;

public class DatastreamIterator implements Iterator<Datastream> {

    private final NodeIterator nodes;

    public DatastreamIterator(final NodeIterator nodes) {
        this.nodes = nodes;
    }

    public Datastream nextDatastream() {
        return new Datastream(nodes.nextNode());
    }

    @Override
    public boolean hasNext() {
        return nodes.hasNext();
    }

    @Override
    public Datastream next() {
        return new Datastream(nodes.nextNode());
    }

    @Override
    public void remove() {
        nodes.remove();
    }

    public void skip(final long skipNum) {
        nodes.skip(skipNum);
    }

    public long getSize() {
        return nodes.getSize();
    }

    public long getPosition() {
        return nodes.getPosition();
    }

}
