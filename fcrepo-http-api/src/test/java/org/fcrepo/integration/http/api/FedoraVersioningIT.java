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

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_MEMENTO_DATETIME;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Before;
import org.junit.Test;

/**
 * @author lsitu
 * @author bbpennel
 */
public class FedoraVersioningIT extends AbstractResourceIT {

    private static final String VERSIONED_RESOURCE_LINK_HEADER = "<" + VERSIONED_RESOURCE.getURI() + ">; rel=\"type\"";
    private static final String BINARY_CONTENT = "binary content";
    private static final String BINARY_UPDATED = "updated content";

    private static final String OCTET_STREAM_TYPE = "application/octet-stream";

    final String MEMENTO_DATETIME =
            RFC_1123_DATE_TIME.format(LocalDateTime.of(2000, 1, 1, 00, 00).atZone(ZoneOffset.UTC));

    private String subjectUri;
    private String id;

    @Before
    public void init() {
        id = getRandomUniqueId();
        subjectUri = serverAddress + id;
    }

    @Test
    public void testDeleteTimeMapForContainer() throws Exception {
        createVersionedContainer(id, subjectUri);
        // disabled versioning to delete TimeMap
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + id + "/" + FCR_VERSIONS)));
    }

    @Test
    public void testGetTimeMapResponse() throws Exception {
        createVersionedContainer(id, subjectUri);

        final List<Link> listLinks = new ArrayList<Link>();
        listLinks.add(Link.fromUri(subjectUri).rel("original").build());
        listLinks.add(Link.fromUri(subjectUri).rel("timegate").build());
        listLinks
            .add(Link.fromUri(subjectUri + "/" + FCR_VERSIONS).rel("self").type(APPLICATION_LINK_FORMAT).build());
        final Link[] expectedLinks = listLinks.toArray(new Link[3]);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);
        httpGet.setHeader("Accept", APPLICATION_LINK_FORMAT);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals("Didn't get a OK response!", OK.getStatusCode(), getStatus(response));
            checkForLinkHeader(response, VERSIONING_TIMEMAP_TYPE, "type");
            final List<String> bodyList = Arrays.asList(EntityUtils.toString(response.getEntity()).split(","));
            final Link[] bodyLinks = bodyList.stream().map(String::trim).filter(t -> !t.isEmpty())
                .map(Link::valueOf).toArray(Link[]::new);
            assertArrayEquals(expectedLinks, bodyLinks);
        }
    }

    @Test
    public void testGetTimeMapRDFSubject() throws Exception {
        createVersionedContainer(id, subjectUri);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);

        try (final CloseableDataset dataset = getDataset(httpGet)) {
            final DatasetGraph results = dataset.asDatasetGraph();
            final Node subject = createURI(subjectUri + "/" + FCR_VERSIONS);
            assertTrue("Did not find correct subject", results.contains(ANY, subject, ANY, ANY));
        }
    }

    @Test
    public void testCreateVersion() throws Exception {
        createVersionedContainer(id, subjectUri);

        final HttpPost postMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final String nowDateTime =
                    RFC_1123_DATE_TIME.format(Instant.now().atZone(ZoneOffset.UTC));
            assertMementoDatetimeHeaderMatches(response, nowDateTime);
        }
    }

    @Test
    public void testCreateVersionWithDatetime() throws Exception {
        createVersionedContainer(id, subjectUri);

        final String mementoUri = createMemento(subjectUri, MEMENTO_DATETIME, "text/n3", null);

        final HttpGet httpGet = new HttpGet(mementoUri);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            final CloseableDataset dataset = getDataset(response);
            final DatasetGraph results = dataset.asDatasetGraph();
            final Node subject = createURI(mementoUri);
            final Node property = createURI(FEDORA_MEMENTO_DATETIME);
            assertTrue("Did not find correct subject", results.contains(ANY, subject, ANY, ANY));
            assertFalse("Memento response must not include memento datetime",
                    results.contains(ANY, subject, property, ANY));
        }
    }

    /**
     * POST to create LDPCv without memento-datetime must ignore body
     */
    @Test
    public void testCreateVersionWithBody() throws Exception {
        createVersionedContainer(id, subjectUri);

        final String mementoUri = createContainerMementoWithBody(subjectUri, null);

        final HttpGet httpGet = new HttpGet(mementoUri);
        try (final CloseableDataset dataset = getDataset(httpGet)) {
            final DatasetGraph results = dataset.asDatasetGraph();

            final Node mementoSubject = createURI(mementoUri);
            final Node property = createURI("info:test#label");

            assertTrue("Memento created without must retain original state",
                    results.contains(ANY, mementoSubject, property, createLiteral("foo")));
            assertFalse("Memento created without datetime must ignore updates",
                    results.contains(ANY, mementoSubject, property, createLiteral("bar")));
        }
    }

    @Test
    public void testCreateVersionWithDatetimeAndBody() throws Exception {
        createVersionedContainer(id, subjectUri);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        final Node mementoSubject = createURI(mementoUri);
        final Node subject = createURI(subjectUri);
        final Node property = createURI("info:test#label");

        // Verify that the memento has the new property added to it
        try (final CloseableHttpResponse response = execute(new HttpGet(mementoUri))) {
            // Verify datetime was set correctly
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            final CloseableDataset dataset = getDataset(response);
            final DatasetGraph results = dataset.asDatasetGraph();

            assertTrue("Memento must have original property",
                    results.contains(ANY, mementoSubject, property, createLiteral("foo")));
            assertTrue("Memento must have updated property",
                    results.contains(ANY, mementoSubject, property, createLiteral("bar")));
        }

        // Verify that the original is unchanged
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            assertTrue("Original must have original property",
                    results.contains(ANY, subject, property, createLiteral("foo")));
            assertFalse("Original must not have updated property",
                    results.contains(ANY, subject, property, createLiteral("bar")));
        }
    }

    @Test
    public void testCreateVersionDuplicateMementoDatetime() throws Exception {
        createVersionedContainer(id, subjectUri);

        // Create first memento
        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);

        // Attempt to create second memento with same datetime, which should fail
        final HttpPost createVersionMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createVersionMethod.addHeader(CONTENT_TYPE, "text/n3");
        final String body = "<" + subjectUri + "> <info:test#label> \"far\"";
        createVersionMethod.setEntity(new StringEntity(body));
        createVersionMethod.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals("Duplicate memento datetime should return 412 status",
                    PRECONDITION_FAILED.getStatusCode(), getStatus(response));
        }

        final Node mementoSubject = createURI(mementoUri);
        final Node property = createURI("info:test#label");
        // Verify first memento content persists
        try (final CloseableDataset dataset = getDataset(new HttpGet(mementoUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            assertTrue("Memento must have first updated property",
                    results.contains(ANY, mementoSubject, property, createLiteral("bar")));
            assertFalse("Memento must not have second updated property",
                    results.contains(ANY, mementoSubject, property, createLiteral("far")));
        }
    }

    @Test
    public void testGetTimeMapResponseForBinary() throws Exception {
        createVersionedBinary(id);

        final List<Link> listLinks = new ArrayList<Link>();
        listLinks.add(Link.fromUri(subjectUri).rel("original").build());
        listLinks.add(Link.fromUri(subjectUri).rel("timegate").build());
        listLinks
                .add(Link.fromUri(subjectUri + "/" + FCR_VERSIONS).rel("self").type(APPLICATION_LINK_FORMAT).build());
        final Link[] expectedLinks = listLinks.toArray(new Link[3]);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);
        httpGet.setHeader("Accept", APPLICATION_LINK_FORMAT);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals("Didn't get a OK response!", OK.getStatusCode(), getStatus(response));
            checkForLinkHeader(response, VERSIONING_TIMEMAP_TYPE, "type");
            final List<String> bodyList = Arrays.asList(EntityUtils.toString(response.getEntity()).split(","));
            final Link[] bodyLinks = bodyList.stream().map(String::trim).filter(t -> !t.isEmpty())
                    .map(Link::valueOf).toArray(Link[]::new);
            assertArrayEquals(expectedLinks, bodyLinks);
        }
    }

    @Test
    public void testCreateVersionOfBinary() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, null, null, null);

        final String nowDateTime =
                RFC_1123_DATE_TIME.format(Instant.now().atZone(ZoneOffset.UTC));

        final HttpGet httpGet = new HttpGet(mementoUri);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertMementoDatetimeHeaderMatches(response, nowDateTime);

            assertEquals("Binary content of memento must match original content",
                    BINARY_CONTENT, EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testCreateVersionOfBinaryWithDatetime() throws Exception {
        createVersionedBinary(id);

        final HttpPost createVersionMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createVersionMethod.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals("Must reject memento creation without Content Type",
                    UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testCreateVersionOfBinaryWithDatetimeAndContentType() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, MEMENTO_DATETIME,
                OCTET_STREAM_TYPE, null);

        // Verify that the memento has the updated binary
        try (final CloseableHttpResponse response = execute(new HttpGet(mementoUri))) {
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            assertEquals("Binary content of memento must be empty",
                    "", EntityUtils.toString(response.getEntity()));
            assertEquals(OCTET_STREAM_TYPE, response.getFirstHeader(CONTENT_TYPE).getValue());
        }
    }

    @Test
    public void testCreateVersionOfBinaryWithBody() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, null,
                OCTET_STREAM_TYPE, BINARY_UPDATED);

        final HttpGet httpGet = new HttpGet(mementoUri);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertMementoDatetimeHeaderPresent(response);

            assertEquals("Binary content of memento must not have changed",
                    BINARY_CONTENT, EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testCreateVersionOfBinaryWithDatetimeAndBody() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, MEMENTO_DATETIME,
                OCTET_STREAM_TYPE, BINARY_UPDATED);

        // Verify that the memento has the updated binary
        try (final CloseableHttpResponse response = execute(new HttpGet(mementoUri))) {
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            assertEquals("Binary content of memento must match updated content",
                    BINARY_UPDATED, EntityUtils.toString(response.getEntity()));
        }
    }

    private String createMemento(final String subjectUri, final String mementoDateTime, final String contentType,
            final String body) throws Exception {
        final HttpPost createVersionMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        if (contentType != null) {
            createVersionMethod.addHeader(CONTENT_TYPE, contentType);
        }
        if (body != null) {
            createVersionMethod.setEntity(new StringEntity(body));
        }
        if (mementoDateTime != null) {
            createVersionMethod.addHeader(MEMENTO_DATETIME_HEADER, mementoDateTime);
        }

        // Create new memento of resource with updated body
        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertMementoDatetimeHeaderPresent(response);

            return EntityUtils.toString(response.getEntity());
        }
    }

    private String createContainerMementoWithBody(final String subjectUri, final String mementoDateTime)
            throws Exception {
        final String body = "<" + subjectUri + "> <info:test#label> \"bar\"";
        return createMemento(subjectUri, mementoDateTime, "text/n3", body);
    }

    private void createVersionedContainer(final String id, final String subjectUri) throws Exception {
        final HttpPost createMethod = postObjMethod();
        createMethod.addHeader("Slug", id);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity("<" + subjectUri + "> <info:test#label> \"foo\""));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
    }

    private void createVersionedBinary(final String id) throws Exception {
        final HttpPost createMethod = postObjMethod();
        createMethod.addHeader("Slug", id);
        createMethod.addHeader(CONTENT_TYPE, OCTET_STREAM_TYPE);
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity(BINARY_CONTENT));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
    }

    private static void assertMementoDatetimeHeaderPresent(final CloseableHttpResponse response) {
        assertNotNull("No memento datetime header set in response",
                response.getHeaders(MEMENTO_DATETIME_HEADER));
    }

    private static void assertMementoDatetimeHeaderMatches(final CloseableHttpResponse response,
            final String expected) {
        assertMementoDatetimeHeaderPresent(response);
        assertEquals("Response memento datetime did not match expected value",
                expected, response.getFirstHeader(MEMENTO_DATETIME_HEADER).getValue());
    }
}
