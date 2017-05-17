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

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.Link;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

import static java.util.Calendar.getInstance;
import static java.util.TimeZone.getTimeZone;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createLiteralByValue;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Durbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class FedoraRelaxedLdpIT extends AbstractResourceIT {

    private String forgedUsername = "forged-username";
    private Calendar forgedDate;

    public FedoraRelaxedLdpIT() {
        forgedDate = nowUTC();
        forgedDate.add(Calendar.YEAR, -20);
    }

    @Before
    public void switchToRelaxedMode() {
        System.setProperty(SERVER_MANAGED_PROPERTIES_MODE, "relaxed");
    }


    @Test
    public void testCreateResourceWithForgedCreationInformationIsAllowed() throws IOException, ParseException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check
        final String subjectURI;
        try (CloseableHttpResponse response = postResourceWithTTL(
                getTTLThatUpdatesServerManagedTriples(forgedUsername, forgedDate, null, null))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        try (CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(forgedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(forgedDate));
        }
    }

    @Test
    public void testUpdateNonRdfResourceWithForgedInformationIsAllowed() throws IOException, ParseException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (CloseableHttpResponse response = postBinaryResource("this is the binary")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final String describedByURI = subjectURI + "/fcr:metadata";

        try (CloseableHttpResponse response = putResourceWithTTL(describedByURI,
                getTTLThatUpdatesServerManagedTriples(forgedUsername, forgedDate, forgedUsername, forgedDate))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (CloseableDataset dataset = getDataset(new HttpGet(describedByURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(forgedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(forgedDate));
        }
    }

    @Test
    public void testUpdateResourceWithForgedCreationInformationIsDisallowed() throws IOException, ParseException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (CloseableHttpResponse response = createObject()) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        try (CloseableHttpResponse response = putResourceWithTTL(subjectURI,
                getTTLThatUpdatesServerManagedTriples(forgedUsername, null, null, null))) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }

        try (CloseableHttpResponse response = putResourceWithTTL(subjectURI,
                getTTLThatUpdatesServerManagedTriples(null, forgedDate, null, null))) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
        }

        try (CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustNotHave(CREATED_BY.asNode(), createLiteral(forgedUsername))
                    .mustNotHave(CREATED_DATE.asNode(), createDateTime(forgedDate));
        }
    }

    @Test
    public void testUpdateResourceWithForgedModificationInformationIsAllowed() throws IOException, ParseException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (CloseableHttpResponse response = createObject()) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        forgedDate = nowUTC();
        forgedDate.add(Calendar.MONTH, 1);

        try (CloseableHttpResponse response = putResourceWithTTL(subjectURI,
                getTTLThatUpdatesServerManagedTriples(null, null, forgedUsername, forgedDate))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(forgedUsername))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(forgedDate));
        }
    }

    /**
     * The behavior that is *probably* the most intuative is the one we're testing here.
     *
     * That behavior can be summarized as: if you create a child resource with an explicitly provided
     * creation date, in the absence of other, more recent (than the provided date) modifications to the
     * parent, the parent's modification date should be set to the creation date of the child.
     * @throws IOException
     */
    @Ignore
    @Test
    public void testTouchingParentByChildCreation() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final Calendar firstDate = nowUTC();
        firstDate.add(Calendar.YEAR, -20);
        final String firstUser = "first";

        final Calendar secondDate = nowUTC();
        secondDate.add(Calendar.YEAR, -15);
        final String secondUser = "second";

        // create a resource 20 years ago
        final String subjectURI;
        try (CloseableHttpResponse response = postResourceWithTTL(
                getTTLThatUpdatesServerManagedTriples(firstUser, firstDate, firstUser, firstDate))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        // add a child 15 years ago
        forgedDate.add(Calendar.YEAR, 5);
        forgedUsername = "child-of-" + forgedUsername;
        final String childURI = subjectURI + "/child";
        try (CloseableHttpResponse response = putResourceWithTTL(childURI,
                getTTLThatUpdatesServerManagedTriples(secondUser, secondDate, null, null))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertEquals(childURI, getLocation(response));
        }

        try (CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(firstUser))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(firstDate))
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(secondUser))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(secondDate));
        }

        try (CloseableDataset dataset = getDataset(new HttpGet(childURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(secondUser))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(secondDate))
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(secondUser))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(secondDate));
        }

    }

    /**
     * Tests a lossless roundtrip of a resource.  This method is written witout using PUT
     * because it may not be supported by the Fedora Spec.
     * @throws IOException
     */
    @Test
    public void testSimpleRoundtripping() throws IOException {
        assertEquals("relaxed", System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)); // sanity check

        final String subjectURI;
        try (CloseableHttpResponse response
                     = postResourceWithTTL("<> <http://purl.org/dc/elements/1.1/title> 'title' .")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final Header contentType;
        final String body;
        try (CloseableHttpResponse response = execute(new HttpGet(subjectURI))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            contentType = response.getFirstHeader("Content-Type");
            body = EntityUtils.toString(response.getEntity());
        }

        try (CloseableHttpResponse response = execute(new HttpDelete(subjectURI))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableHttpResponse r2 = execute(new HttpGet(subjectURI))) {
                final Link tombstone = Link.valueOf(r2.getFirstHeader(LINK).getValue());
                assertEquals("hasTombstone", tombstone.getRel());
                assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(tombstone.getUri())));
            }
        }

        final String rdf;
        try (CloseableDataset original = parseTriples(new ByteArrayInputStream(body.getBytes()))) {
            final Model m = original.getDefaultModel();
            final StmtIterator it = m.listStatements();
            while (it.hasNext()) {
                final Statement stmt = it.nextStatement();
                if (!(stmt.getPredicate().equals(CREATED_BY) || stmt.getPredicate().equals(CREATED_DATE)
                        || stmt.getPredicate().equals(LAST_MODIFIED_BY)
                        || stmt.getPredicate().equals(LAST_MODIFIED_DATE)
                        || stmt.getPredicate().getURI().equals("http://purl.org/dc/elements/1.1/title"))) {
                    it.remove();
                }
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.write(baos, "ttl");
            rdf = baos.toString();
        }

        final HttpPost post = new HttpPost(serverAddress);
        post.setHeader(contentType);
        post.setHeader("Slug", subjectURI.substring(serverAddress.length()));
        post.setEntity(new StringEntity(rdf));
        assertEquals(CREATED.getStatusCode(), getStatus(post));

        try (CloseableDataset roundtripped = getDataset(new HttpGet(subjectURI))) {
            try (CloseableDataset original = parseTriples(new ByteArrayInputStream(body.getBytes()))) {
                final DatasetGraph originalGraph = original.asDatasetGraph();
                final DatasetGraph roundtrippedGraph = roundtripped.asDatasetGraph();
                final Iterator<Quad> originalQuadIt = originalGraph.find();
                while (originalQuadIt.hasNext()) {
                    final Quad q = originalQuadIt.next();
                    assertTrue(q + " should be preserved through a roundtrip!", roundtrippedGraph.contains(q));
                    roundtrippedGraph.delete(q);
                }
                assertTrue("Roundtripped graph had extra quads!", roundtrippedGraph.isEmpty());
            }

        }

    }

    @After
    public void switchToStrictMode() {
        System.clearProperty(SERVER_MANAGED_PROPERTIES_MODE);
    }

    private CloseableHttpResponse postBinaryResource(final String content) throws IOException {
        final HttpPost post = postObjMethod("/");
        post.setEntity(new StringEntity(content == null ? "" : content));
        post.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        return execute(post);
    }

    private CloseableHttpResponse postResourceWithTTL(final String ttl) throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", getRandomUniqueId());
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.setEntity(new StringEntity(ttl));
        return execute(httpPost);
    }

    private CloseableHttpResponse putResourceWithTTL(final String uri, final String ttl)
            throws IOException {
        final HttpPut httpPut = new HttpPut(uri);
        httpPut.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        httpPut.addHeader(CONTENT_TYPE, "text/turtle");
        httpPut.setEntity(new StringEntity(ttl));
        return execute(httpPut);
    }

    private ResultGraphWrapper triples(final String uri, final Dataset dataset) throws IOException {
        return new ResultGraphWrapper(uri, dataset.asDatasetGraph());
    }

    private static Calendar nowUTC() {
        return getInstance(getTimeZone("UTC"));
    }

    private class ResultGraphWrapper {

        private DatasetGraph graph;
        private Node nodeUri;
        private String statements;

        public ResultGraphWrapper(final String subjectURI, final DatasetGraph graph) throws IOException {
            this.graph = graph;
            final Model model = createModelForGraph(graph.getDefaultGraph());
            nodeUri = createURI(subjectURI);

            final StringBuffer statements = new StringBuffer();
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
