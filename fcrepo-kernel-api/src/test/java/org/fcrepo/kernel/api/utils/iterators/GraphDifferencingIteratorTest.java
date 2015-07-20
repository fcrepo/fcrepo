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
package org.fcrepo.kernel.api.utils.iterators;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import org.junit.Test;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.junit.Assert.assertTrue;

/**
 * <p>DifferencingIteratorTest class.</p>
 *
 * @author ksclarke
 */
public class GraphDifferencingIteratorTest {

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

        final GraphDifferencingIterator iterator = new GraphDifferencingIterator(graph, new RdfStream(t_xyz));

        final ImmutableSet<Triple> removed = copyOf(iterator);
        final ImmutableSet<Triple> added = copyOf(iterator.notCommon());
        final ImmutableSet<Triple> common = copyOf(iterator.common());

        assertTrue(removed.isEmpty());

        assertTrue(added.isEmpty());

        assertTrue(common.contains(t_xyz));
    }

    @Test
    public void testRemoveOne() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        final GraphDifferencingIterator iterator = new GraphDifferencingIterator(graph, new RdfStream(t_xyz, t_abc));

        final ImmutableSet<Triple> removed = copyOf(iterator);
        final ImmutableSet<Triple> added = copyOf(iterator.notCommon());
        final ImmutableSet<Triple> common = copyOf(iterator.common());

        assertTrue(removed.contains(t_abc));

        assertTrue(added.isEmpty());

        assertTrue(common.contains(t_xyz));
    }

    @Test
    public void testAddOne() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_abc);
        graph.add(t_xyz);

        final GraphDifferencingIterator iterator = new GraphDifferencingIterator(graph, new RdfStream(t_xyz));

        final ImmutableSet<Triple> removed = copyOf(iterator);
        final ImmutableSet<Triple> added = copyOf(iterator.notCommon());
        final ImmutableSet<Triple> common = copyOf(iterator.common());

        assertTrue(removed.isEmpty());

        assertTrue(added.contains(t_abc));

        assertTrue(common.contains(t_xyz));
    }

    @Test
    public void testAllDifferent() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_xyz);

        final GraphDifferencingIterator iterator = new GraphDifferencingIterator(graph, new RdfStream(t_abc));

        final ImmutableSet<Triple> removed = copyOf(iterator);
        final ImmutableSet<Triple> added = copyOf(iterator.notCommon());
        final ImmutableSet<Triple> common = copyOf(iterator.common());

        assertTrue(removed.contains(t_abc));

        assertTrue(added.contains(t_xyz));

        assertTrue(common.isEmpty());
    }

    @Test
    public void testCommonRDFEqualStrings() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_untyped_string);

        final GraphDifferencingIterator iterator = new GraphDifferencingIterator(graph, new RdfStream(t_typed_string));

        final ImmutableSet<Triple> removed = copyOf(iterator);
        final ImmutableSet<Triple> added = copyOf(iterator.notCommon());
        final ImmutableSet<Triple> common = copyOf(iterator.common());

        assertTrue(removed.isEmpty());
        assertTrue(added.isEmpty());
        assertTrue(common.contains(t_typed_string));

    }

    @Test
    public void testCommonRDFEqualIntegers() {

        final Graph graph = GraphFactory.createDefaultGraph();
        graph.add(t_int_equivalent);

        final GraphDifferencingIterator iterator = new GraphDifferencingIterator(graph, new RdfStream(t_int));

        final ImmutableSet<Triple> removed = copyOf(iterator);
        final ImmutableSet<Triple> added = copyOf(iterator.notCommon());
        final ImmutableSet<Triple> common = copyOf(iterator.common());

        assertTrue(removed.isEmpty());
        assertTrue(added.isEmpty());
        assertTrue(common.contains(t_int));

    }
}
