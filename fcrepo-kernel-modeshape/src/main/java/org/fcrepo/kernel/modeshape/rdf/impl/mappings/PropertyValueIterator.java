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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import static com.google.common.collect.Iterators.singletonIterator;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;

/**
 * Iterate over all the values in a property or list of properties
 *
 * @author cabeer
 */
public class PropertyValueIterator extends AbstractIterator<Value> {
    private final Iterator<Property> properties;
    private Iterator<Value> currentValues;

    /**
     * Iterate through multiple properties' values
     * @param properties the properties
     */
    public PropertyValueIterator(final Iterator<Property> properties) {
        this.properties = properties;
        this.currentValues = null;
    }

    /**
     * Iterate through a property's values
     * @param property the properties
     */
    public PropertyValueIterator(final Property property) {
        this.properties = singletonIterator(property);
        this.currentValues = null;
    }

    @Override
    protected Value computeNext() {
        try {
            if (currentValues != null && currentValues.hasNext()) {
                return currentValues.next();
            }
            if (properties.hasNext()) {
                final Property property = properties.next();

                if (property.isMultiple()) {
                    currentValues = Iterators.forArray(property.getValues());
                    return currentValues.next();
                }
                currentValues = null;
                return property.getValue();
            }

            return endOfData();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
