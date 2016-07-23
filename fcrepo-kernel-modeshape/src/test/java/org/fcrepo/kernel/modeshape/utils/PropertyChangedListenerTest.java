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

import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.apache.jena.vocabulary.DC.creator;
import static org.apache.jena.vocabulary.DC.title;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

/**
 * @author acoburn
 * @since May 17, 2016
 */
public class PropertyChangedListenerTest {

    @Test
    public void testAddedTriple() {

        final AtomicBoolean changed = new AtomicBoolean();
        assertFalse(changed.get());

        final PropertyChangedListener listener = new PropertyChangedListener(title, changed);

        listener.addedStatement(createStatement(
                    createResource("<info:fedora/foo>"), creator, createPlainLiteral("some creator")));

        assertFalse(changed.get());

        listener.addedStatement(createStatement(
                    createResource("<info:fedora/foo>"), title, createPlainLiteral("some title")));

        assertTrue(changed.get());
    }

    @Test
    public void testRemovedTriple() {

        final AtomicBoolean changed = new AtomicBoolean();
        assertFalse(changed.get());

        final PropertyChangedListener listener = new PropertyChangedListener(title, changed);

        listener.removedStatement(createStatement(
                    createResource("<info:fedora/foo>"), creator, createPlainLiteral("some creator")));

        assertFalse(changed.get());

        listener.removedStatement(createStatement(
                    createResource("<info:fedora/foo>"), title, createPlainLiteral("some title")));

        assertTrue(changed.get());
    }

}
