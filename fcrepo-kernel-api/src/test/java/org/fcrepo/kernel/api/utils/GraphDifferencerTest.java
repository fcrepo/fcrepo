/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import java.util.stream.Stream;

import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Test;

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>DifferencingIteratorTest class.</p>
 *
 * @author ksclarke
 */
public class GraphDifferencerTest {

    private final Node subject = createURI("x");
    private final Triple t_xyz = new Triple(createURI("x"), createURI("y"), createURI("z"));
    private final Triple t_abc = new Triple(createURI("a"), createURI("b"), createURI("c"));
    private final Triple t_typed_string = new Triple(createURI("i"),
                                               createURI("j"),
                                               createLiteral("k", XSDDatatype.XSDstring));
    private final Triple t_untyped_string = new Triple(createURI("i"),
                                                 createURI("j"),
                                                 createLiteral("k"));
    private final Triple t_int = new Triple(createURI("i"),
            createURI("j"),
            createLiteral("0", XSDDatatype.XSDint));
    private final Triple t_int_equivalent = new Triple(createURI("i"),
            createURI("j"),
            createLiteral("000", XSDDatatype.XSDint));


    @Test
    public void testAllCommon() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        try (final DefaultRdfStream original = new DefaultRdfStream(subject, of(t_xyz))) {
            final GraphDifferencer diff = new GraphDifferencer(graph, original);

            final Stream<Triple> removed = diff.difference();
            final Stream<Triple> added = diff.notCommon();
            final Stream<Triple> common = diff.common();

            assertEquals(0, removed.count());

            assertEquals(0, added.count());

            assertTrue(common.anyMatch(x -> x.equals(t_xyz)));
        }
    }

    @Test
    public void testRemoveOne() {

        final Node subject = createURI("subject");
        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        try (final DefaultRdfStream original = new DefaultRdfStream(subject, of(t_xyz, t_abc))) {
            final GraphDifferencer diff = new GraphDifferencer(graph, original);

            final Stream<Triple> removed = diff.difference();
            final Stream<Triple> added = diff.notCommon();
            final Stream<Triple> common = diff.common();

            assertTrue(removed.anyMatch(x -> x.equals(t_abc)));

            assertEquals(0, added.count());

            assertTrue(common.anyMatch(x -> x.equals(t_xyz)));
        }
    }

    @Test
    public void testAddOne() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_abc);
        graph.add(t_xyz);

        try (final DefaultRdfStream original = new DefaultRdfStream(subject, of(t_xyz))) {
            final GraphDifferencer diff = new GraphDifferencer(graph, original);

            final Stream<Triple> removed = diff.difference();
            final Stream<Triple> added = diff.notCommon();
            final Stream<Triple> common = diff.common();

            assertEquals(0, removed.count());

            assertTrue(added.anyMatch(x -> x.equals(t_abc)));

            assertTrue(common.anyMatch(x -> x.equals(t_xyz)));
        }
    }

    @Test
    public void testAllDifferent() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        try (final DefaultRdfStream original = new DefaultRdfStream(subject, of(t_abc))) {
            final GraphDifferencer diff = new GraphDifferencer(graph, original);

            final Stream<Triple> removed = diff.difference();
            final Stream<Triple> added = diff.notCommon();
            final Stream<Triple> common = diff.common();

            assertTrue(removed.anyMatch(x -> x.equals(t_abc)));

            assertTrue(added.anyMatch(x -> x.equals(t_xyz)));

            assertEquals(0, common.count());
        }
    }

    @Test
    public void testCommonRDFEqualStrings() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_untyped_string);

        try (final DefaultRdfStream original = new DefaultRdfStream(subject, of(t_typed_string))) {
            final GraphDifferencer diff = new GraphDifferencer(graph, original);

            final Stream<Triple> removed = diff.difference();
            final Stream<Triple> added = diff.notCommon();
            final Stream<Triple> common = diff.common();

            assertEquals(0, removed.count());
            assertEquals(0, added.count());
            assertTrue(common.anyMatch(x -> x.equals(t_typed_string)));
        }
    }

    @Test
    public void testCommonRDFEqualIntegers() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_int_equivalent);

        try (final DefaultRdfStream original = new DefaultRdfStream(subject, of(t_int))) {
            final GraphDifferencer diff = new GraphDifferencer(graph, original);

            final Stream<Triple> removed = diff.difference();
            final Stream<Triple> added = diff.notCommon();
            final Stream<Triple> common = diff.common();

            assertEquals(0, removed.count());
            assertEquals(0, added.count());
            assertTrue(common.anyMatch(x -> x.equals(t_int)));
        }
    }
}
