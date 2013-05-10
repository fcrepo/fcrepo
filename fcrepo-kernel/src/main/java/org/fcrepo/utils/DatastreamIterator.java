
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.fcrepo.Datastream;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class DatastreamIterator implements Iterator<Datastream> {

    private static final Logger logger = getLogger(DatastreamIterator.class);

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
                if (n.isNodeType(JcrConstants.NT_FILE)) {
                    nextNode = n;
                } else {
                    logger.debug("skipping node of type {}", n.getPrimaryNodeType());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
