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
package org.fcrepo.kernel.api.rdf;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.junit.Test;

/**
 *
 * @author acoburn
 */
public class DefaultRdfStreamTest {

    @Test
    @SuppressWarnings("static-method")
    public final void testMap() {
        final Node subject = createURI("subject");
        try (final RdfStream stream = new DefaultRdfStream(subject, getTriples(subject).stream())) {

            final List<String> objs = stream.map(Triple::getObject).map(Node::getURI).collect(toList());

            assertEquals(6, objs.size());
            assertEquals("obj1", objs.get(0));
            assertEquals("obj2", objs.get(1));
            assertEquals("obj3", objs.get(2));
        }
    }

    @Test
    @SuppressWarnings("static-method")
    public final void testFlatMap() {
        final Node subject = createURI("subject");

        final List<String> objs = of(subject, subject, subject)
            .flatMap(x -> new DefaultRdfStream(x, getTriples(x).stream()))
            .map(Triple::getObject)
            .map(Node::getURI)
            .collect(toList());

        assertEquals(18, objs.size());
        assertEquals("obj1", objs.get(0));
        assertEquals("obj1", objs.get(6));
    }

    private static List<Triple> getTriples(final Node subject) {
        final Node prop1 = createURI("prop1");
        final Node prop2 = createURI("prop2");
        return asList(
                new Triple(subject, prop1, createURI("obj1")),
                new Triple(subject, prop1, createURI("obj2")),
                new Triple(subject, prop1, createURI("obj3")),
                new Triple(subject, prop2, createURI("obj1")),
                new Triple(subject, prop2, createURI("obj2")),
                new Triple(subject, prop2, createURI("obj3")));
    }
}
