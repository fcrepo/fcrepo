package org.fcrepo.responses;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Node;
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
import org.fcrepo.RdfLexicon;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ViewHelpersTest {

    private ViewHelpers testObj;

    @Before
    public void setUp() {
        testObj = ViewHelpers.getInstance();
    }
    @Test
    public void shouldConvertAUriToNodeBreadcrumbs() {

        UriInfo mockUriInfo = TestHelpers.getUriInfoImpl();

        final Map<String,String> nodeBreadcrumbs = testObj.getNodeBreadcrumbs(mockUriInfo, ResourceFactory.createResource("http://localhost/fcrepo/a/b/c").asNode());

        assertEquals(ImmutableMap.of("a", "http://localhost/fcrepo/a", "b", "http://localhost/fcrepo/a/b", "c", "http://localhost/fcrepo/a/b/c"), nodeBreadcrumbs);
    }

    @Test
    public void shouldTryToExtractDublinCoreTitleFromNode() {
        final DatasetGraph mem = DatasetGraphFactory.createMem();
        mem.add(Node.createAnon(), Node.createURI("a/b/c"), RdfLexicon.DC_TITLE.asNode(), Node.createLiteral("abc"));

        assertEquals("abc", testObj.getObjectTitle(mem,  Node.createURI("a/b/c")));
    }

    @Test
    public void shouldUseTheObjectUriIfATitleIsNotAvailable() {
        final DatasetGraph mem = DatasetGraphFactory.createMem();

        assertEquals("a/b/c", testObj.getObjectTitle(mem,  Node.createURI("a/b/c")));

    }

    @Test
    public void shouldConvertRdfObjectsToStrings() {

        final DatasetGraph mem = DatasetGraphFactory.createMem();
        mem.add(Node.createAnon(), Node.createURI("subject"), Node.createURI("a/b/c"), Node.createLiteral("abc"));
        mem.add(Node.createAnon(), Node.createURI("subject"), Node.createURI("a-numeric-type"), ResourceFactory.createTypedLiteral(0).asNode());
        mem.add(Node.createAnon(), Node.createURI("subject"), Node.createURI("an-empty-string"), Node.createLiteral(""));
        mem.add(Node.createAnon(), Node.createURI("subject"), Node.createURI("a-uri"), Node.createURI("some-uri"));

        assertEquals("abc", testObj.getObjectsAsString(mem, Node.createURI("subject"), ResourceFactory.createResource("a/b/c")));
        assertEquals("0", testObj.getObjectsAsString(mem, Node.createURI("subject"), ResourceFactory.createResource("a-numeric-type")));
        assertEquals("<empty>", testObj.getObjectsAsString(mem, Node.createURI("subject"), ResourceFactory.createResource("an-empty-string")));
        assertEquals("&lt;<a href=\"some-uri\">some-uri</a>&gt;", testObj.getObjectsAsString(mem, Node.createURI("subject"), ResourceFactory.createResource("a-uri")));

    }

    @Test
    public void shouldExtractNamespaceAndPrefix() {
        final Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("prefix", "namespace");

        assertEquals("prefix:", testObj.getNamespacePrefix(model, "namespace"));
        assertEquals("some-other-namespace", testObj.getNamespacePrefix(model, "some-other-namespace"));
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


        final Iterator<Quad> iterator = DatasetFactory.create(model).asDatasetGraph().find();

        final List<Quad> sortedTriples = testObj.getSortedTriples(model, iterator);

        sortedTriples.get(0).matches(Node.ANY, a.asNode(), propertyA.asNode(), literalA.asNode());
        sortedTriples.get(1).matches(Node.ANY, a.asNode(), propertyA.asNode(), literalB.asNode());
        sortedTriples.get(2).matches(Node.ANY, a.asNode(), propertyB.asNode(), literalA.asNode());
        sortedTriples.get(3).matches(Node.ANY, a.asNode(), propertyC.asNode(), literalA.asNode());
        sortedTriples.get(4).matches(Node.ANY, resourceB.asNode(), propertyC.asNode(), literalA.asNode());

    }
}
