/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.utils.iterators;

import java.util.Iterator;

import javax.jcr.Property;

import com.google.common.collect.ForwardingIterator;

/**
 * A type-aware iterator that wraps the generic JCR PropertyIterator
 *
 * @author ajs6f
 * @since Apr 25, 2013
 */
public class PropertyIterator extends ForwardingIterator<Property> implements
        Iterable<Property> {

    private final Iterator<Property> i;

    /**
     * Wrap the JCR PropertyIterator with our generic iterator
     *
     * @param i
     */
    @SuppressWarnings("unchecked")
    public PropertyIterator(final javax.jcr.PropertyIterator i) {
        this.i = i;
    }

    @Override
    public Iterator<Property> iterator() {
        return this;
    }

    @Override
    protected Iterator<Property> delegate() {
        return i;
    }
}
