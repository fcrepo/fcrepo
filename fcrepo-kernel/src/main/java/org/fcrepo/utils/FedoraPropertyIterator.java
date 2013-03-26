
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

public class FedoraPropertyIterator implements Iterator<Property> {

    PropertyIterator i;

    public FedoraPropertyIterator(final PropertyIterator i) {
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