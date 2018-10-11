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
import static java.util.Arrays.sort;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
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
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_RFC_1123_FORMATTER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Link;

import org.apache.commons.io.FileUtils;
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
import org.apache.http.client.methods.HttpUriRequest;
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
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author lsitu
 * @author bbpennel
 */
public class FedoraVersioningIT extends AbstractResourceIT {

    private static final String BINARY_CONTENT = "binary content";
    private static final String BINARY_UPDATED = "updated content";

    private static final String OCTET_STREAM_TYPE = "application/octet-stream";

    private static final Node MEMENTO_TYPE_NODE = createURI(MEMENTO_TYPE);
    private static final Node TEST_PROPERTY_NODE = createURI("info:test#label");

    private static final Property TEST_PROPERTY = createProperty("info:test#label");

    private final String MEMENTO_DATETIME =
        MEMENTO_RFC_1123_FORMATTER.format(LocalDateTime.of(2000, 1, 1, 00, 00).atZone(ZoneOffset.UTC));
    private final List<String> rdfTypes = new ArrayList<>(Arrays.asList(POSSIBLE_RDF_RESPONSE_VARIANTS_STRING));

    private String subjectUri;
    private String id;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void init() {
        id = getRandomUniqueId();
        subjectUri = serverAddress + id;
    }

