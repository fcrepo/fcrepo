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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.http.api.FedoraLdp.ACCEPT_DATETIME;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_FIXITY;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.Link;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Before;
import org.junit.Test;

/**
 * @author lsitu
 * @author bbpennel
 */
public class FedoraVersioningIT extends AbstractResourceIT {

    public static final DateTimeFormatter MEMENTO_DATETIME_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("GMT"));

    private static final String VERSIONED_RESOURCE_LINK_HEADER = "<" + VERSIONED_RESOURCE.getURI() + ">; rel=\"type\"";
    private static final String BINARY_CONTENT = "binary content";
    private static final String BINARY_UPDATED = "updated content";

    private static final String OCTET_STREAM_TYPE = "application/octet-stream";

    private static final Node MEMENTO_TYPE_NODE = createURI(MEMENTO_TYPE);
    private static final Node TEST_PROPERTY_NODE = createURI("info:test#label");

    private static final Property TEST_PROPERTY = createProperty("info:test#label");

    private final String MEMENTO_DATETIME =
            RFC_1123_DATE_TIME.format(LocalDateTime.of(2000, 1, 1, 00, 00).atZone(ZoneOffset.UTC));
    private final List<String> rdfTypes = new ArrayList<>(Arrays.asList(POSSIBLE_RDF_RESPONSE_VARIANTS_STRING));

    private String subjectUri;
    private String id;

    @Before
    public void init() {
        id = getRandomUniqueId();
        subjectUri = serverAddress + id;
    }

    @Test
    public void testDeleteTimeMapForContainer() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createMemento(subjectUri, null, null, null);
        assertEquals(200, getStatus(new HttpGet(mementoUri)));

        final String timeMapUri = subjectUri + "/" + FCR_VERSIONS;
        assertEquals(200, getStatus(new HttpGet(timeMapUri)));

        // disabled versioning to delete TimeMap
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + id + "/" + FCR_VERSIONS)));

        // validate that the memento version is gone
        assertEquals(404, getStatus(new HttpGet(mementoUri)));
        // validate that the LDPCv is gone
        assertEquals(404, getStatus(new HttpGet(timeMapUri)));
    }

    @Test
    public void testGetTimeMapResponse() throws Exception {
        createVersionedContainer(id);

        createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        verifyTimemapResponse(subjectUri, id, MEMENTO_DATETIME);
    }

    @Test
    public void testGetTimeMapResponseMultipleMementos() throws Exception {
        createVersionedContainer(id);
        final String memento1 =
                RFC_1123_DATE_TIME.format(LocalDateTime.of(2000, 1, 1, 00, 00, 00).atOffset(ZoneOffset.UTC));
        final String memento2 =
                RFC_1123_DATE_TIME.format(LocalDateTime.of(2015, 8, 13, 18, 30, 0).atOffset(ZoneOffset.UTC));
        final String memento3 =
                RFC_1123_DATE_TIME.format(LocalDateTime.of(1980, 5, 31, 9, 15, 30).atOffset(ZoneOffset.UTC));
        createContainerMementoWithBody(subjectUri, memento1);
        createContainerMementoWithBody(subjectUri, memento2);
        createContainerMementoWithBody(subjectUri, memento3);
        final String[] mementos = { memento1, memento2, memento3 };
        verifyTimemapResponse(subjectUri, id, mementos, memento3, memento2);
    }

    @Test
    public void testGetTimeMapRDFSubject() throws Exception {
        createVersionedContainer(id);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);

        try (final CloseableDataset dataset = getDataset(httpGet)) {
            final DatasetGraph results = dataset.asDatasetGraph();
            final Node subject = createURI(subjectUri + "/" + FCR_VERSIONS);
            assertTrue("Did not find correct subject", results.contains(ANY, subject, ANY, ANY));
        }
    }

    @Test
    public void testCreateVersion() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, null);
        assertMementoUri(mementoUri, subjectUri);

        try (final CloseableDataset dataset = getDataset(new HttpGet(mementoUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            final Node mementoSubject = createURI(mementoUri);

            assertFalse("Memento type should not be visible",
                    results.contains(ANY, mementoSubject, RDF.type.asNode(), MEMENTO_TYPE_NODE));
        }
    }

    @Test
    public void testCreateVersionWithSlugHeader() throws Exception {
        createVersionedContainer(id);

        // Bad request with Slug header to create memento
        final String mementoDateTime = "Tue, 3 Jun 2008 11:05:30 GMT";
        final String body = createContainerMementoBodyContent(subjectUri, N3);
        final HttpPost post = postObjMethod(id + "/" + FCR_VERSIONS);

        post.addHeader("Slug", "version_label");
        post.addHeader(MEMENTO_DATETIME_HEADER, mementoDateTime);
        post.addHeader(CONTENT_TYPE, N3);
        post.setEntity(new StringEntity(body));

        assertEquals("Created memento with Slug!",
                BAD_REQUEST.getStatusCode(), getStatus(post));
    }

    @Test
    public void testCreateVersionWithMementoDatetimeFormat() throws Exception {
        createVersionedContainer(id);

        // Create memento with RFC-1123 date-time format
        final String mementoDateTime = "Tue, 3 Jun 2008 11:05:30 GMT";
        final String body = createContainerMementoBodyContent(subjectUri, N3);

        final HttpPost post = postObjMethod(id + "/" + FCR_VERSIONS);

        post.addHeader(MEMENTO_DATETIME_HEADER, mementoDateTime);
        post.addHeader(CONTENT_TYPE, N3);
        post.setEntity(new StringEntity(body));

        assertEquals("Unable to create memento with RFC-1123 date-time format!",
                CREATED.getStatusCode(), getStatus(post));

        // Create memento with RFC-1123 date-time format in wrong value
        final String dateTime1 = "Tue, 13 Jun 2008 11:05:35 ANYTIMEZONE";
        final HttpPost post1 = postObjMethod(id + "/" + FCR_VERSIONS);

        post1.addHeader(CONTENT_TYPE, N3);
        post1.setEntity(new StringEntity(body));
        post1.addHeader(MEMENTO_DATETIME_HEADER, dateTime1);

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(post1));

        // Create memento in date-time format other than RFC-1123
        final String dateTime2 = "2000-01-01T01:01:01.11Z";
        final HttpPost post2 = postObjMethod(id + "/" + FCR_VERSIONS);

        post2.addHeader(CONTENT_TYPE, N3);
        post2.setEntity(new StringEntity(body));
        post2.addHeader(MEMENTO_DATETIME_HEADER, dateTime2);

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(post2));
    }

    @Test
    public void testCreateVersionWithDatetime() throws Exception {
        createVersionedContainer(id);

        final HttpPost createVersionMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createVersionMethod.addHeader(CONTENT_TYPE, N3);
        createVersionMethod.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        // Attempt to create memento with no body
        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(), getStatus(response));

            // Request must fail with constrained exception due to empty body
            assertConstrainedByPresent(response);
        }
    }

    @Test
    public void testCreateContainerWithoutServerManagedTriples() throws Exception {
        createVersionedContainer(id);

        final HttpPost createMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createMethod.addHeader(CONTENT_TYPE, N3);
        createMethod.setEntity(new StringEntity("<" + subjectUri + "> <info:test#label> \"part\""));
        createMethod.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        // Attempt to create memento with partial record
        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(), getStatus(response));

            // Request must fail with constrained exception due to empty body
            assertConstrainedByPresent(response);
        }
    }

    /**
     * POST to create LDPCv without memento-datetime must ignore body
     *
     * @throws Exception in case of error with test
     */
    @Test
    public void testCreateVersionWithBody() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, null);
        assertMementoUri(mementoUri, subjectUri);

        final HttpGet httpGet = new HttpGet(mementoUri);
        try (final CloseableDataset dataset = getDataset(httpGet)) {
            final DatasetGraph results = dataset.asDatasetGraph();

            final Node mementoSubject = createURI(mementoUri);

            assertTrue("Memento created without datetime must retain original state",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("foo")));
            assertFalse("Memento created without datetime must ignore updates",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("bar")));
        }
    }

    @Test
    public void testCreateVersionWithDatetimeAndBody() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        assertMementoUri(mementoUri, subjectUri);
        final Node mementoSubject = createURI(mementoUri);
        final Node subject = createURI(subjectUri);

        // Verify that the memento has the new property added to it
        try (final CloseableHttpResponse response = execute(new HttpGet(mementoUri))) {
            // Verify datetime was set correctly
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            final CloseableDataset dataset = getDataset(response);
            final DatasetGraph results = dataset.asDatasetGraph();

            assertFalse("Memento must not have original property",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("foo")));
            assertTrue("Memento must have updated property",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("bar")));
        }

        // Verify that the original is unchanged
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            assertTrue("Original must have original property",
                    results.contains(ANY, subject, TEST_PROPERTY_NODE, createLiteral("foo")));
            assertFalse("Original must not have updated property",
                    results.contains(ANY, subject, TEST_PROPERTY_NODE, createLiteral("bar")));
        }
    }

    @Test
    public void testCreateVersionDuplicateMementoDatetime() throws Exception {
        createVersionedContainer(id);

        // Create first memento
        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);

        // Attempt to create second memento with same datetime, which should fail
        final HttpPost createVersionMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createVersionMethod.addHeader(CONTENT_TYPE, N3);
        final String body = "<" + subjectUri + "> <info:test#label> \"far\"";
        createVersionMethod.setEntity(new StringEntity(body));
        createVersionMethod.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals("Duplicate memento datetime should return 409 status",
                    CONFLICT.getStatusCode(), getStatus(response));
        }

        final Node mementoSubject = createURI(mementoUri);
        // Verify first memento content persists
        try (final CloseableDataset dataset = getDataset(new HttpGet(mementoUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            assertTrue("Memento must have first updated property",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("bar")));
            assertFalse("Memento must not have second updated property",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("far")));
        }
    }

    @Test
    public void testDeleteAndPostContainerMemento() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);

        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(mementoUri)));

        assertEquals("Deleted memento must be removed",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(mementoUri)));

        final String recreatedUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        assertEquals("Recreated memento must exist",
                OK.getStatusCode(), getStatus(new HttpGet(recreatedUri)));
    }

    @Test
    public void testDeleteAndPostBinaryMemento() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createLDPNRMementoWithExistingBody(MEMENTO_DATETIME);

        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(mementoUri)));

        assertEquals("Deleted memento must be removed",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(mementoUri)));

        final String recreatedUri = createLDPNRMementoWithExistingBody(MEMENTO_DATETIME);
        assertEquals("Recreated memento must exist",
                OK.getStatusCode(), getStatus(new HttpGet(recreatedUri)));
    }

    @Test
    public void testDeleteAndPostDescriptionMemento() throws Exception {
        createVersionedBinary(id);

        final String descId = id + "/" + FCR_METADATA;
        final String descUri = serverAddress + descId;
        final String mementoUri = createMementoWithExistingBody(descId, MEMENTO_DATETIME, false);

        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(mementoUri)));

        assertEquals("Deleted memento must be removed",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(mementoUri)));

        final String recreatedUri = createContainerMementoWithBody(descUri, MEMENTO_DATETIME);
        assertEquals("Recreated memento must exist",
                OK.getStatusCode(), getStatus(new HttpGet(recreatedUri)));
    }

    @Test
    public void testMementoContainmentReferences() throws Exception {
        createVersionedContainer(id);

        final String childUri = subjectUri + "/x";
        createObjectAndClose(id + "/x");

        // create memento
        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);

        // Remove the child resource
        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(childUri)));

        // Ensure that the resource reference is gone
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(subjectUri));
                final CloseableDataset dataset = getDataset(getResponse1);) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse("Expected NOT to have child resource: " + graph, graph.contains(ANY,
                    ANY, createURI(CONTAINS.getURI()), createURI(childUri)));
        }

        // Ensure that the resource reference is still in memento
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(mementoUri));
                final CloseableDataset dataset = getDataset(getResponse1);) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Expected child resource NOT found: " + graph, graph.contains(ANY,
                    ANY, createURI(CONTAINS.getURI()), createURI(childUri)));
        }
    }

    @Test
    public void testMementoExternalReference() throws Exception {
        createVersionedContainer(id);

        final String pid = getRandomUniqueId();
        final String resource = serverAddress + pid;
        createObjectAndClose(pid);

        final HttpPatch updateObjectGraphMethod = patchObjMethod(id);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT {"
                + " <> <http://pcdm.org/models#hasMember> <" + resource + "> } WHERE {}"));
        executeAndClose(updateObjectGraphMethod);

        // create memento
        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);

        // Remove the referencing resource
        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(resource)));

        // Ensure that the resource reference is gone
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(subjectUri));
                final CloseableDataset dataset = getDataset(getResponse1);) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse("Expected NOT to have resource: " + graph, graph.contains(ANY,
                    ANY, createURI("http://pcdm.org/models#hasMember"), createURI(resource)));
        }

        // Ensure that the resource reference is still in memento
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(mementoUri));
                final CloseableDataset dataset = getDataset(getResponse1);) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Expected resource NOT found: " + graph, graph.contains(ANY,
                    ANY, createURI("http://pcdm.org/models#hasMember"), createURI(resource)));
        }
    }

    @Test
    public void testPutOnTimeMapContainer() throws Exception {
        createVersionedContainer(id);

        // status 405: PUT On LPDCv is disallowed.
        assertEquals(405, getStatus(new HttpPut(serverAddress + id + "/" + FCR_VERSIONS)));
    }

    @Test
    public void testPatchOnTimeMapContainer() throws Exception {
        createVersionedContainer(id);

        // status 405: PATCH On LPDCv is disallowed.
        assertEquals(405, getStatus(new HttpPatch(serverAddress + id + "/" + FCR_VERSIONS)));
    }

    @Test
    public void testGetTimeMapResponseForBinary() throws Exception {
        createVersionedBinary(id);

        verifyTimemapResponse(subjectUri, id);
    }

    @Test
    public void testGetTimeMapResponseWithBadAcceptHeader() throws Exception {
        createVersionedContainer(id);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);
        httpGet.setHeader("Accept", "application/arbitrary");
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals("Should get a 'Not Acceptable' response!", NOT_ACCEPTABLE.getStatusCode(), getStatus(
                    response));
        }

    }

    @Test
    public void testGetTimeMapResponseForBinaryDescription() throws Exception {
        createVersionedBinary(id);

        final String descriptionUri = subjectUri + "/fcr:metadata";
        final String descriptionId = id + "/fcr:metadata";

        verifyTimemapResponse(descriptionUri, descriptionId);
    }

    private void verifyTimemapResponse(final String uri, final String id) throws Exception {
        verifyTimemapResponse(uri, id, null, null, null);
    }

    private void verifyTimemapResponse(final String uri, final String id, final String mementoDateTime)
            throws Exception {
        final String[] mementoDateTimes = { mementoDateTime };
        verifyTimemapResponse(uri, id, mementoDateTimes, null, null);
    }

    /**
     * Verify an application/link-format TimeMap response.
     *
     * @param uri The full URI of the Original Resource.
     * @param id The path of the Original Resource.
     * @param mementoDateTime Array of all the RFC-1123 datetimes for all the mementos.
     * @param rangeStart RFC-1123 datetime of the first memento.
     * @param rangeEnd RFC-1123 datetime of the last memento.
     * @throws Exception
     */
    private void verifyTimemapResponse(final String uri, final String id, final String[] mementoDateTime,
            final String rangeStart, final String rangeEnd)
            throws Exception {
        final String ldpcvUri = uri + "/" + FCR_VERSIONS;
        final List<Link> listLinks = new ArrayList<>();
        listLinks.add(Link.fromUri(uri).rel("original").build());
        listLinks.add(Link.fromUri(uri).rel("timegate").build());

        final javax.ws.rs.core.Link.Builder selfLink = Link.fromUri(ldpcvUri).rel("self").type(
                APPLICATION_LINK_FORMAT);
        if (rangeStart != null && rangeEnd != null) {
            selfLink.param("from", rangeStart).param("until",
                    rangeEnd);
        }
        listLinks.add(selfLink.build());
        if (mementoDateTime != null) {
            for (final String memento : mementoDateTime) {
                final TemporalAccessor instant = RFC_1123_DATE_TIME.parse(memento);
                listLinks.add(Link.fromUri(ldpcvUri + "/" + MEMENTO_DATETIME_ID_FORMATTER.format(instant))
                        .rel("memento")
                        .param("datetime", memento)
                        .build());
            }
        }

        final Link[] expectedLinks = listLinks.stream()
                .sorted((a, b) -> a.toString().compareTo(b.toString()))
                .toArray(Link[]::new);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);
        httpGet.setHeader("Accept", APPLICATION_LINK_FORMAT);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals("Didn't get a OK response!", OK.getStatusCode(), getStatus(response));
            // verify headers in link format.
            checkForLinkHeader(response, RESOURCE.toString(), "type");
            checkForLinkHeader(response, RDF_SOURCE.toString(), "type");
            checkForLinkHeader(response, uri, "original");
            checkForLinkHeader(response, uri, "timegate");
            checkForLinkHeader(response, uri + "/" + FCR_VERSIONS, "timemap");
            checkForLinkHeader(response, VERSIONING_TIMEMAP_TYPE, "type");
            checkForLinkHeader(response, ldpcvUri + "/" + FCR_ACL, "acl");
            final List<String> bodyList = Arrays.asList(EntityUtils.toString(response.getEntity()).split(",\n"));
            // the links from the body are not

            final Link[] bodyLinks = bodyList.stream().map(String::trim).filter(t -> !t.isEmpty())
                    .sorted((a, b) -> a.toString().compareTo(b.toString()))
                    .map(Link::valueOf).toArray(Link[]::new);
            assertArrayEquals(expectedLinks, bodyLinks);
        }
    }

    @Test
    public void testCreateVersionOfBinary() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, null, null, null);
        assertMementoUri(mementoUri, subjectUri);

        final HttpGet httpGet = new HttpGet(mementoUri);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertMementoDatetimeHeaderPresent(response);
            assertEquals("Binary content of memento must match original content",
                    BINARY_CONTENT, EntityUtils.toString(response.getEntity()));
        }

        // Verifying that the associated description memento was created
        final String descriptionMementoUri = mementoUri.replace("fcr:versions", "fcr:metadata/fcr:versions");

        final HttpGet descGet = new HttpGet(descriptionMementoUri);
        try (final CloseableHttpResponse response = execute(descGet)) {
            assertMementoDatetimeHeaderPresent(response);
            assertHasLink(response, type, RDF_SOURCE.getURI());
        }
    }

    @Test
    public void testCreateVersionOfBinaryWithDatetimeAndContentType() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, MEMENTO_DATETIME,
                OCTET_STREAM_TYPE, null);
        assertMementoUri(mementoUri, subjectUri);

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
        assertMementoUri(mementoUri, subjectUri);

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
                "text/plain", BINARY_UPDATED);
        assertMementoUri(mementoUri, subjectUri);

        // Verify that the memento has the updated binary
        try (final CloseableHttpResponse response = execute(new HttpGet(mementoUri))) {
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            // Content-type is not retained for a binary memento created without description
            assertEquals(OCTET_STREAM_TYPE, response.getFirstHeader(CONTENT_TYPE).getValue());

            assertEquals("Binary content of memento must match updated content",
                    BINARY_UPDATED, EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testCreateVersionOfBinaryDescription() throws Exception {
        createVersionedBinary(id);

        final String descriptionUri = subjectUri + "/fcr:metadata";

        final String mementoUri = createContainerMementoWithBody(descriptionUri, null);
        assertMementoUri(mementoUri, descriptionUri);

        setDescriptionProperty(id, null, DC.title.getURI(), "Updated");

        try (final CloseableDataset dataset = getDataset(new HttpGet(mementoUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            final Node mementoSubject = createURI(subjectUri);

            assertFalse("Property added to original must not appear in memento",
                    results.contains(ANY, mementoSubject, DC.title.asNode(), ANY));
            assertFalse("Memento type should not be visible",
                    results.contains(ANY, mementoSubject, RDF.type.asNode(), MEMENTO_TYPE_NODE));
            assertFalse("Property added to original must not appear in memento",
                    results.contains(ANY, mementoSubject, DC.title.asNode(), ANY));
        }

        // No binary memento should be created when specifically creating a description memento.
        final String hypotheticalBinaryUri = mementoUri.replaceAll("fcr:metadata/fcr:versions", "fcr:versions");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(new HttpGet(hypotheticalBinaryUri)));
    }

    /*
     * Attempt to create binary description with container triples
     */
    @Test
    public void testCreateVersionOfBinaryDescriptionInvalidTriples() throws Exception {
        final String containerId = getRandomUniqueId();
        final String containerSubjectUri = serverAddress + containerId;
        createObjectAndClose(containerId);

        createVersionedBinary(id);

        final String descriptionUri = subjectUri + "/fcr:metadata";

        final String containerBody = createContainerMementoBodyContent(containerSubjectUri, "text/n3");
        final HttpPost createMethod = postObjMethod(descriptionUri);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.setEntity(new StringEntity(containerBody));

        // Attempt to create memento with partial record
        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testCreateVersionBinaryDescriptionWithBodyAndDatetime() throws Exception {
        createVersionedBinary(id);

        final String descriptionUri = subjectUri + "/fcr:metadata";

        final String mementoUri = createContainerMementoWithBody(descriptionUri, MEMENTO_DATETIME);
        assertMementoUri(mementoUri, descriptionUri);

        try (final CloseableDataset dataset = getDataset(new HttpGet(mementoUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            final Node mementoSubject = createURI(mementoUri);

            assertFalse("Memento type should not be visible",
                    results.contains(ANY, mementoSubject, RDF.type.asNode(), MEMENTO_TYPE_NODE));
        }
    }

    @Test
    public void testCreateVersionHistoricBinaryAndDescription() throws Exception {
        createVersionedBinary(id, "text/plain", BINARY_CONTENT);

        final String descriptionUri = subjectUri + "/fcr:metadata";

        final String binaryMementoUri = createMemento(subjectUri, MEMENTO_DATETIME,
                null, "content");
        assertMementoUri(binaryMementoUri, subjectUri);

        final String mementoUri = createContainerMementoWithBody(descriptionUri, MEMENTO_DATETIME);
        assertMementoUri(mementoUri, descriptionUri);

        try (final CloseableDataset dataset = getDataset(new HttpGet(mementoUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();

            final Node mementoSubject = createURI(subjectUri);

            assertTrue("Type must be a fedora:Binary",
                    results.contains(ANY, mementoSubject, RDF.type.asNode(), FEDORA_BINARY.asNode()));
            assertTrue("Memento must have first updated property",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("bar")));
        }

        // Verify that the memento has the updated binary
        try (final CloseableHttpResponse response = execute(new HttpGet(binaryMementoUri))) {
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            assertEquals("Binary content of memento must reflect historic version",
                    "content", EntityUtils.toString(response.getEntity()));
            assertEquals("text/plain", response.getFirstHeader(CONTENT_TYPE).getValue());
        }
    }

    @Test
    public void testGetUnversionedObjectVersionProfile() throws Exception {
        final String containerUri = serverAddress + id;

        createObject(id);

        final HttpGet getVersion = new HttpGet(containerUri + "/" + FCR_VERSIONS);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getVersion));

        // Verify original has no memento headers
        assertNoMementoHeaders(containerUri);
    }

    @Test
    public void testGetUnversionedBinaryAndDescriptionVersionProfile() throws Exception {
        createDatastream(id, "ds", "content");

        final String binaryUri = serverAddress + id + "/ds";
        final HttpGet getBinary = new HttpGet(binaryUri + "/" + FCR_VERSIONS);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getBinary));

        // Verify original binary has no memento headers
        assertNoMementoHeaders(binaryUri);

        final String metadataUri = binaryUri + "/" + FCR_METADATA;
        final HttpGet getDesc = new HttpGet(metadataUri + "/" + FCR_VERSIONS);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getDesc));

        // Verify original has no memento headers
        assertNoMementoHeaders(metadataUri);
    }

    private void assertNoMementoHeaders(final String uri) throws Exception {
        final HttpGet getOriginal = new HttpGet(uri);

        try (final CloseableHttpResponse response = execute(getOriginal)) {
            assertEquals(OK.getStatusCode(), getStatus(response));

            assertNoLinkHeader(response, MEMENTO_TYPE, "type");
            assertNoLinkHeader(response, uri, "original");
            assertNoMementoDatetimeHeaderPresent(response);
        }
    }

    @Test
    public void testAddAndRetrieveVersion() throws Exception {
        createVersionedContainer(id);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + id, title.getURI(), "First Title");

        try (final CloseableDataset dataset = getContent(serverAddress + id)) {
            assertTrue("Should find original title", dataset.asDatasetGraph().contains(ANY,
                    ANY, title.asNode(), createLiteral("First Title")));
        }
        logger.debug("Posting version v0.0.1");
        final String mementoUri = createContainerMementoWithBody(subjectUri, null);
        assertMementoUri(mementoUri, subjectUri);

        logger.debug("Replacing the title");
        patchLiteralProperty(serverAddress + id, title.getURI(), "Second Title");

        try (final CloseableDataset dataset = getContent(mementoUri)) {
            logger.debug("Got version profile:");
            final DatasetGraph versionResults = dataset.asDatasetGraph();

            assertTrue("Should find a title in historic version", versionResults.contains(ANY,
                    ANY, title.asNode(), ANY));
            assertTrue("Should find original title in historic version", versionResults.contains(ANY,
                    ANY, title.asNode(), createLiteral("First Title")));
            assertFalse("Should not find the updated title in historic version", versionResults.contains(ANY,
                    ANY, title.asNode(), createLiteral("Second Title")));
        }
    }

    @Test
    public void testInvalidVersionDatetime() throws Exception {
        final String invalidDate = "blah";
        createVersionedContainer(id);

        // Create memento body
        final String body = createContainerMementoBodyContent(subjectUri, N3);

        final HttpPost postReq = postObjMethod(serverAddress + id + "/" + FCR_VERSIONS);
        postReq.addHeader(MEMENTO_DATETIME_HEADER, invalidDate);
        postReq.addHeader(CONTENT_TYPE, N3);
        postReq.setEntity(new StringEntity(body));

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(postReq));
    }

    @Test
    public void testEnableVersioning() throws Exception {
        final String containerUri = serverAddress + id;
        createObjectAndClose(id);

        final String versionsUri = containerUri + "/" + FCR_VERSIONS;
        assertEquals("fcr:versions must not exist before enabling versioning",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(versionsUri)));

        // Enable versioning
        enableVersioning(containerUri);

        assertEquals("fcr:versions must be available after enabling versioning",
                OK.getStatusCode(), getStatus(new HttpGet(versionsUri)));

        final String mementoUri = createMemento(subjectUri, null, null, null);
        assertEquals("Memento must be created",
                OK.getStatusCode(), getStatus(new HttpGet(mementoUri)));
    }

    @Test
    public void testEnableVersioningBinary() throws Exception {
        final String binaryUri = serverAddress + id + "/ds";
        createDatastream(id, "ds", "content");

        final String versionsUri = binaryUri + "/" + FCR_VERSIONS;
        assertEquals("fcr:versions must not exist before enabling versioning",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(versionsUri)));

        final String descUri = serverAddress + id + "/ds/" + FCR_METADATA;
        final String versionsDescUri = descUri + "/" + FCR_VERSIONS;
        assertEquals("fcr:metadata/fcr:versions must not exist before enabling versioning",
                NOT_FOUND.getStatusCode(), getStatus(new HttpGet(versionsDescUri)));

        // Enable versioning
        enableVersioning(binaryUri);

        assertEquals("fcr:versions must be available after enabling versioning",
                OK.getStatusCode(), getStatus(new HttpGet(versionsUri)));

        assertEquals("fcr:metadata/fcr:versions must be available after enabling versioning",
                OK.getStatusCode(), getStatus(new HttpGet(versionsDescUri)));

        final String mementoUri = createMemento(subjectUri, null, null, null);
        assertEquals("Memento must be created",
                OK.getStatusCode(), getStatus(new HttpGet(mementoUri)));
    }

    @Test
    public void testTimeMapResponseContentTypes() throws Exception {
        createVersionedContainer(id);

        final String[] timeMapResponseTypes = getTimeMapResponseTypes();
        for (final String type : timeMapResponseTypes) {
            final HttpGet method = new HttpGet(serverAddress + id + "/fcr:versions");
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testGetVersionResponseContentTypes() throws Exception {
        createVersionedContainer(id);
        final String versionUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);

        final String[] rdfResponseTypes = rdfTypes.toArray(new String[rdfTypes.size()]);;
        for (final String type : rdfResponseTypes) {
            final HttpGet method = new HttpGet(versionUri);
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testDatetimeNegotiationLDPRv() throws Exception {
        final CloseableHttpClient customClient = createClient(true);
        final DateTimeFormatter FMT = RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));

        createVersionedContainer(id);
        final String memento1 =
                FMT.format(ISO_INSTANT.parse("2017-06-10T11:41:00Z", Instant::from));
        final String version1Uri = createLDPRSMementoWithExistingBody(memento1);
        final String memento2 =
                FMT.format(ISO_INSTANT.parse("2016-06-17T11:41:00Z", Instant::from));
        final String version2Uri = createLDPRSMementoWithExistingBody(memento2);

        final String request1Datetime =
                FMT.format(ISO_INSTANT.parse("2017-01-12T00:00:00Z", Instant::from));
        final HttpGet getMemento = getObjMethod(id);
        getMemento.addHeader(ACCEPT_DATETIME, request1Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get Location header", version2Uri, response.getFirstHeader(LOCATION).getValue());
            assertEquals("Did not get Content-Length == 0", "0", response.getFirstHeader(CONTENT_LENGTH).getValue());
        }

        final String request2Datetime =
                FMT.format(ISO_INSTANT.parse("2018-01-10T00:00:00Z", Instant::from));
        final HttpGet getMemento2 = getObjMethod(id);
        getMemento2.addHeader(ACCEPT_DATETIME, request2Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento2)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get Location header", version1Uri, response.getFirstHeader(LOCATION).getValue());
            assertEquals("Did not get Content-Length == 0", "0", response.getFirstHeader(CONTENT_LENGTH).getValue());
        }
    }

    @Test
    public void testDatetimeNegotiationNoMementos() throws Exception {
        final CloseableHttpClient customClient = createClient(true);
        final DateTimeFormatter FMT = RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));

        createVersionedContainer(id);
        final String requestDatetime =
                FMT.format(ISO_INSTANT.parse("2017-01-12T00:00:00Z", Instant::from));
        final HttpGet getMemento = getObjMethod(id);
        getMemento.addHeader(ACCEPT_DATETIME, requestDatetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento)) {
            assertEquals("Did not get NOT_FOUND response", NOT_FOUND.getStatusCode(), getStatus(response));
            assertNull("Did not expect a Location header", response.getFirstHeader(LOCATION));
            assertNotEquals("Did not get Content-Length > 0", 0, response.getFirstHeader(CONTENT_LENGTH).getValue());
        }
    }

    @Test
    public void testFixityOnVersionedResource() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, null, null, null);

        final HttpGet checkFixity = new HttpGet(mementoUri + "/" + FCR_FIXITY);
        try (final CloseableHttpResponse response = execute(checkFixity)) {
            assertEquals("Did not get OK response", OK.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testOptionsMemento() throws Exception {
        createVersionedContainer(id);
        final String mementoUri = createContainerMementoWithBody(subjectUri, null);

        final HttpOptions optionsRequest = new HttpOptions(mementoUri);
        try (final CloseableHttpResponse optionsResponse = execute(optionsRequest)) {
            assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());
            assertMementoOptionsHeaders(optionsResponse);
        }
    }

    @Test
    public void testPatchOnMemento() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        final HttpPatch patch = new HttpPatch(mementoUri);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "INSERT DATA { <> <" + title.getURI() + "> \"Memento title\" } "));

        // status 405: PATCH on memento is not allowed.
        assertEquals(405, getStatus(patch));
    }

    @Test
    public void testPostOnMemento() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        final String body = createContainerMementoBodyContent(subjectUri, N3);
        final HttpPost post = new HttpPost(mementoUri);
        post.addHeader(CONTENT_TYPE, N3);
        post.setEntity(new StringEntity(body));

        // status 405: POST on memento is not allowed.
        assertEquals(405, getStatus(post));
    }

    @Test
    public void testPutOnMemento() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        final String body = createContainerMementoBodyContent(subjectUri, N3);
        final HttpPut put = new HttpPut(mementoUri);
        put.addHeader(CONTENT_TYPE, N3);
        put.setEntity(new StringEntity(body));

        // status 405: PUT on memento is not allowed.
        assertEquals(405, getStatus(put));
    }

    @Test
    public void testGetLDPRSMementoHeaders() throws Exception {
        final DateTimeFormatter FMT = RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));
        createVersionedContainer(id);

        final String memento1 =
                FMT.format(ISO_INSTANT.parse("2001-06-10T16:41:00Z", Instant::from));
        final String version1Uri = createLDPRSMementoWithExistingBody(memento1);
        final HttpGet getRequest = new HttpGet(version1Uri);

        try (final CloseableHttpResponse response = execute(getRequest)) {
            assertMementoDatetimeHeaderMatches(response, memento1);
            checkForLinkHeader(response, MEMENTO_TYPE, "type");
            checkForLinkHeader(response, subjectUri, "original");
            checkForLinkHeader(response, subjectUri, "timegate");
            checkForLinkHeader(response, subjectUri + "/" + FCR_VERSIONS, "timemap");
            checkForLinkHeader(response, RESOURCE.toString(), "type");
            assertNoLinkHeader(response, VERSIONED_RESOURCE.toString(), "type");
            assertNoLinkHeader(response, VERSIONING_TIMEMAP_TYPE.toString(), "type");
            assertNoLinkHeader(response, version1Uri + "/" + FCR_ACL, "acl");
        }
    }

    @Test
    public void testGetLDPNRMementoHeaders() throws Exception {
        final DateTimeFormatter FMT = RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));
        createVersionedBinary(id, "text/plain", "This is some versioned content");

        final String memento1 =
                FMT.format(ISO_INSTANT.parse("2001-06-10T16:41:00Z", Instant::from));
        final String version1Uri = createLDPNRMementoWithExistingBody(memento1);
        final HttpGet getRequest = new HttpGet(version1Uri);

        try (final CloseableHttpResponse response = execute(getRequest)) {
            assertMementoDatetimeHeaderMatches(response, memento1);
            checkForLinkHeader(response, MEMENTO_TYPE, "type");
            checkForLinkHeader(response, subjectUri, "original");
            checkForLinkHeader(response, subjectUri, "timegate");
            checkForLinkHeader(response, subjectUri + "/" + FCR_VERSIONS, "timemap");
            checkForLinkHeader(response, NON_RDF_SOURCE.toString(), "type");
            assertNoLinkHeader(response, VERSIONED_RESOURCE.toString(), "type");
            assertNoLinkHeader(response, VERSIONING_TIMEMAP_TYPE.toString(), "type");
            assertNoLinkHeader(response, version1Uri + "/" + FCR_ACL, "acl");
        }
    }

    private static void assertMementoOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow GET", methods.contains(HttpGet.METHOD_NAME));
        assertTrue("Should allow HEAD", methods.contains(HttpHead.METHOD_NAME));
        assertTrue("Should allow OPTIONS", methods.contains(HttpOptions.METHOD_NAME));
        assertTrue("Should allow DELETE", methods.contains(HttpDelete.METHOD_NAME));
    }

    private String createLDPRSMementoWithExistingBody(final String mementoDateTime) throws Exception {
        return createMementoWithExistingBody(id, mementoDateTime, false);
    }

    private String createLDPNRMementoWithExistingBody(final String mementoDateTime) throws Exception {
        return createMementoWithExistingBody(id, mementoDateTime, true);
    }

    private String createMementoWithExistingBody(final String id, final String mementoDateTime, final boolean isLDPNR)
            throws Exception {
        final HttpGet getRequest = getObjMethod(id);
        if (!isLDPNR) {
            getRequest.setHeader(ACCEPT, NTRIPLES);
        }
        try (final CloseableHttpResponse response = execute(getRequest)) {
            if (getStatus(response) == OK.getStatusCode()) {
                // Resource exists so get the body to put back with header
                final String body = EntityUtils.toString(response.getEntity());
                final String mimeType =
                        response.getFirstHeader(CONTENT_TYPE).getValue();
                return createMemento(serverAddress + id, mementoDateTime, mimeType, body);
            }
        }
        return null;
    }

    private String createMemento(final String subjectUri, final String mementoDateTime, final String contentType,
            final String body) throws Exception {
        final HttpPost createVersionMethod = new HttpPost(subjectUri + "/" + FCR_VERSIONS);
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

            return response.getFirstHeader(LOCATION).getValue();
        }
    }

    private String createContainerMementoWithBody(final String subjectUri, final String mementoDateTime)
            throws Exception {

        final String body = createContainerMementoBodyContent(subjectUri, N3);
        return createMemento(subjectUri, mementoDateTime, N3, body);
    }

    private String createContainerMementoBodyContent(final String subjectUri, final String contentType)
        throws Exception {
        // Produce new body from current body with changed triple
        final String body;
        final HttpGet httpGet = new HttpGet(subjectUri);
        final Model model = createDefaultModel();
        final Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), "", rdfLang.getName());
        }
        final String resourceUri = subjectUri.replace("/fcr:metadata", "");
        final Resource subjectResc = model.getResource(resourceUri);
        subjectResc.removeAll(TEST_PROPERTY);
        subjectResc.addLiteral(TEST_PROPERTY, "bar");

        try (StringWriter stringOut = new StringWriter()) {
            RDFDataMgr.write(stringOut, model, RDFFormat.NTRIPLES);
            body = stringOut.toString();
        }

        return body;
    }

    /**
     * Create a versioned LDP-RS
     *
     * @param id the desired slug
     * @return Location of the new resource
     * @throws Exception
     */
    private String createVersionedContainer(final String id) throws Exception {
        final HttpPost createMethod = postObjMethod();
        createMethod.addHeader("Slug", id);
        createMethod.addHeader(CONTENT_TYPE, N3);
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity("<> <info:test#label> \"foo\""));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            return response.getFirstHeader(LOCATION).getValue();
        }
    }

    /**
     * Create a versioned LDP-NR.
     *
     * @param id the desired slug
     * @param mimeType the mimeType of the content
     * @param content the actual content
     * @return Location of the new resource
     * @throws Exception
     */
    private String createVersionedBinary(final String id, final String mimeType, final String content)
            throws Exception {
        final HttpPost createMethod = postObjMethod();
        createMethod.addHeader("Slug", id);
        if (mimeType == null && content == null) {
            createMethod.addHeader(CONTENT_TYPE, OCTET_STREAM_TYPE);
            createMethod.setEntity(new StringEntity(BINARY_CONTENT));
        } else {
            createMethod.addHeader(CONTENT_TYPE, mimeType);
            createMethod.setEntity(new StringEntity(content));
        }
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            return response.getFirstHeader(LOCATION).getValue();
        }
    }

    /**
     * Create a versioned LDP-NR.
     *
     * @param id the desired slug
     * @return Location of the new resource
     * @throws Exception
     */
    private String createVersionedBinary(final String id) throws Exception {
        return createVersionedBinary(id, null, null);
    }

    private static void assertNoMementoDatetimeHeaderPresent(final CloseableHttpResponse response) {
        assertNull("No memento datetime header set in response",
                response.getFirstHeader(MEMENTO_DATETIME_HEADER));
    }

    private static void assertMementoDatetimeHeaderPresent(final CloseableHttpResponse response) {
        assertNotNull("No memento datetime header set in response",
                response.getFirstHeader(MEMENTO_DATETIME_HEADER));
    }

    private static void assertMementoDatetimeHeaderMatches(final CloseableHttpResponse response,
            final String expected) {
        assertMementoDatetimeHeaderPresent(response);
        assertEquals("Response memento datetime did not match expected value",
                expected, response.getFirstHeader(MEMENTO_DATETIME_HEADER).getValue());
    }

    private static void assertConstrainedByPresent(final CloseableHttpResponse response) {
        final Collection<String> linkHeaders = getLinkHeaders(response);
        assertTrue("Constrained by link header no present",
                linkHeaders.stream().map(Link::valueOf)
                        .anyMatch(l -> l.getRel().equals(CONSTRAINED_BY.getURI())));
    }

    private static void enableVersioning(final String uri) throws Exception {
        final HttpPut enableMethod = new HttpPut(uri);
        enableMethod.addHeader(CONTENT_TYPE, N3);
        enableMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        // Resubmit the existing state of the resource
        try (final CloseableHttpResponse response = execute(new HttpGet(uri))) {
            final String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            enableMethod.setEntity(new StringEntity(body));
        }

        try (final CloseableHttpResponse response = execute(enableMethod)) {
            assertEquals("Didn't get a CREATED response!", NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }

    private static void patchLiteralProperty(final String url, final String predicate, final String literal)
            throws IOException {
        final HttpPatch updateObjectGraphMethod = new HttpPatch(url);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity(
                "INSERT DATA { <> <" + predicate + "> \"" + literal + "\" } "));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
    }

    private CloseableDataset getContent(final String url) throws IOException {
        final HttpGet getVersion = new HttpGet(url);
        getVersion.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINED.toString() + "\"");
        return getDataset(getVersion);
    }

    private String[] getTimeMapResponseTypes() {
        rdfTypes.add(APPLICATION_LINK_FORMAT);
        return rdfTypes.toArray(new String[rdfTypes.size()]);
    }

    protected static void assertMementoUri(final String mementoUri, final String subjectUri) {
        assertTrue(mementoUri.matches(subjectUri + "/fcr:versions/\\d+"));
    }

    private static void assertHasLink(final CloseableHttpResponse response, final Property relation,
            final String uri) {
        final String relName = relation.getLocalName();
        assertTrue("Missing link " + relName + " with value " + uri, getLinkHeaders(response)
                .stream().map(Link::valueOf)
                .anyMatch(l -> relName.equals(l.getRel()) && uri.equals(l.getUri().toString())));
    }
}
