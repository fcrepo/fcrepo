/**
 * Copyright 2014 DuraSpace, Inc.
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
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.WRITABLE;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.RdfLexicon;
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
    public void testGetVersions() {
        final DatasetGraph mem = createMem();
        final Node anon = createAnon();
        final Node version = createURI("http://localhost/fcrepo/abc/fcr:version/adcd");
        final String date = new Date().toString();
        mem.add(anon, createURI("http://localhost/fcrepo/abc"), HAS_VERSION.asNode(),
                version);
        mem.add(anon, version, LAST_MODIFIED_DATE.asNode(),
                createLiteral(date));
        assertEquals("Version should be available.",
                     version, testObj.getVersions(mem, createURI("http://localhost/fcrepo/abc")).next());
    }

    @Test
    public void testGetOrderedVersions() {
        final Node resource = createURI("http://localhost/fcrepo/abc");
        final Node v1 = createURI("http://localhost/fcrepo/abc/fcr:version/1");
        final Node v2 = createURI("http://localhost/fcrepo/abc/fcr:version/2");
        final Node v3 = createURI("http://localhost/fcrepo/abc/fcr:version/3");
        final Date now = new Date();
        final Date later = new Date();
        later.setTime(later.getTime() + 10000l);

        final DatasetGraph mem = createMem();
        final Node anon = createAnon();
        mem.add(anon, resource, HAS_VERSION.asNode(), v1);
        mem.add(anon, v1, LAST_MODIFIED_DATE.asNode(),
                createLiteral(now.toString()));
        mem.add(anon, resource, HAS_VERSION.asNode(), v2);
        mem.add(anon, v2, LAST_MODIFIED_DATE.asNode(),
                createLiteral(now.toString()));
        mem.add(anon, resource, HAS_VERSION.asNode(), v3);
        mem.add(anon, v3, LAST_MODIFIED_DATE.asNode(),
                createLiteral(later.toString()));

        final Iterator<Node> versions = testObj.getOrderedVersions(mem, resource, HAS_VERSION);
        final Node r1 = versions.next();
        final Node r2 = versions.next();
        final Node r3 = versions.next();
        assertEquals("Latest version should be last.", v3, r3);
    }
    @Test
    public void testGetChildVersions() {
        final DatasetGraph mem = createMem();
        final Node anon = createAnon();
        final Node version = createURI("http://localhost/fcrepo/abc/fcr:version/adcd");
        final Node contentVersion = createURI("http://localhost/fcrepo/abc/fcr:version/adcd/fcr:content");
        final String date = new Date().toString();
        mem.add(anon, version, HAS_VERSION.asNode(),
                version);
        mem.add(anon, version, HAS_CONTENT.asNode(),
                contentVersion);
        mem.add(anon, contentVersion, LAST_MODIFIED_DATE.asNode(),
                createLiteral(date));
        assertEquals("Content version should be available.",
                     contentVersion, testObj.getChildVersions(mem, version).next());
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
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b/c"), WRITABLE.asNode(), createLiteral(Boolean.TRUE.toString()));
        assertTrue("Node is should be writable.", testObj.isWritable(mem, createURI("a/b/c")));
    }

    @Test
    public void testIsWritableFalse() {
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b/c"), WRITABLE.asNode(), createLiteral(Boolean.FALSE.toString()));
        assertFalse("Node should not be writable.", testObj.isWritable(mem, createURI("a/b/c")));
    }

    @Test
    public void testIsWritableFalseJunk() {
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b/c"), HAS_CONTENT.asNode(), createLiteral("junk"));
        assertFalse("Node should not be writable.", testObj.isWritable(mem, createURI("a/b/c")));
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
    public void testRdfResource() {
        final String ns = "http://any/namespace#";
        final String type = "anyType";
        final DatasetGraph mem = createMem();
        mem.add(createAnon(),
                createURI("a/b"),
                createResource(RDF_NAMESPACE + "type").asNode(),
                createResource(ns + type).asNode());

        assertTrue("Node is a " + type + " node.",
                testObj.isRdfResource(mem, createURI("a/b"), ns, type));
        assertFalse("Node is not a " + type + " node.",
                testObj.isRdfResource(mem, createURI("a/b"), ns, "otherType"));
    }

    @Test
    public void testGetLockUrl() {
        final Node lockUrl = createURI("a/b/fcr:lock");
        final DatasetGraph mem = createMem();
        mem.add(createAnon(), createURI("a/b"), RdfLexicon.HAS_LOCK.asNode(), lockUrl);

        assertEquals("Wrong lock url returned!", lockUrl.getURI(),
                testObj.getLockUrl(mem, createURI("a/b")));
    }

    @Test
    public void shouldFindVersionRoot() {

        final UriInfo mockUriInfo = getUriInfoImpl();

        final String nodeUri = testObj.getVersionSubjectUrl(mockUriInfo, createResource(
                        "http://localhost/fcrepo/a/b/fcr:versions/c").asNode());
        assertEquals("http://localhost/fcrepo/a/b", nodeUri);
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
        assertEquals("Default version label should be used.",
                     testObj.getVersionLabel(mem, createURI("a/b/c"), "default"), "default");
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
        assertEquals("Date should not be available.", testObj.getVersionDate(mem, createURI("a/b/c")), "");
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
    public void shouldGetSerializationFormat() {
        final Node subject = createURI("subject/fcr:export?format=jcr/xml");
        final DatasetGraph mem = createMem();

        mem.add(createAnon(), subject, createLiteral("predicate"),createLiteral("abc"));
        assertEquals("jcr/xml", testObj.getObjectTitle(mem, subject));
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