    @Test
    public void testDeleteTimeMapNotAllowed() throws Exception {
        createVersionedContainer(id);
        final String timeMapUri = subjectUri + "/" + FCR_VERSIONS;
        assertEquals(200, getStatus(new HttpGet(timeMapUri)));
        // disabled versioning to delete TimeMap
        assertEquals(METHOD_NOT_ALLOWED.getStatusCode(),
                     getStatus(new HttpDelete(serverAddress + id + "/" + FCR_VERSIONS)));
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
            MEMENTO_RFC_1123_FORMATTER.format(LocalDateTime.of(2000, 1, 1, 00, 00, 00).atOffset(ZoneOffset.UTC));
        final String memento2 =
            MEMENTO_RFC_1123_FORMATTER.format(LocalDateTime.of(2015, 8, 13, 18, 30, 0).atOffset(ZoneOffset.UTC));
        final String memento3 =
            MEMENTO_RFC_1123_FORMATTER.format(LocalDateTime.of(1980, 5, 31, 9, 15, 30).atOffset(ZoneOffset.UTC));
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

            assertMementoEqualsOriginal(mementoUri);
        }
    }

    /**
     * This will test for weird date/time scenario.  If the date time stamp has
     * a ms field which is a multiple of 10 and only has one or two digits (ie, .5 or .86)
     * then Modeshape 5.0 will incorrectly parse that value (ie. .5 s becomes .005 s),
     * thereby changing the time.
     */
    @Test
    public void testCreateVersionWithLastModifiedDateTimestamp() throws Exception {
        try {
            final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            // relaxing the server managed mode here so lastModifiedDate can be set
            System.setProperty(SERVER_MANAGED_PROPERTIES_MODE, "relaxed");

            createVersionedContainer(id);

            // this results in a time which has .86 in the ms area. This is key for this test.
            final String createdDate = FMT.format(
                    LocalDateTime.of(2018, 9, 21, 5, 30, 01, 860000000).atZone(ZoneOffset.UTC));

            final String lastModified = FMT.format(
                    LocalDateTime.of(2018, 9, 21, 5, 30, 03, 500000000).atZone(ZoneOffset.UTC));

            // patch the resource with timestamps that trigger millisecond truncation in modeshape 5.0
            // (ie, .86 will get interpreted by Modeshape as .086 and .5 will become .005)
            patchLiteralProperty(serverAddress + id, CREATED_DATE.toString(), createdDate,
                    "<http://www.w3.org/2001/XMLSchema#dateTime>");
            patchLiteralProperty(serverAddress + id, LAST_MODIFIED_DATE.toString(), lastModified,
                    "<http://www.w3.org/2001/XMLSchema#dateTime>");

            final String memento = createMemento(subjectUri, null, null, null);

            assertMementoEqualsOriginal(memento);

        } finally {
            // set server managed mode back to strict
            System.clearProperty(SERVER_MANAGED_PROPERTIES_MODE);
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

            final Node mementoSubject = createURI(subjectUri);

            assertTrue("Memento created without datetime must retain original state",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("foo")));
            assertFalse("Memento created without datetime must ignore updates",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("bar")));

            // memento should be the same as original, in this case
            assertMementoEqualsOriginal(mementoUri);
        }
    }

    @Test
    public void testCreateVersionWithDatetimeAndBody() throws Exception {
        createVersionedContainer(id);

        final String mementoUri = createContainerMementoWithBody(subjectUri, MEMENTO_DATETIME);
        assertMementoUri(mementoUri, subjectUri);
        final Node mementoSubject = createURI(subjectUri);
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

        final Node mementoSubject = createURI(subjectUri);
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
    public void testCreateVersionWithDatetimeAndEmptyBody() throws Exception {
        createVersionedContainer(id);

        final HttpPost createVersionMethod = new HttpPost(subjectUri + "/" + FCR_VERSIONS);
        createVersionMethod.setEntity(new StringEntity(""));
        createVersionMethod.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        // Create new memento of resource with updated body
        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals("Didn't get a BAD_REQUEST response!", BAD_REQUEST.getStatusCode(), getStatus(response));
            final String responseMsg = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            assertTrue("Expected error message indicating empty body provided, received: " + responseMsg,
                    responseMsg.contains("Cannot create historic memento from an empty body"));
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
    public void testHeadOnMemento() throws Exception {

        createVersionedContainer(id);
        final String mementoDateTime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-06-10T11:41:00Z", Instant::from));
        final String mementoUri = createLDPRSMementoWithExistingBody(mementoDateTime);

        // Status 200: HEAD request on existing memento
        final HttpHead headMethod = new HttpHead(mementoUri);
        assertEquals("Expected memento is NOT found: " + mementoUri, OK.getStatusCode(), getStatus(headMethod));

        // Status 404: HEAD request on absent memento
        final HttpHead headMementoAbsent = headObjMethod(id + "/" + FCR_VERSIONS + "/20000101000001");
        assertEquals("Didn't get status 404 on absent memento!",
            NOT_FOUND.getStatusCode(), getStatus(headMementoAbsent));

        // Status 400: HEAD request with invalid memento path
        final HttpHead headMethodInvalid = headObjMethod(id + "/" + FCR_VERSIONS + "/any");
        checkResponseWithInvalidMementoID(headMethodInvalid);
    }

    @Test
    public void testGetOnMemento() throws Exception {

        createVersionedContainer(id);
        final String mementoDateTime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-06-10T11:41:00Z", Instant::from));
        final String mementoUri = createLDPRSMementoWithExistingBody(mementoDateTime);

        // Status 200: GET request on existing memento
        final HttpGet getMemento = new HttpGet(mementoUri);
        assertEquals("Expected memento is NOT found: " + mementoUri, OK.getStatusCode(), getStatus(getMemento));

        // Status 404: GET request on absent memento
        final HttpGet getMementoAbsent = getObjMethod(id + "/" + FCR_VERSIONS + "/20000101000001");
        assertEquals("Didn't get status 404 on absent memento!",
            NOT_FOUND.getStatusCode(), getStatus(getMementoAbsent));

        // Status 400: GET request with invalid memento path
        final HttpGet getMementoInvalid = getObjMethod(id + "/" + FCR_VERSIONS + "/any");
        checkResponseWithInvalidMementoID(getMementoInvalid);
    }

    @Test
    public void testOptionsOnMemento() throws Exception {

        createVersionedContainer(id);
        final String mementoDateTime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-06-10T11:41:00Z", Instant::from));
        final String mementoUri = createLDPRSMementoWithExistingBody(mementoDateTime);

        // Status 200: OPTIONS request on existing memento
        final HttpOptions optionsMemento = new HttpOptions(mementoUri);
        assertEquals("Expected memento is NOT found: " + mementoUri, OK.getStatusCode(), getStatus(optionsMemento));

        // Status 404: OPTIONS request on absent memento
        final String absentMementoPath = serverAddress + id + "/" + FCR_VERSIONS + "/20000101000001";
        final HttpOptions optionsMementoAbsent = new HttpOptions(absentMementoPath);
        assertEquals("Didn't get status 404 on absent memento!",
            NOT_FOUND.getStatusCode(), getStatus(optionsMementoAbsent));

        // Status 400: OPTIONS request with invalid memento path
        final HttpOptions optionsMementoInvalid = new HttpOptions(serverAddress + id + "/" + FCR_VERSIONS + "/any");
        checkResponseWithInvalidMementoID(optionsMementoInvalid);
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

        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(mementoUri));
                final CloseableDataset dataset = getDataset(getResponse1);) {

            final DatasetGraph graph = dataset.asDatasetGraph();

            // Ensure that the resource reference is still in memento
            assertTrue("Expected resource NOT found: " + graph, graph.contains(ANY,
                    ANY, createURI("http://pcdm.org/models#hasMember"), createURI(resource)));

            // Ensure that the subject of the memento is the original reosurce
            assertTrue("Subjects should be the original resource, not the memento: " + graph,
                       !graph.contains(ANY, createURI(mementoUri), ANY, ANY));
        }
    }

    @Test
    public void testDescriptionMementoReference() throws Exception {
        // Create binary with description referencing other resource
        createVersionedBinary(id);

        final String referencedPid = getRandomUniqueId();
        final String referencedResource = serverAddress + referencedPid;
        createObjectAndClose(referencedPid);

        final String metadataId = id + "/fcr:metadata";
        final String metadataUri = serverAddress + metadataId;

        final String relation = "http://purl.org/dc/elements/1.1/relation";
        final HttpPatch updateObjectGraphMethod = patchObjMethod(metadataId);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity(
                "INSERT {" + " <> <" + relation + "> <" + referencedResource + "> } WHERE {}"));
        executeAndClose(updateObjectGraphMethod);

        // Create memento
        final String mementoUri = createMemento(subjectUri, null, null, null);
        assertMementoUri(mementoUri, subjectUri);

        // Delete referenced resource
        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(referencedResource)));

        final Node originalBinaryNode = createURI(serverAddress + id);
        // Ensure that the resource reference is gone
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(metadataUri));
                final CloseableDataset dataset = getDataset(getResponse1);) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse("Expected NOT to have resource: " + graph, graph.contains(ANY,
                    originalBinaryNode, createURI(relation), createURI(referencedResource)));
        }

        final String descMementoUrl = mementoUri.replace(FCR_VERSIONS, "fcr:metadata/fcr:versions");
        // Ensure that the resource reference is still in memento
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(descMementoUrl));
                final CloseableDataset dataset = getDataset(getResponse1);) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Expected resource NOT found: " + graph, graph.contains(ANY,
                    originalBinaryNode, createURI(relation), createURI(referencedResource)));

            // Verify that described by link persists and there is only one
            final Iterator<Quad> describedIt = graph.find(ANY, originalBinaryNode, DESCRIBED_BY.asNode(), ANY);
            assertEquals(metadataUri, describedIt.next().getObject().getURI());
            assertFalse(describedIt.hasNext());
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

    /**
     * Verify an application/link-format TimeMap response.
     *
     * @param uri The full URI of the Original Resource.
     * @param id The path of the Original Resource.
     * @throws Exception on HTTP request error
     */
    private void verifyTimemapResponse(final String uri, final String id) throws Exception {
        verifyTimemapResponse(uri, id, null, null, null);
    }

    /**
     * Verify an application/link-format TimeMap response.
     *
     * @param uri The full URI of the Original Resource.
     * @param id The path of the Original Resource.
     * @param mementoDateTime a RFC-1123 datetime
     * @throws Exception on HTTP request error
     */
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
     * @throws Exception on HTTP request error
     */
    private void verifyTimemapResponse(final String uri, final String id, final String[] mementoDateTime,
        final String rangeStart, final String rangeEnd)
        throws Exception {
        final String ldpcvUri = uri + "/" + FCR_VERSIONS;
        final List<Link> listLinks = new ArrayList<>();
        listLinks.add(Link.fromUri(uri).rel("original").build());
        listLinks.add(Link.fromUri(uri).rel("timegate").build());

        final javax.ws.rs.core.Link.Builder selfLink = Link.fromUri(ldpcvUri).rel("self").type(APPLICATION_LINK_FORMAT);
        if (rangeStart != null && rangeEnd != null) {
            selfLink.param("from", rangeStart).param("until",
                rangeEnd);
        }
        listLinks.add(selfLink.build());
        if (mementoDateTime != null) {
            for (final String memento : mementoDateTime) {
                final TemporalAccessor instant = MEMENTO_RFC_1123_FORMATTER.parse(memento);
                listLinks.add(Link.fromUri(ldpcvUri + "/" + MEMENTO_LABEL_FORMATTER.format(instant))
                              .rel("memento")
                    .param("datetime", memento)
                              .build());
            }
        }

        final Link[] expectedLinks = listLinks.stream()
                                              .sorted((a,b) ->a.toString().compareTo(b.toString()))
                                              .toArray(Link[]::new);

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);
        httpGet.setHeader("Accept", APPLICATION_LINK_FORMAT);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals("Didn't get a OK response!", OK.getStatusCode(), getStatus(response));
            // verify headers in link format.
            verifyTimeMapHeaders(response, uri);
            final List<String> bodyList = Arrays.asList(EntityUtils.toString(response.getEntity()).split(",\n"));
            //the links from the body are not

            final Link[] bodyLinks = bodyList.stream().map(String::trim).filter(t -> !t.isEmpty())
                                                      .sorted((a,b) ->a.toString().compareTo(b.toString()))
                                                      .map(Link::valueOf).toArray(Link[]::new);
            assertArrayEquals(expectedLinks, bodyLinks);
        }
    }

    /**
     * Utility function to verify TimeMap headers
     *
     * @param response the response
     * @param uri the URI of the resource.
     */
    private static void verifyTimeMapHeaders(final CloseableHttpResponse response, final String uri) {
        final String ldpcvUri = uri + "/" + FCR_VERSIONS;
        checkForLinkHeader(response, RESOURCE.toString(), "type");
        checkForLinkHeader(response, CONTAINER.toString(), "type");
        checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
        checkForLinkHeader(response, uri, "original");
        checkForLinkHeader(response, uri, "timegate");
        checkForLinkHeader(response, uri + "/" + FCR_VERSIONS, "timemap");
        checkForLinkHeader(response, VERSIONING_TIMEMAP_TYPE, "type");
        checkForLinkHeader(response, ldpcvUri + "/" + FCR_ACL, "acl");
        assertEquals(1, response.getHeaders("Accept-Post").length);
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
            assertTrue("Must have binary type",
                    results.contains(ANY, mementoSubject, RDF.type.asNode(), FEDORA_BINARY.asNode()));
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

            final Node mementoSubject = createURI(subjectUri);

            assertFalse("Memento type should not be visible",
                    results.contains(ANY, mementoSubject, RDF.type.asNode(), MEMENTO_TYPE_NODE));
            assertTrue("Memento must have first updated property",
                    results.contains(ANY, mementoSubject, TEST_PROPERTY_NODE, createLiteral("bar")));
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

        createVersionedContainer(id);
        final String memento1 =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-06-10T11:41:00Z", Instant::from));
        final String version1Uri = createLDPRSMementoWithExistingBody(memento1);
        final String memento2 =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2016-06-17T11:41:00Z", Instant::from));
        final String version2Uri = createLDPRSMementoWithExistingBody(memento2);

        // Request datetime between memento1 and memento2
        final String request1Datetime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-01-12T00:00:00Z", Instant::from));
        final HttpGet getMemento = getObjMethod(id);
        getMemento.addHeader(ACCEPT_DATETIME, request1Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get Location header", version2Uri, response.getFirstHeader(LOCATION).getValue());
            assertEquals("Did not get Content-Length == 0", "0", response.getFirstHeader(CONTENT_LENGTH).getValue());
        }

        // Request datetime more recent than both mementos
        final String request2Datetime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2018-01-10T00:00:00Z", Instant::from));
        final HttpGet getMemento2 = getObjMethod(id);
        getMemento2.addHeader(ACCEPT_DATETIME, request2Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento2)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get Location header", version1Uri, response.getFirstHeader(LOCATION).getValue());
            assertEquals("Did not get Content-Length == 0", "0", response.getFirstHeader(CONTENT_LENGTH).getValue());
        }

        // Request datetime older than either mementos
        final String request3Datetime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2014-01-01T00:00:00Z", Instant::from));
        final HttpGet getMemento3 = getObjMethod(id);
        getMemento3.addHeader(ACCEPT_DATETIME, request3Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento3)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get Location header", version2Uri, response.getFirstHeader(LOCATION).getValue());
            assertEquals("Did not get Content-Length == 0", "0", response.getFirstHeader(CONTENT_LENGTH).getValue());
        }
    }

    @Test
    public void testDatetimeNegotiationExactMatch() throws Exception {
        final CloseableHttpClient customClient = createClient(true);

        final String originalUri = createVersionedContainer(id);

        // Create a current memento
        final String version1Uri = createMemento(originalUri, null, "text/turtle", null);
        final HttpHead httpHead = new HttpHead(version1Uri);
        final String version1Datetime;
        try (final CloseableHttpResponse response = customClient.execute(httpHead)) {
            version1Datetime = response.getFirstHeader(MEMENTO_DATETIME_HEADER).getValue();
        }

        // Create a slightly older memento
        final Instant version2Instant =
            Instant.from(MEMENTO_RFC_1123_FORMATTER.parse(version1Datetime)).minus(5, ChronoUnit.SECONDS);
        final String version2Datetime = MEMENTO_RFC_1123_FORMATTER.format(version2Instant);
        final String version2Uri = createLDPRSMementoWithExistingBody(version2Datetime);

        // Attempt to retrieve older memento
        final HttpGet getVersion2 = getObjMethod(id);
        getVersion2.addHeader(ACCEPT_DATETIME, version2Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getVersion2)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get expected memento location",
                    version2Uri, response.getFirstHeader(LOCATION).getValue());
        }

        // Attempt to get newest memento
        final HttpGet getVersion1 = getObjMethod(id);
        getVersion1.addHeader(ACCEPT_DATETIME, version1Datetime);

        try (final CloseableHttpResponse response = customClient.execute(getVersion1)) {
            assertEquals("Did not get FOUND response", FOUND.getStatusCode(), getStatus(response));
            assertNoMementoDatetimeHeaderPresent(response);
            assertEquals("Did not get expected memento location",
                    version1Uri, response.getFirstHeader(LOCATION).getValue());
        }
    }

    @Test
    public void testDatetimeNegotiationNoMementos() throws Exception {
        final CloseableHttpClient customClient = createClient(true);

        createVersionedContainer(id);
        final String requestDatetime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-01-12T00:00:00Z", Instant::from));
        final HttpGet getMemento = getObjMethod(id);
        getMemento.addHeader(ACCEPT_DATETIME, requestDatetime);

        try (final CloseableHttpResponse response = customClient.execute(getMemento)) {
            assertEquals("Did not get NOT_ACCEPTABLE response", NOT_ACCEPTABLE.getStatusCode(), getStatus(response));
            assertNull("Did not expect a Location header", response.getFirstHeader(LOCATION));
            assertNotEquals("Did not get Content-Length > 0", 0, response.getFirstHeader(CONTENT_LENGTH).getValue());
        }
    }


    @Test
    public void testGetWithDateTimeNegotiation() throws Exception {
        final CloseableHttpClient customClient = createClient(true);
        final String mementoDateTime =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2017-08-29T15:47:50Z", Instant::from));

        createVersionedContainer(id);

        // Status 406: Get absent memento with datetime negotiation.
        final HttpGet getMethod1 = getObjMethod(id);
        getMethod1.setHeader(ACCEPT_DATETIME, mementoDateTime);
        assertEquals("Didn't get status 406 on absent memento!",
            NOT_ACCEPTABLE.getStatusCode(), getStatus(customClient.execute(getMethod1)));

        // Create memento
        final String mementoUri = createLDPRSMementoWithExistingBody(mementoDateTime);

        // Status 302: GET memento with datetime negotiation
        final HttpGet getMethod2 = getObjMethod(id);
        getMethod2.setHeader(ACCEPT_DATETIME, mementoDateTime);
        assertEquals("Expected memento is NOT found: " + mementoUri, FOUND.getStatusCode(),
                getStatus(customClient.execute(getMethod2)));

        // Status 400: Get memento with bad Accept-Datetime header value
        final String badDataTime = "Wed, 29 Aug 2017 15:47:50 GMT"; // should be TUE, 29 Aug 2017 15:47:50 GMT
        final HttpGet getMethod3 = getObjMethod(id);
        getMethod3.setHeader(ACCEPT_DATETIME, badDataTime);
        assertEquals("Didn't get status 400 on bad Accept-Datetime value!",
            BAD_REQUEST.getStatusCode(), getStatus(customClient.execute(getMethod3)));
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
    public void testPatchOnInvalidMementoPath() throws Exception {
        createVersionedContainer(id);

        final String anyMementoUri = subjectUri + "/fcr:versions/any";
        final HttpPatch anyPatch = new HttpPatch(anyMementoUri);
        anyPatch.addHeader(CONTENT_TYPE, "application/sparql-update");
        anyPatch.setEntity(new StringEntity(
                "INSERT DATA { <> <" + title.getURI() + "> \"Memento title\" } "));

        // status 405: PATCH on memento path is not allowed.
        assertEquals(405, getStatus(anyPatch));
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
    public void testPostOnInvalidMementoPath() throws Exception {
        createVersionedContainer(id);

        final String body = createContainerMementoBodyContent(subjectUri, N3);
        final String anyMementoUri = subjectUri + "/fcr:versions/any";
        final HttpPost anyPost = new HttpPost(anyMementoUri);;
        anyPost.addHeader(CONTENT_TYPE, N3);
        anyPost.setEntity(new StringEntity(body));

        // status 405: POST on memento path is not allowed.
        assertEquals(405, getStatus(anyPost));
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
    public void testPutOnInvalidMementoPath() throws Exception {
        createVersionedContainer(id);

        final String body = createContainerMementoBodyContent(subjectUri, N3);
        final String anyMementoUri = subjectUri + "/fcr:versions/any";
        final HttpPut anyPut = new HttpPut(anyMementoUri);
        anyPut.addHeader(CONTENT_TYPE, N3);
        anyPut.setEntity(new StringEntity(body));

        // status 405: PUT on memento path is not allowed.
        assertEquals(405, getStatus(anyPut));
    }

    @Test
    public void testGetLDPRSMementoHeaders() throws Exception {
        createVersionedContainer(id);

        final String memento1 =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2001-06-10T16:41:00Z", Instant::from));
        final String version1Uri = createLDPRSMementoWithExistingBody(memento1);
        final HttpGet getRequest = new HttpGet(version1Uri);

        try (final CloseableHttpResponse response = execute(getRequest)) {
            assertMementoDatetimeHeaderMatches(response, memento1);
            checkForLinkHeader(response, MEMENTO_TYPE, "type");
            checkForLinkHeader(response, subjectUri, "original");
            checkForLinkHeader(response, subjectUri, "timegate");
            checkForLinkHeader(response, subjectUri + "/" + FCR_VERSIONS, "timemap");
            checkForLinkHeader(response, RESOURCE.toString(), "type");
            checkForLinkHeader(response, RDF_SOURCE.toString(), "type");
            assertNoLinkHeader(response, VERSIONED_RESOURCE.toString(), "type");
            assertNoLinkHeader(response, VERSIONING_TIMEMAP_TYPE.toString(), "type");
            assertNoLinkHeader(response, version1Uri + "/" + FCR_ACL, "acl");
        }
    }

    @Test
    public void testGetLDPNRMementoHeaders() throws Exception {
        createVersionedBinary(id, "text/plain", "This is some versioned content");

        final String memento1 =
            MEMENTO_RFC_1123_FORMATTER.format(ISO_INSTANT.parse("2001-06-10T16:41:00Z", Instant::from));
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

    /*
     * Verify binary description timemap RDF representation can be retrieved with and without
     * accompanying binary memento
     */
    @Test
    public void testFcrepo2792() throws Exception {
        // 1. Create versioned resource
        createVersionedBinary(id);

        final String descriptionUri = subjectUri + "/fcr:metadata";
        final String descTimemapUri = descriptionUri + "/" + FCR_VERSIONS;

        // 2. verify that metadata versions endpoint returns 200
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(descTimemapUri)));

        // 3. create a binary version against binary timemap
        final String mementoUri = createMemento(subjectUri, null, null, null);
        final String descMementoUri = mementoUri.replace("fcr:versions", "fcr:metadata/fcr:versions");

        final Node timemapSubject = createURI(descTimemapUri);
        final Node descMementoResc = createURI(descMementoUri);
        // 4. verify that the binary description timemap RDF is there and contains the new description memento
        try (final CloseableDataset dataset = getDataset(new HttpGet(descTimemapUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();
            assertTrue("Timemap RDF response must contain description memento",
                    results.contains(ANY, timemapSubject, CONTAINS.asNode(), descMementoResc));
        }

        // Wait a second to avoid timestamp collisions
        TimeUnit.SECONDS.sleep(1);

        // 5. Create a second binary description memento
        final String descMementoUri2 = createMemento(descriptionUri, null, null, null);

        // 6. verify that the binary description timemap availabe (returns 404 in fcrepo-2792)
        try (final CloseableDataset dataset = getDataset(new HttpGet(descTimemapUri))) {
            final DatasetGraph results = dataset.asDatasetGraph();
            final Node descMementoResc2 = createURI(descMementoUri2);

            assertTrue("Timemap RDF response must contain first description memento",
                    results.contains(ANY, timemapSubject, CONTAINS.asNode(), descMementoResc));
            assertTrue("Timemap RDF response must contain second description memento",
                    results.contains(ANY, timemapSubject, CONTAINS.asNode(), descMementoResc2));
        }
    }

    @Test
    public void testOptionsTimeMap() throws Exception {
        createVersionedContainer(id);
        final String timemapUri = subjectUri + "/" + FCR_VERSIONS;

        try (final CloseableHttpResponse response = execute(new HttpOptions(timemapUri))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            verifyTimeMapHeaders(response, subjectUri);
        }
    }

    @Test
    public void testCreateExternalBinaryProxyVersion() throws Exception {
        // Create binary to use as content for proxying
        final String proxyContent = "proxied content";
        final String proxiedId = getRandomUniqueId();
        final String proxiedUri = serverAddress + proxiedId + "/ds";
        createDatastream(proxiedId, "ds", proxyContent);

        // Create the proxied external binary object using the first binary
        createVersionedExternalBinaryMemento(id, "proxy", proxiedUri);

        // Create a version of the external binary using the second binary as content
        final String mementoUri = createMemento(subjectUri, null, null, null);

        // Verify that the historic version exists and proxies the old content
        final HttpGet httpGet1 = new HttpGet(mementoUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet1)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            assertMementoDatetimeHeaderPresent(getResponse);
            assertEquals(proxiedUri, getContentLocation(getResponse));
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Entity Data doesn't match proxied versioned content!", proxyContent, content);
        }
    }

    @Test
    public void testCreateHistoricExternalBinaryProxyVersion() throws Exception {
        // Create two binaries to use as content for proxying
        final String newContent = "new content";
        final String proxied1Id = getRandomUniqueId();
        final String proxied1Uri = serverAddress + proxied1Id + "/ds";
        createDatastream(proxied1Id, "ds", newContent);

        final String oldContent = "old content";
        final String proxied2Id = getRandomUniqueId();
        final String proxied2Uri = serverAddress + proxied2Id + "/ds";
        createDatastream(proxied2Id, "ds", "old content");

        // Create the proxied external binary object using the first binary
        createVersionedExternalBinaryMemento(id, "proxy", proxied1Uri);

        // Create a historic version of the external binary using the second binary as content
        final String mementoUri = createExternalBinaryMemento(subjectUri, "proxy", proxied2Uri);

        // Verify that the historic version exists and proxies the old content
        final HttpGet httpGet1 = new HttpGet(mementoUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet1)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            assertMementoDatetimeHeaderMatches(getResponse, MEMENTO_DATETIME);
            assertEquals(proxied2Uri, getContentLocation(getResponse));
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Entity Data doesn't match proxied historic content!", oldContent, content);
        }

        // Verify that the current version still proxies the correct content
        final HttpGet httpGet2 = new HttpGet(subjectUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet2)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            assertEquals(proxied1Uri, getContentLocation(getResponse));
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Entity Data doesn't match proxied historic content!", newContent, content);
        }
    }

    @Test
    public void testCreateHistoricExternalBinaryRedirectVersion() throws Exception {
        // Create two binaries to use as content for proxying
        final String newContent = "new content";
        final String ext1Id = getRandomUniqueId();
        final String ext1Uri = serverAddress + ext1Id + "/ds";
        createDatastream(ext1Id, "ds", newContent);

        final String oldContent = "old content";
        final String ext2Id = getRandomUniqueId();
        final String ext2Uri = serverAddress + ext2Id + "/ds";
        createDatastream(ext2Id, "ds", "old content");

        // Create the proxied external binary object using the first binary
        createVersionedExternalBinaryMemento(id, "redirect", ext1Uri);

        // Create a historic version of the external binary using the second binary as content
        final String mementoUri = createExternalBinaryMemento(subjectUri, "redirect", ext2Uri);

        // Verify that the historic version exists and redirects to the old content
        final HttpGet httpGet1 = new HttpGet(mementoUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet1)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Content doesn't match redirected historic content", oldContent, content);
        }

        // Verify that the current version still redirects to the correct content
        final HttpGet httpGet2 = new HttpGet(subjectUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet2)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Content doesn't match redirected historic content", newContent, content);
        }
    }

    @Test
    public void testCreateHistoricExternalBinaryCopyVersion() throws Exception {
        final String newContent = "new content";

        final String oldContent = "old content";
        final File localFile = tmpDir.newFile();
        FileUtils.writeStringToFile(localFile, oldContent, "UTF-8");

        createVersionedBinary(id, "text/plain", newContent);

        final String mementoUri = createExternalBinaryMemento(subjectUri, "copy", localFile.toURI().toString());

        // Verify that the historic version exists and is the copied old content
        final HttpGet httpGet1 = new HttpGet(mementoUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet1)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            assertMementoDatetimeHeaderMatches(getResponse, MEMENTO_DATETIME);
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Content doesn't match copied historic content", oldContent, content);
        }

        // Verify that the current version is still available
        final HttpGet httpGet2 = new HttpGet(subjectUri);
        try (final CloseableHttpResponse getResponse = execute(httpGet2)) {
            assertEquals(OK.getStatusCode(), getStatus(getResponse));
            final String content = EntityUtils.toString(getResponse.getEntity());
            assertEquals("Binary doesn't match expected content", newContent, content);
        }
    }

    @Test
    public void versionedResourcesCreatedByDefault() throws Exception {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String timemap = id + "/" + FCR_VERSIONS;
        final HttpPost versionPost = postObjMethod(timemap);
        try (final CloseableHttpResponse response = execute(versionPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

    private void createVersionedExternalBinaryMemento(final String rescId, final String handling,
            final String externalUri) throws Exception {
        final HttpPut httpPut = putObjMethod(rescId);
        httpPut.addHeader(LINK, "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"");
        httpPut.addHeader(LINK, getExternalContentLinkHeader(externalUri, handling, null));
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
    }

    private String createExternalBinaryMemento(final String rescUri, final String handling, final String externalUri)
            throws Exception {
        final String ldpcvUri = rescUri + "/fcr:versions";
        final HttpPost httpPost = new HttpPost(ldpcvUri);
        httpPost.addHeader(LINK, getExternalContentLinkHeader(externalUri, handling, null));
        httpPost.addHeader(MEMENTO_DATETIME_HEADER, MEMENTO_DATETIME);

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertMementoDatetimeHeaderPresent(response);
            return response.getFirstHeader(LOCATION).getValue();
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
        if (mementoDateTime != null && !mementoDateTime.isEmpty()) {
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

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            logger.info("created object: {}", response.getFirstHeader(LOCATION).getValue());
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

    private static void patchLiteralProperty(final String url, final String predicate, final String literal)
            throws IOException {
        patchLiteralProperty(url, predicate, literal, null);
    }
    private static void patchLiteralProperty(final String url, final String predicate, final String literal,
                                             final String xsdType)
            throws IOException {
        final HttpPatch updateObjectGraphMethod = new HttpPatch(url);
        final String type = xsdType != null ? "^^" + xsdType : "";
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity(
                "INSERT DATA { <> <" + predicate + "> \"" + literal + "\"" + type + " } "));
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

    protected static void assertMementoEqualsOriginal(final String mementoURI) throws Exception {

        final HttpGet getMemento = new HttpGet(mementoURI);
        getMemento.addHeader(ACCEPT, "application/n-triples");

        try (final CloseableHttpResponse response = execute(getMemento)) {
            final HttpGet getOriginal = new HttpGet(getOriginalResourceUri(response));
            getOriginal.addHeader(ACCEPT, "application/n-triples");

            try (final CloseableHttpResponse origResponse = execute(getOriginal)) {

                final String[] mTriples = EntityUtils.toString(response.getEntity()).split("\\.\\r?\\n");
                final String[] oTriples = EntityUtils.toString(origResponse.getEntity()).split("\\.\\r?\\n");

                sort(mTriples);
                sort(oTriples);

                assertArrayEquals("Memento and Original Resource triples do not match!", mTriples, oTriples);
            }
        }
    }

    private static void assertHasLink(final CloseableHttpResponse response, final Property relation,
            final String uri) {
        final String relName = relation.getLocalName();
        assertTrue("Missing link " + relName + " with value " + uri, getLinkHeaders(response)
                .stream().map(Link::valueOf)
                .anyMatch(l -> relName.equals(l.getRel()) && uri.equals(l.getUri().toString())));
    }

    private void checkResponseWithInvalidMementoID(final HttpUriRequest req) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            assertEquals("Didn't get status 400 with invalid memento path!",
                BAD_REQUEST.getStatusCode(), getStatus(req));

            // Request must fail with constrained exception due to invalid memento ID
            assertConstrainedByPresent(response);
        }
    }
}
