
package org.fcrepo.responses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.fcrepo.RdfLexicon;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.core.Quad;

public class ViewHelpersTest {

    private ViewHelpers testObj;

    @Before
    public void setUp() {
        testObj = ViewHelpers.getInstance();
    }

    @Test
    public void shouldConvertAUriToNodeBreadcrumbs() {

        UriInfo mockUriInfo = TestHelpers.getUriInfoImpl();

        final Map<String, String> nodeBreadcrumbs =
                testObj.getNodeBreadcrumbs(mockUriInfo, ResourceFactory
                        .createResource("http://localhost/fcrepo/a/b/c")
                        .asNode());

        assertEquals(ImmutableMap.of("http://localhost/fcrepo/a", "a",
                "http://localhost/fcrepo/a/b", "b",
                "http://localhost/fcrepo/a/b/c", "c"), nodeBreadcrumbs);
    }

    @Test
    public void shouldRefuseToConvertAForeignUriToNodeBreadcrumbs() {

        UriInfo mockUriInfo = TestHelpers.getUriInfoImpl();

        final Map<String, String> nodeBreadcrumbs =
                testObj.getNodeBreadcrumbs(mockUriInfo, ResourceFactory
                        .createResource("http://somewhere/else/a/b/c").asNode());

        assertTrue(nodeBreadcrumbs.isEmpty());
    }

    @Test
    public void shouldTryToExtractDublinCoreTitleFromNode() {
        final DatasetGraph mem = DatasetGraphFactory.createMem();
        mem.add(NodeFactory.createAnon(), NodeFactory.createURI("a/b/c"),
                RdfLexicon.DC_TITLE.asNode(), NodeFactory.createLiteral("abc"));

        assertEquals("abc", testObj.getObjectTitle(mem, NodeFactory
                .createURI("a/b/c")));
    }

    @Test
    public void shouldUseTheObjectUriIfATitleIsNotAvailable() {
        final DatasetGraph mem = DatasetGraphFactory.createMem();

        assertEquals("a/b/c", testObj.getObjectTitle(mem, NodeFactory
                .createURI("a/b/c")));

    }

    @Test
    public void shouldConvertRdfObjectsToStrings() {

        final DatasetGraph mem = DatasetGraphFactory.createMem();
        mem.add(NodeFactory.createAnon(), NodeFactory.createURI("subject"),
                NodeFactory.createURI("a/b/c"), NodeFactory
                        .createLiteral("abc"));
        mem.add(NodeFactory.createAnon(), NodeFactory.createURI("subject"),
                NodeFactory.createURI("a-numeric-type"), ResourceFactory
                        .createTypedLiteral(0).asNode());
        mem.add(NodeFactory.createAnon(), NodeFactory.createURI("subject"),
                NodeFactory.createURI("an-empty-string"), NodeFactory
                        .createLiteral(""));
        mem.add(NodeFactory.createAnon(), NodeFactory.createURI("subject"),
                NodeFactory.createURI("a-uri"), NodeFactory
                        .createURI("some-uri"));

        assertEquals("abc", testObj.getObjectsAsString(mem, NodeFactory
                .createURI("subject"), ResourceFactory.createResource("a/b/c")));
        assertEquals("0", testObj.getObjectsAsString(mem, NodeFactory
                .createURI("subject"), ResourceFactory
                .createResource("a-numeric-type")));
        assertEquals("<empty>", testObj.getObjectsAsString(mem, NodeFactory
                .createURI("subject"), ResourceFactory
                .createResource("an-empty-string")));
        assertEquals("&lt;<a href=\"some-uri\">some-uri</a>&gt;", testObj
                .getObjectsAsString(mem, NodeFactory.createURI("subject"),
                        ResourceFactory.createResource("a-uri")));
        assertEquals("", testObj.getObjectsAsString(mem, NodeFactory
                .createURI("subject"), ResourceFactory
                .createResource("a-nonexistent-uri")));

    }

    @Test
    public void shouldExtractNamespaceAndPrefix() {
        final Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("prefix", "namespace");

        assertEquals("prefix:", testObj.getNamespacePrefix(model, "namespace"));
        assertEquals("some-other-namespace", testObj.getNamespacePrefix(model,
                "some-other-namespace"));
    }

    @Test
    public void shouldSortTriplesForDisplay() {
        final Model model = ModelFactory.createDefaultModel();

        model.setNsPrefix("prefix", "namespace");
        final Property propertyA = model.createProperty("namespace", "a");
        final Property propertyB = model.createProperty("namespace", "b");
        final Property propertyC = model.createProperty("c");
        final Literal literalA = model.createLiteral("a");
        final Literal literalB = model.createLiteral("b");
        final Resource resourceB = model.createResource("b");
        model.add(resourceB, propertyA, literalA);

        final Resource a = model.createResource("a");
        model.add(a, propertyC, literalA);

        model.add(a, propertyB, literalA);

        model.add(a, propertyA, literalA);
        model.add(a, propertyA, literalB);

        final Iterator<Quad> iterator =
                DatasetFactory.create(model).asDatasetGraph().find();

        final List<Quad> sortedTriples =
                testObj.getSortedTriples(model, iterator);

        sortedTriples.get(0).matches(Node.ANY, a.asNode(), propertyA.asNode(),
                literalA.asNode());
        sortedTriples.get(1).matches(Node.ANY, a.asNode(), propertyA.asNode(),
                literalB.asNode());
        sortedTriples.get(2).matches(Node.ANY, a.asNode(), propertyB.asNode(),
                literalA.asNode());
        sortedTriples.get(3).matches(Node.ANY, a.asNode(), propertyC.asNode(),
                literalA.asNode());
        sortedTriples.get(4).matches(Node.ANY, resourceB.asNode(),
                propertyC.asNode(), literalA.asNode());

    }

    @Test
    public void shouldConvertPrefixMappingToSparqlUpdatePrefixPreamble() {

        final Model model = ModelFactory.createDefaultModel();

        model.setNsPrefix("prefix", "namespace");

        final String prefixPreamble = testObj.getPrefixPreamble(model);

        assertEquals("PREFIX prefix: <namespace>\n\n", prefixPreamble);
    }

    @Test
    public void shouldConvertRdfResourcesToNodes() {
        assertEquals(RdfLexicon.CREATED_BY.asNode(), testObj
                .asNode(RdfLexicon.CREATED_BY));
    }
}
