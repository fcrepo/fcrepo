/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Node;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Apr 20, 2013
 */
public class NodeIterator implements Iterator<Node> {

    private javax.jcr.NodeIterator i;

    /**
     * @todo Add Documentation.
     */
    public NodeIterator(final javax.jcr.NodeIterator i) {
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
    public Node next() {
        return (Node)i.next();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public void remove() {
        i.remove();
    }

}
