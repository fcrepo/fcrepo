/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Property;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Apr 25, 2013
 */
public class PropertyIterator implements Iterator<Property> {

    private javax.jcr.PropertyIterator i;

    /**
     * @todo Add Documentation.
     */
    public PropertyIterator(final javax.jcr.PropertyIterator i) {
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
    public Property next() {
        return i.nextProperty();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public void remove() {
        i.remove();
    }
}
