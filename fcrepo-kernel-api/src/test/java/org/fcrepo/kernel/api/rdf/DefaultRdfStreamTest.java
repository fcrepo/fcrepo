/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.rdf;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.utils.WrappingStream;
import org.fcrepo.kernel.api.utils.WrappingStreamTest;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

/**
 * Test Class for {@link DefaultRdfStream}
 *
 * @author acoburn
 * @author whikloj
 */
public class DefaultRdfStreamTest extends WrappingStreamTest {

    @Override
    protected WrappingStream<Triple> generateFloatStream() {
        return new DefaultRdfStream(createURI("subject"), Stream.of(
                new Triple(subject, predicate, objectFloat1),
                new Triple(subject, predicate, objectFloat2),
                new Triple(subject, predicate, objectFloat3)
        ));
    }

    @Override
    protected WrappingStream<Triple> generateTextStream() {
        return new DefaultRdfStream(createURI("subject"), Stream.of(
                new Triple(subject, predicate, objectA),
                new Triple(subject, predicate, objectB),
                new Triple(subject, predicate, objectC)
        ));
    }

    @Override
    protected WrappingStream<Triple> generateIntStream() {
        return new DefaultRdfStream(createURI("subject"), Stream.of(
                new Triple(subject, predicate, objectInt1),
                new Triple(subject, predicate, objectInt2),
                new Triple(subject, predicate, objectInt3)
        ));
    }

    @Test
    public final void testMapCustom() {
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
    public final void testFlatMapCustom() {
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

    @Test
    public void testFromModel() {
        final Resource subject = createResource("subject");
        final Model model = createDefaultModel();
        model.add(new StatementImpl(subject, CREATED_BY, createPlainLiteral("test-user")));
        model.add(new StatementImpl(
                subject,
                CREATED_DATE,
                createTypedLiteral("2023-10-01T00:00:00Z", XSDDatatype.XSDdateTime)
        ));
        model.add(new StatementImpl(subject, RDF.type, createResource("http://example.org/Type")));
        try (final var stream = DefaultRdfStream.fromModel(subject.asNode(), model)) {
            assertEquals(subject.asNode(), stream.topic());
            final List<Triple> objects = stream.collect(toList());
            assertEquals(3, objects.size());
            assertTrue(objects.contains(new Triple(subject.asNode(), RDF.type.asNode(), createURI("http://example.org/Type"))));
            assertTrue(objects.contains(new Triple(subject.asNode(), CREATED_BY.asNode(), createLiteral("test-user"))));
            assertTrue(objects.contains(new Triple(
                    subject.asNode(),
                    CREATED_DATE.asNode(),
                    createLiteral("2023-10-01T00:00:00Z", XSDDatatype.XSDdateTime)
            )));
        }
    }
}
