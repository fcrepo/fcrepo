/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.http.commons.responses;

import static com.google.common.collect.ImmutableMap.of;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static com.hp.hpl.jena.sparql.core.DatasetGraphFactory.createMem;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.RdfLexicon.LAST_MODIFIED_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;

public class ViewHelpersTest {

    private ViewHelpers testObj;

    @Before
    public void setUp() {
        testObj = ViewHelpers.getInstance();
    }

    @Test
    public void shouldConvertAUriToNodeBreadcrumbs() {

        final UriInfo mockUriInfo = getUriInfoImpl();

        final Map<String, String> nodeBreadcrumbs =
            testObj.getNodeBreadcrumbs(mockUriInfo, createResource(
                    "http://localhost/fcrepo/a/b/c").asNode());

        assertEquals(of("http://localhost/fcrepo/a", "a",
                "http://localhost/fcrepo/a/b", "b",
                "http://localhost/fcrepo/a/b/c", "c"), nodeBreadcrumbs);
    }

    @Test
    public void shouldRefuseToConvertAForeignUriToNodeBreadcrumbs() {

        final UriInfo mockUriInfo = getUriInfoImpl();

        final Map<String, String> nodeBreadcrumbs =
            testObj.getNodeBreadcrumbs(mockUriInfo, createResource(
                    "http://somewhere/else/a/b/c").asNode());

        assertTrue(nodeBreadcrumbs.isEmpty());
    }

    @Test
    public void testIsFrozenNode() {
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b/c"), HAS_PRIMARY_TYPE.asNode(),
                createLiteral("nt:frozenNode"));
        assertTrue("Node is a frozen node.", testObj.isFrozenNode(mem, createURI("a/b/c")));
    }

    @Test
    public void testIsNotFrozenNode() {
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b/c"), HAS_PRIMARY_TYPE.asNode(),
                createLiteral("nt:file"));
        assertFalse("Node is not a frozen node.", testObj.isFrozenNode(mem, createURI("a/b/c")));
    }

    @Test
    public void testGetLabeledVersion() {
        final DatasetGraph mem = createMem();
        final String label = "testLabel";
        mem.add(createAnon(), createURI("a/b/c"), HAS_VERSION_LABEL.asNode(),
                createLiteral(label));
        assertEquals("Version label should be available.", label, testObj.getVersionLabel(mem, createURI("a/b/c"), ""));
    }

    @Test
    public void testGetUnlabeledVersion() {
        final DatasetGraph mem = createMem();
        assertEquals("Default version label should be used.", testObj.getVersionLabel(mem, createURI("a/b/c"), "default"), "default");
    }

    @Test
    public void testGetVersionDate() {
        final DatasetGraph mem = createMem();
        final String date = new Date().toString();
        mem.add(createAnon(), createURI("a/b/c"), LAST_MODIFIED_DATE.asNode(),
                createLiteral(date));
        assertEquals("Date should be available.", date, testObj.getVersionDate(mem, createURI("a/b/c")));
    }

    @Test
    public void testGetMissingVersionDate() {
        final DatasetGraph mem = createMem();
        assertNull("Date should not be available.", testObj.getVersionDate(mem, createURI("a/b/c")));
    }

    @Test
    public void shouldTryToExtractDublinCoreTitleFromNode() {
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b/c"), DC_TITLE.asNode(),
                createLiteral("abc"));

        assertEquals("abc", testObj.getObjectTitle(mem, createURI("a/b/c")));
    }

    @Test
    public void shouldUseTheObjectUriIfATitleIsNotAvailable() {
        final DatasetGraph mem = createMem();

        assertEquals("a/b/c", testObj.getObjectTitle(mem, createURI("a/b/c")));

    }

    @Test
    public void shouldUsetheBNodeIdIfItIsABNode() {
        final DatasetGraph mem = createMem();
        final Node anon = createAnon();
        assertEquals(anon.getBlankNodeLabel(), testObj
                .getObjectTitle(mem, anon));
    }

    @Test
    public void shouldJustUseTheStringIfItIsALiteral() {
        final DatasetGraph mem = createMem();
        final Node lit = createLiteral("xyz");
        assertEquals("\"xyz\"", testObj.getObjectTitle(mem, lit));
    }

    @Test
    public void shouldConvertRdfObjectsToStrings() {

        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("subject"), createURI("a/b/c"),
                NodeFactory.createLiteral("abc"));
        mem.add(createAnon(), createURI("subject"),
                createURI("a-numeric-type"), createTypedLiteral(0).asNode());
        mem.add(createAnon(), createURI("subject"),
                createURI("an-empty-string"), createLiteral(""));
        mem.add(createAnon(), createURI("subject"), createURI("a-uri"),
                createURI("some-uri"));

        assertEquals("abc", testObj.getObjectsAsString(mem,
                createURI("subject"), createResource("a/b/c"), true));
        assertEquals("0", testObj.getObjectsAsString(mem, createURI("subject"),
                createResource("a-numeric-type"), true));
        assertEquals("<empty>", testObj.getObjectsAsString(mem,
                createURI("subject"), createResource("an-empty-string"), true));
        assertEquals("&lt;<a href=\"some-uri\">some-uri</a>&gt;", testObj
                .getObjectsAsString(mem, createURI("subject"),
                        createResource("a-uri"), true));

        assertEquals("some-uri", testObj
                .getObjectsAsString(mem, createURI("subject"),
                        createResource("a-uri"), false));
        assertEquals("", testObj.getObjectsAsString(mem, createURI("subject"),
                createResource("a-nonexistent-uri"), true));

    }

    @Test
    public void shouldExtractNamespaceAndPrefix() {
        final Model model = createDefaultModel();
        model.setNsPrefix("prefix", "namespace");

        assertEquals("prefix:", testObj.getNamespacePrefix(model, "namespace", false));
        assertEquals("some-other-namespace", testObj.getNamespacePrefix(model,
                "some-other-namespace", false));
    }

    @Test
    public void shouldSortTriplesForDisplay() {
        final Model model = createDefaultModel();

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

        sortedTriples.get(0).matches(ANY, a.asNode(), propertyA.asNode(),
                literalA.asNode());
        sortedTriples.get(1).matches(ANY, a.asNode(), propertyA.asNode(),
                literalB.asNode());
        sortedTriples.get(2).matches(ANY, a.asNode(), propertyB.asNode(),
                literalA.asNode());
        sortedTriples.get(3).matches(ANY, a.asNode(), propertyC.asNode(),
                literalA.asNode());
        sortedTriples.get(4).matches(ANY, resourceB.asNode(),
                propertyC.asNode(), literalA.asNode());

    }

    @Test
    public void shouldConvertPrefixMappingToSparqlUpdatePrefixPreamble() {

        final Model model = createDefaultModel();

        model.setNsPrefix("prefix", "namespace");

        final String prefixPreamble = testObj.getPrefixPreamble(model);

        assertEquals("PREFIX prefix: <namespace>\n\n", prefixPreamble);
    }

    @Test
    public void shouldConvertRdfResourcesToNodes() {
        assertEquals(CREATED_BY.asNode(), testObj.asNode(CREATED_BY));
    }

    @Test
    public void shouldConvertStringLiteralsToNodes() {
        final String uri = "fedora:resource";
        final Literal URIRES = ResourceFactory.createPlainLiteral(uri);
        assertEquals(URIRES.asNode(), testObj.asLiteralStringNode(uri));
    }
}
