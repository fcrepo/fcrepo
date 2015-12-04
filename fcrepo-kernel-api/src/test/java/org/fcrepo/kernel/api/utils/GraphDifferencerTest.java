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
package org.fcrepo.kernel.api.utils;

import java.util.stream.Stream;

import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import org.junit.Test;

import static java.util.stream.Stream.of;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * <p>DifferencingIteratorTest class.</p>
 *
 * @author ksclarke
 */
public class GraphDifferencerTest {

    private Triple t_xyz = new Triple(createURI("x"), createURI("y"), createURI("z"));
    private Triple t_abc = new Triple(createURI("a"), createURI("b"), createURI("c"));
    private Triple t_typed_string = new Triple(createURI("i"),
                                               createURI("j"),
                                               createLiteral("k", XSDDatatype.XSDstring));
    private Triple t_untyped_string = new Triple(createURI("i"),
                                                 createURI("j"),
                                                 createLiteral("k"));
    private Triple t_int = new Triple(createURI("i"),
            createURI("j"),
            createLiteral("0", XSDDatatype.XSDint));
    private Triple t_int_equivalent = new Triple(createURI("i"),
            createURI("j"),
            createLiteral("000", XSDDatatype.XSDint));


    @Test
    public void testAllCommon() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        final GraphDifferencer diff = new GraphDifferencer(graph, new DefaultRdfStream(of(t_xyz)));

        final Stream<Triple> removed = diff.difference();
        final Stream<Triple> added = diff.notCommon();
        final Stream<Triple> common = diff.common();

        assertEquals(0, removed.count());

        assertEquals(0, added.count());

        assertTrue(common.filter(x -> x.equals(t_xyz)).findFirst().isPresent());
    }

    @Test
    public void testRemoveOne() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        final GraphDifferencer diff = new GraphDifferencer(graph, new DefaultRdfStream(of(t_xyz, t_abc)));

        final Stream<Triple> removed = diff.difference();
        final Stream<Triple> added = diff.notCommon();
        final Stream<Triple> common = diff.common();

        assertTrue(removed.filter(x -> x.equals(t_abc)).findFirst().isPresent());

        assertEquals(0, added.count());

        assertTrue(common.filter(x -> x.equals(t_xyz)).findFirst().isPresent());
    }

    @Test
    public void testAddOne() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_abc);
        graph.add(t_xyz);

        final GraphDifferencer diff = new GraphDifferencer(graph, new DefaultRdfStream(of(t_xyz)));

        final Stream<Triple> removed = diff.difference();
        final Stream<Triple> added = diff.notCommon();
        final Stream<Triple> common = diff.common();

        assertEquals(0, removed.count());

        assertTrue(added.filter(x -> x.equals(t_abc)).findFirst().isPresent());

        assertTrue(common.filter(x -> x.equals(t_xyz)).findFirst().isPresent());
    }

    @Test
    public void testAllDifferent() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        final GraphDifferencer diff = new GraphDifferencer(graph, new DefaultRdfStream(of(t_abc)));

        final Stream<Triple> removed = diff.difference();
        final Stream<Triple> added = diff.notCommon();
        final Stream<Triple> common = diff.common();

        assertTrue(removed.filter(x -> x.equals(t_abc)).findFirst().isPresent());

        assertTrue(added.filter(x -> x.equals(t_xyz)).findFirst().isPresent());

        assertEquals(0, common.count());
    }

    @Test
    public void testCommonRDFEqualStrings() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_untyped_string);

        final GraphDifferencer diff = new GraphDifferencer(graph, new DefaultRdfStream(of(t_typed_string)));

        final Stream<Triple> removed = diff.difference();
        final Stream<Triple> added = diff.notCommon();
        final Stream<Triple> common = diff.common();

        assertEquals(0, removed.count());
        assertEquals(0, added.count());
        assertTrue(common.filter(x -> x.equals(t_typed_string)).findFirst().isPresent());

    }

    @Test
    public void testCommonRDFEqualIntegers() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_int_equivalent);

        final GraphDifferencer diff = new GraphDifferencer(graph, new DefaultRdfStream(of(t_int)));

        final Stream<Triple> removed = diff.difference();
        final Stream<Triple> added = diff.notCommon();
        final Stream<Triple> common = diff.common();

        assertEquals(0, removed.count());
        assertEquals(0, added.count());
        assertTrue(common.filter(x -> x.equals(t_int)).findFirst().isPresent());

    }
}
