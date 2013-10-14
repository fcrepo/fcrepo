
package org.fcrepo.kernel.testutilities;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import com.google.common.collect.Lists;

/**
 * A simple {@link PropertyIterator} for test purposes.
 *
 * @author ajs6f
 * @date Oct 14, 2013
 */
public class TestPropertyIterator implements PropertyIterator {

    private final Iterator<Property> iterator;

    public TestPropertyIterator(final List<Property> properties) {
        this.iterator = properties.iterator();
    }

    public TestPropertyIterator(final Property... properties) {
        this.iterator = Lists.newArrayList(properties).iterator();
    }

    @Override
    public void skip(final long skipNum) {
        for (int i = 0; i < skipNum; i++) {
            iterator.next();
        }

    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public long getPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public Property nextProperty() {
        return iterator.next();
    }

}
