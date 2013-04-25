
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.fcrepo.Datastream;

public class DatastreamIterator implements Iterator<Datastream> {

    private final NodeIterator nodes;
    
    private Node nextNode;
    
    public DatastreamIterator(final NodeIterator nodes) {
        this.nodes = nodes;
        if (nodes != null) lookAhead();
    }

    @Override
    public boolean hasNext() {
        return nextNode != null;
    }

    @Override
    public Datastream next() {
        Datastream result = new Datastream(nextNode);
        lookAhead();
        return result;
    }

    @Override
    public void remove() {
        nodes.remove();
    }
    
    private void lookAhead() {
        nextNode = null;
        while (nextNode == null && nodes.hasNext()) {
            try {
                Node n = nodes.nextNode();
                if (n.isNodeType("nt:file")) nextNode = n;
                else System.out.println("rejected node of type " + n.getPrimaryNodeType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
