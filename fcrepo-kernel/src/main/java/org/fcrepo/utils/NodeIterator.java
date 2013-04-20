
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Node;

public class NodeIterator implements Iterator<Node> {

    javax.jcr.NodeIterator i;

    public NodeIterator(final javax.jcr.NodeIterator i) {
        this.i = i;
    }

    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    @Override
    public Node next() {
        return i.nextNode();
    }

    @Override
    public void remove() {
        i.remove();
    }

}
