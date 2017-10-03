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
package org.fcrepo.http.commons.responses;

import static com.google.common.collect.ImmutableMap.of;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.DC.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.RDFS.label;
import static org.apache.jena.vocabulary.SKOS.prefLabel;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBES;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.time.Instant.now;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;

/**
 * <p>ViewHelpersTest class.</p>
 *
 * @author awoods
 */
public class ViewHelpersTest {

    private ViewHelpers testObj;

    @Before
    public void setUp() {
        testObj = ViewHelpers.getInstance();
    }

    @Test
    @Ignore("Until Mementos have been implemented!")
    public void testGetVersions() {
        final Graph mem = createDefaultModel().getGraph();
        final Node version = createURI("http://localhost/fcrepo/abc/fcr:version/adcd");
//        mem.add(new Triple(createURI("http://localhost/fcrepo/abc"), HAS_VERSION.asNode(),
//                version));
        mem.add(new Triple(version, CREATED_DATE.asNode(), createLiteral(now().toString())));
        assertEquals("Version should be available.",
                version, testObj.getVersions(mem, createURI("http://localhost/fcrepo/abc")).next());
    }

    @Test
    @Ignore("Until Mementos have been implemented!")
    public void testGetOrderedVersions() {
        final Node resource = createURI("http://localhost/fcrepo/abc");
        final Node v1 = createURI("http://localhost/fcrepo/abc/fcr:version/1");
        final Node v2 = createURI("http://localhost/fcrepo/abc/fcr:version/2");
        final Node v3 = createURI("http://localhost/fcrepo/abc/fcr:version/3");
        final Instant date = now();

        final Graph mem = createDefaultModel().getGraph();
//        mem.add(new Triple(resource, HAS_VERSION.asNode(), v1));
//        mem.add(new Triple(v1, CREATED_DATE.asNode(), createLiteral(date.toString())));
//        mem.add(new Triple(resource, HAS_VERSION.asNode(), v2));
//        mem.add(new Triple(v2, CREATED_DATE.asNode(), createLiteral(date.toString())));
//        mem.add(new Triple(resource, HAS_VERSION.asNode(), v3));
        mem.add(new Triple(v3, CREATED_DATE.asNode(),
                createLiteral(date.plusMillis(10000l).toString())));

//        final Iterator<Node> versions = testObj.getOrderedVersions(mem, resource, HAS_VERSION);
//        versions.next();
//        versions.next();
//        final Node r3 = versions.next();
//        assertEquals("Latest version should be last.", v3, r3);
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
    public void testIsWritable() {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), WRITABLE.asNode(), createLiteral(Boolean.TRUE.toString())));
        assertTrue("Node is should be writable.", testObj.isWritable(mem, createURI("a/b/c")));
    }

    @Test
    public void testIsWritableFalse() {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), WRITABLE.asNode(), createLiteral(Boolean.FALSE.toString())));
        assertFalse("Node should not be writable.", testObj.isWritable(mem, createURI("a/b/c")));
    }

    @Test
    public void testIsWritableFalseJunk() {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), DESCRIBES.asNode(), createLiteral("junk")));
        assertFalse("Node should not be writable.", testObj.isWritable(mem, createURI("a/b/c")));
    }

    @Test
    public void testIsVersionedNode() {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), type.asNode(), createURI(REPOSITORY_NAMESPACE + "Version")));
        assertTrue("Node is a versioned node.", testObj.isVersionedNode(mem, createURI("a/b/c")));
    }

    @Test
    public void testRdfResource() {
        final String ns = "http://any/namespace#";
        final String rdfType = "anyType";
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b"), type.asNode(),
                createResource(ns + rdfType).asNode()));

        assertTrue("Node is a " + rdfType + " node.",
                testObj.isRdfResource(mem, createURI("a/b"), ns, rdfType));
        assertFalse("Node is not a " + rdfType + " node.",
                testObj.isRdfResource(mem, createURI("a/b"), ns, "otherType"));
    }

    @Test
    public void shouldFindVersionRoot() {

        final UriInfo mockUriInfo = getUriInfoImpl();

        final String nodeUri = testObj.getVersionSubjectUrl(mockUriInfo, createResource(
                        "http://localhost/fcrepo/a/b/fcr:versions/c").asNode());
        assertEquals("http://localhost/fcrepo/a/b", nodeUri);
    }

    @Test
    public void testGetVersionDate() {
        final Graph mem = createDefaultModel().getGraph();
        final String date = now().toString();
        mem.add(new Triple(createURI("a/b/c"), CREATED_DATE.asNode(),
                createLiteral(date)));
        assertEquals("Date should be available.", date, testObj.getVersionDate(mem, createURI("a/b/c")).get());
    }

    @Test
    public void testGetMissingVersionDate() {
        final Graph mem = createDefaultModel().getGraph();
        assertFalse("Date should not be available.", testObj.getVersionDate(mem, createURI("a/b/c")).isPresent());
    }

    @Test
    public void shouldExtractTitleFromNode() {
        shouldExtractTitleFromNode(title);
        shouldExtractTitleFromNode(DCTerms.title);
        shouldExtractTitleFromNode(label);
        shouldExtractTitleFromNode(prefLabel);
    }

    private void shouldExtractTitleFromNode( final Property property ) {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), property.asNode(), createLiteral("abc")));
        assertEquals("abc", testObj.getObjectTitle(mem, createURI("a/b/c")));
    }

    @Test
    public void shouldUseTheObjectUriIfATitleIsNotAvailable() {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), title.asNode(), createURI("d/e/f")));

        assertEquals("a/b/c", testObj.getObjectTitle(mem, createURI("a/b/c")));

    }

    @Test
    public void shouldUsetheBNodeIdIfItIsABNode() {
        final Graph mem = createDefaultModel().getGraph();
        final Node anon = createBlankNode();
        assertEquals(anon.getBlankNodeLabel(), testObj
                .getObjectTitle(mem, anon));
    }

    @Test
    public void shouldJustUseTheStringIfItIsALiteral() {
        final Graph mem = createDefaultModel().getGraph();
        final Node lit = createLiteral("xyz");
        assertEquals("\"xyz\"", testObj.getObjectTitle(mem, lit));
    }

    @Test
    public void shouldConvertRdfObjectsToStrings() {

        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("subject"), createURI("a/b/c"),
                NodeFactory.createLiteral("abc")));
        mem.add(new Triple(createURI("subject"),
                createURI("a-numeric-type"), createTypedLiteral(0).asNode()));
        mem.add(new Triple(createURI("subject"),
                createURI("an-empty-string"), createLiteral("")));
        mem.add(new Triple( createURI("subject"), createURI("a-uri"),
                createURI("some-uri")));

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

        final Iterator<Triple> iterator = model.getGraph().find(null, null, null);

        final List<Triple> sortedTriples =
            testObj.getSortedTriples(model, iterator);

        sortedTriples.get(0).matches(a.asNode(), propertyA.asNode(),
                literalA.asNode());
        sortedTriples.get(1).matches(a.asNode(), propertyA.asNode(),
                literalB.asNode());
        sortedTriples.get(2).matches(a.asNode(), propertyB.asNode(),
                literalA.asNode());
        sortedTriples.get(3).matches(a.asNode(), propertyC.asNode(),
                literalA.asNode());
        sortedTriples.get(4).matches(resourceB.asNode(),
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
    public void shouldConvertStringLiteralsToNodes() {
        final String uri = "fedora:resource";
        final Literal URIRES = ResourceFactory.createPlainLiteral(uri);
        assertEquals(URIRES.asNode(), testObj.asLiteralStringNode(uri));
    }

    @Test
    public void testGetNumChildren() {
        final Graph mem = createDefaultModel().getGraph();
        mem.add(new Triple(createURI("a/b/c"), CONTAINS.asNode(), createResource("a/b/c/1").asNode()));
        mem.add(new Triple(createURI("a/b/c"), CONTAINS.asNode(), createResource("a/b/c/2").asNode()));
        mem.add(new Triple(createURI("a/b/c"), CONTAINS.asNode(), createResource("a/b/c/3").asNode()));
        mem.add(new Triple(createURI("a/b/c"), CONTAINS.asNode(), createResource("a/b/c/4").asNode()));
        assertEquals(4, testObj.getNumChildren(mem, createURI("a/b/c")));
    }

    @Test
    public void testGetNumChildrenEmpty() {
        final Graph mem = createDefaultModel().getGraph();
        assertEquals(0, testObj.getNumChildren(mem, createURI("a/b/c")));
    }

    @Test
    public void testGetNumChildrenNone() {
        final Graph mem = createDefaultModel().getGraph();
        assertEquals(0, testObj.getNumChildren(mem, createURI("a/b/c")));
    }

}
