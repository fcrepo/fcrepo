
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class FedoraNodeIterator implements Iterator<Node> {

    NodeIterator i;

    public FedoraNodeIterator(final NodeIterator i) {
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
