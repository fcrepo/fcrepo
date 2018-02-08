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
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Link;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Test;

/**
 * @author lsitu
 */
public class FedoraVersioningIT extends AbstractResourceIT {

    private static final String VERSIONED_RESOURCE_LINK_HEADER = "<" + VERSIONED_RESOURCE.getURI() + ">; rel=\"type\"";

    @Test
    public void testDeleteTimeMapForContainer() throws IOException {
        final String id = getRandomUniqueId();

        // POST to enable versioning
        final String subjectURI = serverAddress + id;
        final HttpPost createMethod = postObjMethod();
        createMethod.addHeader("Slug", id);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
        // disabled versioning to delete TimeMap
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + id + "/" + FCR_VERSIONS)));
    }

    @Test
    public void testGetTimeMapResponse() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final List<Link> listLinks = new ArrayList<Link>();
        listLinks.add(Link.fromUri(subjectURI).rel("original").build());
        listLinks.add(Link.fromUri(subjectURI).rel("timegate").build());
        listLinks
            .add(Link.fromUri(subjectURI + "/" + FCR_VERSIONS).rel("self").type(APPLICATION_LINK_FORMAT).build());
        final Link[] expectedLinks = listLinks.toArray(new Link[3]);

        final HttpPut createMethod = putObjMethod(id);

        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
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
    public void testGetTimeMapRDFSubject() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;

        final HttpPut createMethod = putObjMethod(id);

        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }

        final HttpGet httpGet = getObjMethod(id + "/" + FCR_VERSIONS);

        try (final CloseableDataset dataset = getDataset(httpGet)) {
            final DatasetGraph results = dataset.asDatasetGraph();
            final Node subject = createURI(subjectURI + "/" + FCR_VERSIONS);
            assertTrue("Did not find correct subject", results.contains(ANY, subject, ANY, ANY));
        }
    }

    @Test
    public void testCreateVersion() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final HttpPut putMethod = putObjMethod(id);

        putMethod.addHeader(CONTENT_TYPE, "text/n3");
        putMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"versioned resource\""));
        putMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);

        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
        final HttpPost postMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));

        }
    }

    @Test
    public void testCreateVersionWithDatetime() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final HttpPut putMethod = putObjMethod(id);
        final String mementoDateTime =
            DateTimeFormatter.RFC_1123_DATE_TIME.format(LocalDateTime.of(2000, 1, 1, 00, 00).atZone(ZoneOffset.UTC));

        putMethod.addHeader(CONTENT_TYPE, "text/n3");
        putMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"versioned resource\""));
        putMethod.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);

        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
        }
        final HttpPost postMethod = postObjMethod(id + "/" + FCR_VERSIONS);
        postMethod.addHeader(MEMENTO_DATETIME_HEADER, mementoDateTime);
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertTrue("Didn't get the correct Memento Datetime",
                mementoDateTimeHeaderMatches(response, mementoDateTime));
        }
    }

    /**
     * @param response
     * @param expected
     * @return
     */
    private static boolean mementoDateTimeHeaderMatches(final CloseableHttpResponse response, final String expected) {
        return Arrays.asList(response.getHeaders(MEMENTO_DATETIME_HEADER)).stream().map(Header::getValue)
            .anyMatch(t -> expected.contentEquals(t));
    }
}
