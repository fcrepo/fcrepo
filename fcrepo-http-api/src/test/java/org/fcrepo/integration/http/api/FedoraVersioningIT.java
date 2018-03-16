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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author lsitu
 * @author bbpennel
 */
public class FedoraVersioningIT extends AbstractVersioningIT {

    @Test
    public void testDeleteTimeMapForContainer() throws Exception {
        createVersionedContainer(id, subjectUri);

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
        createVersionedContainer(id, subjectUri);

        verifyTimemapResponse(subjectUri, id);
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
        createVersionedContainer(id, subjectUri);

        // Bad request with Slug header to create memento
        final String mementoDateTime = "Tue, 3 Jun 2008 11:05:30 GMT";
        final String body = createContainerMementoBodyContent(subjectUri, "text/n3");
        final HttpPost post = postObjMethod(id + "/" + FCR_VERSIONS);

        post.addHeader("Slug", "version_label");
        post.addHeader(MEMENTO_DATETIME_HEADER, mementoDateTime);
        post.addHeader(CONTENT_TYPE, "text/n3");
        post.setEntity(new StringEntity(body));

        assertEquals("Created memento with Slug!",
                BAD_REQUEST.getStatusCode(), getStatus(post));
    }

    @Test
    public void testCreateVersionWithMementoDatetimeFormat() throws Exception {
        createVersionedContainer(id, subjectUri);

        // Create memento with RFC-1123 date-time format
        final String mementoDateTime = "Tue, 3 Jun 2008 11:05:30 GMT";
        final String body = createContainerMementoBodyContent(subjectUri, "text/n3");

        final HttpPost post = postObjMethod(id + "/" + FCR_VERSIONS);

        post.addHeader(MEMENTO_DATETIME_HEADER, mementoDateTime);
        post.addHeader(CONTENT_TYPE, "text/n3");
        post.setEntity(new StringEntity(body));

        assertEquals("Unable to create memento with RFC-1123 date-time format!",
                CREATED.getStatusCode(), getStatus(post));

        // Create memento with RFC-1123 date-time format in wrong value
        final String dateTime1 = "Tue, 13 Jun 2008 11:05:35 ANYTIMEZONE";
        final HttpPost post1 = postObjMethod(id + "/" + FCR_VERSIONS);

        post1.addHeader(CONTENT_TYPE, "text/n3");
        post1.setEntity(new StringEntity(body));
        post1.addHeader(MEMENTO_DATETIME_HEADER, dateTime1);

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(post1));

        // Create memento in date-time format other than RFC-1123
        final String dateTime2 = "2000-01-01T01:01:01.11Z";
        final HttpPost post2 = postObjMethod(id + "/" + FCR_VERSIONS);

        post2.addHeader(CONTENT_TYPE, "text/n3");
        post2.setEntity(new StringEntity(body));
        post2.addHeader(MEMENTO_DATETIME_HEADER, dateTime2);

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(post2));
    }

    @Test
    public void testCreateVersionWithDatetime() throws Exception {
        createVersionedContainer(id, subjectUri);

        final HttpPost createVersionMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createVersionMethod.addHeader(CONTENT_TYPE, "text/n3");
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
        createVersionedContainer(id, subjectUri);

        final HttpPost createMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
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
     */
    @Test
    public void testCreateVersionWithBody() throws Exception {
        createVersionedContainer(id, subjectUri);

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
        createVersionedContainer(id, subjectUri);

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
    public void testPutOnTimeMapContainer() throws Exception {
        createVersionedContainer(id, subjectUri);

        // status 405: PUT On LPDCv is disallowed.
        assertEquals(405, getStatus(new HttpPut(serverAddress + id + "/" + FCR_VERSIONS)));
    }

    @Test
    public void testPatchOnTimeMapContainer() throws Exception {
        createVersionedContainer(id, subjectUri);

        // status 405: PATCH On LPDCv is disallowed.
        assertEquals(405, getStatus(new HttpPatch(serverAddress + id + "/" + FCR_VERSIONS)));
    }

    @Test
    public void testGetTimeMapResponseForBinary() throws Exception {
        createVersionedBinary(id);

        verifyTimemapResponse(subjectUri, id);
    }

    @Test
    public void testGetTimeMapResponseForBinaryDescription() throws Exception {
        createVersionedBinary(id);

        final String descriptionUri = subjectUri + "/fcr:metadata";
        final String descriptionId = id + "/fcr:metadata";

        verifyTimemapResponse(descriptionUri, descriptionId);
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

    @Ignore("Disable until binary version creation from body implemented")
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

    @Ignore("Disable until binary version creation from body implemented")
    @Test
    public void testCreateVersionOfBinaryWithDatetimeAndBody() throws Exception {
        createVersionedBinary(id);

        final String mementoUri = createMemento(subjectUri, MEMENTO_DATETIME,
                OCTET_STREAM_TYPE, BINARY_UPDATED);
        assertMementoUri(mementoUri, subjectUri);

        // Verify that the memento has the updated binary
        try (final CloseableHttpResponse response = execute(new HttpGet(mementoUri))) {
            assertMementoDatetimeHeaderMatches(response, MEMENTO_DATETIME);

            assertEquals("Binary content of memento must match updated content",
                    BINARY_UPDATED, EntityUtils.toString(response.getEntity()));
        }
    }
}
