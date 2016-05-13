/*
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
package org.fcrepo.kernel.modeshape.utils;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Listen to Jena statement events to see whether a certain
 * property was added or removed.
 *
 * @author acoburn
 */
public class PropertyChangedListener extends StatementListener {

    private final Set<Resource> properties = new HashSet<>();

    private boolean propertyChanged = false;

    @Override
    public void addedStatement(final Statement s) {
        if (properties.contains(s.getPredicate())) {
            propertyChanged = true;
        }
    }

    @Override
    public void removedStatement(final Statement s) {
        if (properties.contains(s.getPredicate())) {
            propertyChanged = true;
        }
    }

    /**
     *  Add a property to watch
     *
     *  @param property the property
     *  @return whether the property was added
     */
    public boolean addProperty(final Resource property) {
        return properties.add(property);
    }

    /**
     * Report whether the given property changed.
     *
     * @return whether the property was changed
     */
    public boolean propertyChanged() {
        return propertyChanged;
    }
}
