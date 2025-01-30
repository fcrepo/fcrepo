/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.rdf;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.junit.jupiter.api.Test;

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
