/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jena.rdf.listeners.StatementListener;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * Listen to Jena statement events to see whether a certain
 * property was added or removed.
 *
 * @author acoburn
 */
public class PropertyChangedListener extends StatementListener {

    private final Resource property;

    private final AtomicBoolean changed;

    /**
     * Create a listener for a particular RDF predicate.
     *
     * @param property the predicate for which to watch
     * @param changed a value to keep track of whether the value changed
     */
    public PropertyChangedListener(final Resource property, final AtomicBoolean changed) {
        this.property = property;
        this.changed = changed;
    }

    @Override
    public void addedStatement(final Statement s) {
        if (property.equals(s.getPredicate())) {
            changed.set(true);
        }
    }

    @Override
    public void removedStatement(final Statement s) {
        if (property.equals(s.getPredicate())) {
            changed.set(true);
        }
    }
}
