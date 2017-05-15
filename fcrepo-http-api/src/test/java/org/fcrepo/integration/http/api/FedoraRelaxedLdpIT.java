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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.DatasetGraph;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;

import static java.util.Calendar.getInstance;
import static java.util.TimeZone.getTimeZone;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createLiteralByValue;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;
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

    private final String forgedUsername = "forged-username";
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


    // Things to test
    public void testCreatingNodesWithoutTouchingParents() {

    }

    // test setting an old created and modified date then touching it and ensuring that the modified date and modified
    // were update

    // create something, change it's creation date, make an update, ensure that the creation date is correct.

    // create a container with explicit dates, create a child with explicit dates, make sure the parent's
    // modification date hasn't changed

    @After
    public void switchToStrictMode() {
        System.clearProperty(SERVER_MANAGED_PROPERTIES_MODE);
    }


    private CloseableHttpResponse postResourceWithTTL(final String ttl) throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", getRandomUniqueId());
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.setEntity(new StringEntity(ttl));
        return execute(httpPost);
    }

    private CloseableHttpResponse putResourceWithTTL(final String uri, final String ttl) throws IOException {
        final HttpPut httpPut = new HttpPut(uri);
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
