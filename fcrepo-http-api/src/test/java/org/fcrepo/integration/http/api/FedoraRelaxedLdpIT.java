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
package org.fcrepo.integration.http.api;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.kernel.api.utils.GraphDifferencer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.Link;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Calendar.getInstance;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.StreamSupport.stream;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createLiteralByValue;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Durbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraRelaxedLdpIT extends AbstractResourceIT {

    private final String providedUsername = "provided-username";
    private Calendar providedDate;

    public FedoraRelaxedLdpIT() {
        providedDate = nowUTC();
        providedDate.add(Calendar.YEAR, -20);
    }

    @Before
    public void switchToRelaxedMode() {
        System.setProperty(SERVER_MANAGED_PROPERTIES_MODE, "relaxed");
    }


    @Ignore //TODO Fix this test
    @Test
    public void testBasicPutRoundtrip() throws IOException {
        final String subjectURI;
        try (final CloseableHttpResponse response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final String body;
        final HttpGet get = new HttpGet(subjectURI);
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            body = EntityUtils.toString(response.getEntity());
        }

        final HttpPut put = new HttpPut(subjectURI);
        put.setEntity(new StringEntity(body));
        put.setHeader(CONTENT_TYPE, "text/turtle");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(put));
    }

    @Ignore //TODO Fix this test
    @Test
    public void testCreateResourceWithSpecificCreationInformationIsAllowed() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check
        final String subjectURI;
        try (final CloseableHttpResponse response = postResourceWithTTL(
                getTTLThatUpdatesServerManagedTriples(providedUsername, providedDate, null, null))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(providedDate));
        }
    }

    @Ignore //TODO Fix this test
    @Test
    public void testUpdateNonRdfResourceWithSpecificInformationIsAllowed() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = postBinaryResource(serverAddress, "this is the binary")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final String describedByURI = subjectURI + "/fcr:metadata";

        try (final CloseableHttpResponse response = putResourceWithTTL(describedByURI,
                getTTLThatUpdatesServerManagedTriples(providedUsername, providedDate,
                                                      providedUsername, providedDate))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(describedByURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(providedDate))
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(providedDate));
        }
    }

    @Ignore //TODO Fix this test
    @Test
    public void testValidSparqlUpdate() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = postResourceWithTTL(
                getTTLThatUpdatesServerManagedTriples(providedUsername, providedDate, null, null))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final Calendar updatedDate = Calendar.getInstance(getTimeZone("UTC"));
        final String sparqlUpdate = "PREFIX fedora: <http://fedora.info/definitions/v4/repository#>\n" +
                "\n" +
                "DELETE { <> fedora:created \"" + DatatypeConverter.printDateTime(providedDate)
                        + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> }\n" +
                "INSERT { <> fedora:created \"" + DatatypeConverter.printDateTime(updatedDate)
                        + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> }\n" +
                "WHERE { }";
        try (final CloseableHttpResponse response = patchResource(subjectURI, sparqlUpdate)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(updatedDate));
        }
    }

    @Ignore //TODO Fix this test
    @Test
    public void testInvalidSparqlUpdate() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final Calendar updatedDate = Calendar.getInstance(getTimeZone("UTC"));
        final String sparqlUpdate = "PREFIX fedora: <http://fedora.info/definitions/v4/repository#>\n" +
                "\n" +
                "INSERT { <> fedora:created \"" + DatatypeConverter.printDateTime(updatedDate) +
                "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .\n" +
                " <> fedora:created \"" + DatatypeConverter.printDateTime(providedDate) +
                "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> }\n" +
                "WHERE { }";
        try (final CloseableHttpResponse response = patchResource(subjectURI, sparqlUpdate)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustNotHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustNotHave(CREATED_DATE.asNode(), createDateTime(updatedDate));
        }
    }

    @Ignore //TODO Fix this test
    @Test
    public void testUpdateResourceWithSpecificModificationInformationIsAllowed() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = createObject()) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        providedDate = nowUTC();
        providedDate.add(Calendar.MONTH, 1);

        try (final CloseableHttpResponse response = putResourceWithTTL(subjectURI,
                getTTLThatUpdatesServerManagedTriples(null, null, providedUsername, providedDate))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(providedDate));
        }
    }

    /**
     * Tests a lossless roundtrip of a resource.
     * @throws IOException if an error occurs while reading or writing to repository over HTTP
     */
    @Ignore //TODO Fix this test
    @Test
    public void testRoundtripping() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        // POST a resource with one user-managed triple
        final String containerURI;
        try (final CloseableHttpResponse response
                     = postResourceWithTTL("<> <http://purl.org/dc/elements/1.1/title> 'title' .")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containerURI = getLocation(response);
        }

        // POST a non-rdf child resource
        final String containedBinaryURI;
        final String containedBinaryDescriptionURI;
        try (final CloseableHttpResponse response = postBinaryResource(containerURI, "content")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containedBinaryURI = getLocation(response);
            containedBinaryDescriptionURI = containedBinaryURI + "/fcr:metadata";
        }

        // export the RDF of the container
        final String containerBody;
        try (final CloseableHttpResponse response = execute(new HttpGet(containerURI))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            containerBody = EntityUtils.toString(response.getEntity());
        }

        // export the RDF of the child
        final String containedBinaryDescriptionBody;
        try (final CloseableHttpResponse response = execute(new HttpGet(containedBinaryDescriptionURI))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            containedBinaryDescriptionBody = EntityUtils.toString(response.getEntity());
        }


        // delete the container and its tombstone
        try (final CloseableHttpResponse response = execute(new HttpDelete(containerURI))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableHttpResponse r2 = execute(new HttpGet(containerURI))) {
                final Link tombstone = Link.valueOf(r2.getFirstHeader(LINK).getValue());
                assertEquals("hasTombstone", tombstone.getRel());
                assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(tombstone.getUri())));
            }
        }


        // post the container from the export
        final String containerRdf = filterRdf(containerBody, CREATED_BY, CREATED_DATE, LAST_MODIFIED_BY,
                LAST_MODIFIED_DATE, createProperty("http://purl.org/dc/elements/1.1/title"));
        try (final CloseableHttpResponse response = postResourceWithTTL(containerURI, containerRdf)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        // post the binary then patch its metadata to match the export
        try (final CloseableHttpResponse response = postBinaryResource(containerURI,
                containedBinaryURI.substring(containerURI.length() + 1), "content")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
        final String sparqlUpdate;
        try (final CloseableDataset d = getDataset(new HttpGet(containedBinaryDescriptionURI))) {
            final Model model = createDefaultModel();
            model.read(new ByteArrayInputStream(containedBinaryDescriptionBody.getBytes()), "", "N3");
            final Stream<Statement> statements =
                    stream(spliteratorUnknownSize(d.getDefaultModel().listStatements(), IMMUTABLE), false);
            final GraphDifferencer diff = new GraphDifferencer(model, statements.map(Statement::asTriple));
            sparqlUpdate = buildSparqlUpdate(diff.difference(), diff.notCommon(), CREATED_BY, CREATED_DATE,
                    LAST_MODIFIED_BY, LAST_MODIFIED_DATE);
        }

        try (final CloseableHttpResponse response = patchResource(containedBinaryDescriptionURI, sparqlUpdate)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        // Because the creation of the contained resource resulted in an implicit modification to
        // the triples on the parent, we need to overwrite them again...  This is done doing a PUT
        // but could be achieved using a sparql update.
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(putResourceWithTTL(containerURI, containerRdf)));

        assertIdentical(containerURI, containerBody);
        assertIdentical(containedBinaryDescriptionURI, containedBinaryDescriptionBody);
    }

    @After
    public void switchToStrictMode() {
        System.clearProperty(SERVER_MANAGED_PROPERTIES_MODE);
    }

    private void assertIdentical(final String uri, final String originalRdf) throws IOException {
        try (final CloseableDataset roundtripped = getDataset(new HttpGet(uri))) {
            try (final CloseableDataset original = parseTriples(new ByteArrayInputStream(originalRdf.getBytes()))) {
                final DatasetGraph originalGraph = original.asDatasetGraph();
                final DatasetGraph roundtrippedGraph = roundtripped.asDatasetGraph();
                final Iterator<Quad> originalQuadIt = originalGraph.find();
                while (originalQuadIt.hasNext()) {
                    final Quad q = originalQuadIt.next();
                    assertTrue(q + " should be preserved through a roundtrip! \nOriginal RDF: " + originalRdf
                            + "\nRoundtripped Graph:\n" + roundtrippedGraph, roundtrippedGraph.contains(q));
                    roundtrippedGraph.delete(q);
                }
                assertTrue("Roundtripped graph had extra quads! " + roundtrippedGraph, roundtrippedGraph.isEmpty());
            }
        }
    }

    private String buildSparqlUpdate(final Stream<Triple> toRemove, final Stream<Triple> toAdd,
                                     final Property ... predicates) {
        final Set<Property> allowedPredicates = new HashSet<>(Arrays.asList(predicates));
        final StringBuilder r = new StringBuilder();
        r.append("DELETE { \n");
        toRemove.filter(t -> allowedPredicates.contains(createProperty(t.getPredicate().getURI())))
                .forEach(triple -> r.append(" <> <" + triple.getPredicate().toString() + "> "
                        + wrapLiteral(triple.getObject()) + " .\n"));
        r.append("}\nINSERT { ");
        toAdd.filter(t -> allowedPredicates.contains(createProperty(t.getPredicate().getURI())))
                .forEach(triple -> r.append(" <> <" + triple.getPredicate().toString() + "> "
                        + wrapLiteral(triple.getObject()) + " .\n"));
        r.append("} WHERE {}");
        return r.toString();
    }

    private String wrapLiteral(final Node node) {
        return "\"" + node.getLiteralLexicalForm() + "\"^^<" + node.getLiteralDatatype().getURI() + ">";
    }

    /**
     * Parses the provided triples and filters it to just include statements with the
     * specified predicates.
     * @param triples to filter
     * @param predicates to filter on
     * @return filtered triples
     */
    private String filterRdf(final String triples, final Property ... predicates) {
        try (final CloseableDataset original = parseTriples(new ByteArrayInputStream(triples.getBytes()))) {
            final Model m = original.getDefaultModel();
            final StmtIterator it = m.listStatements();
            while (it.hasNext()) {
                final Statement stmt = it.nextStatement();
                boolean keep = false;
                for (final Property p : predicates) {
                    if (stmt.getPredicate().equals(p)) {
                        keep = true;
                        break;
                    }
                }
                if (!keep) {
                    it.remove();
                }
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.write(baos, "N3");
            return baos.toString();
        }
    }

    private CloseableHttpResponse patchResource(final String uri, final String sparqlUpdate) throws IOException {
        final HttpPatch patch = new HttpPatch(uri);
        patch.setHeader("Content-type", "application/sparql-update");
        patch.setEntity(new StringEntity(sparqlUpdate));
        return execute(patch);
    }

    private CloseableHttpResponse postBinaryResource(final String parentURI, final String content) throws IOException {
        return postBinaryResource(parentURI, null, content);
    }

    private CloseableHttpResponse postBinaryResource(final String parentURI, final String slug,
                                                     final String content) throws IOException {
        final HttpPost post = new HttpPost(parentURI);
        if (slug != null) {
            post.setHeader("Slug", slug);
        }
        post.setEntity(new StringEntity(content == null ? "" : content));
        post.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        post.setHeader(LINK, "<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"");
        return execute(post);
    }

    private CloseableHttpResponse postResourceWithTTL(final String ttl) throws IOException {
        return this.postResourceWithTTL(null, ttl);
    }

    private CloseableHttpResponse postResourceWithTTL(final String uri, final String ttl) throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress);
        httpPost.addHeader("Slug", uri == null ? getRandomUniqueId() : uri.substring(serverAddress.length()));
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.addHeader(LINK, "<" + BASIC_CONTAINER.toString() + ">; rel=\"type\"");
        httpPost.setEntity(new StringEntity(ttl));
        return execute(httpPost);
    }

    private CloseableHttpResponse putResourceWithTTL(final String uri, final String ttl)
            throws IOException {
        final HttpPut httpPut = new HttpPut(uri);
        httpPut.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        httpPut.addHeader(CONTENT_TYPE, "text/turtle");
        httpPut.addHeader(LINK, "<" + BASIC_CONTAINER.toString() + ">; rel=\"type\"");
        httpPut.setEntity(new StringEntity(ttl));
        return execute(httpPut);
    }

    private ResultGraphWrapper triples(final String uri, final Dataset dataset) {
        return new ResultGraphWrapper(uri, dataset.asDatasetGraph());
    }

    private static Calendar nowUTC() {
        return getInstance(getTimeZone("UTC"));
    }

    private class ResultGraphWrapper {

        private final DatasetGraph graph;
        private final Node nodeUri;
        private final String statements;

        public ResultGraphWrapper(final String subjectURI, final DatasetGraph graph) {
            this.graph = graph;
            final Model model = createModelForGraph(graph.getDefaultGraph());
            nodeUri = createURI(subjectURI);

            final StringBuilder statements = new StringBuilder();
            final StmtIterator it = model.listStatements();
            while (it.hasNext()) {
                statements.append(it.next() + "\n");
            }
            this.statements = statements.toString();
        }

        public ResultGraphWrapper mustHave(final Node predicate, final Node object) {
            assertTrue(predicate.getLocalName() + " should have been set to " + object.getLiteral().toString()
                    + "!\nStatements:\n" + statements, graph.contains(ANY, nodeUri, predicate, object));
            return this;
        }

        public ResultGraphWrapper mustNotHave(final Node predicate, final Node object) {
            assertFalse(predicate.getLocalName() + " should not have been set to " + object.getLiteral().toString()
                    + "!", graph.contains(ANY, nodeUri, predicate, object));
            return this;
        }

    }

    private Node createDateTime(final Calendar cal) {
        return createLiteralByValue(DatatypeConverter.printDateTime(cal), XSDDatatype.XSDdateTime);
    }

}
