
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Property;

public class PropertyIterator implements Iterator<Property> {

    private javax.jcr.PropertyIterator i;

    public PropertyIterator(final javax.jcr.PropertyIterator i) {
        this.i = i;
    }

    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    @Override
    public Property next() {
        return i.nextProperty();
    }

    @Override
    public void remove() {
        i.remove();
    }
}