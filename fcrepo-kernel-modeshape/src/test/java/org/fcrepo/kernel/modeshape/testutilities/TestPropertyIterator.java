/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.testutilities;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import com.google.common.collect.Lists;

/**
 * A simple {@link PropertyIterator} for test purposes.
 *
 * @author ajs6f
 * @since Oct 14, 2013
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
