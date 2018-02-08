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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.DatatypeConverter;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>Abstract AbstractResourceIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    protected static Logger logger;
    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    protected static final int SERVER_PORT = parseInt(System.getProperty("fcrepo.dynamic.test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" + SERVER_PORT + "/";

    protected static CloseableHttpClient client = createClient();

    protected static CloseableHttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE).setMaxConnTotal(MAX_VALUE).build();
    }

    protected static HttpPost postObjMethod() {
        return postObjMethod("/");
    }

    protected static HttpPost postObjMethod(final String id) {
        return new HttpPost(serverAddress + id);
    }

    protected static HttpPut putObjMethod(final String id) {
        return new HttpPut(serverAddress + id);
    }

    protected static HttpGet getObjMethod(final String id) {
        return new HttpGet(serverAddress + id);
    }

    protected static HttpHead headObjMethod(final String id) {
        return new HttpHead(serverAddress + id);
    }

    protected static HttpDelete deleteObjMethod(final String id) {
        return new HttpDelete(serverAddress + id);
    }

    protected static HttpPatch patchObjMethod(final String id) {
        return new HttpPatch(serverAddress + id);
    }

    protected static HttpPost postObjMethod(final String id, final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + id);
        }
        return new HttpPost(serverAddress + id + "?" + query);
    }

    protected static HttpPut putDSMethod(final String pid, final String ds, final String content)
            throws UnsupportedEncodingException {
        final HttpPut put = new HttpPut(serverAddress + pid + "/" + ds);
        put.setEntity(new StringEntity(content == null ? "" : content));
        put.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        put.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        return put;
    }

    protected static HttpPut putObjMethod(final String pid, final String contentType, final String content)
            throws UnsupportedEncodingException {
        final HttpPut put = new HttpPut(serverAddress + pid);
        put.setEntity(new StringEntity(content));
        put.setHeader(CONTENT_TYPE, contentType);
        return put;
    }

    protected static HttpGet getDSMethod(final String pid, final String ds) {
        return new HttpGet(serverAddress + pid + "/" + ds);
    }

    protected static HttpGet getDSDescMethod(final String pid, final String ds) {
        return new HttpGet(serverAddress + pid + "/" + ds + "/" + FCR_METADATA);
    }

    /**
     * Execute an HTTP request and return the open response.
     *
     * @param req Request to execute
     * @return the open response
     * @throws IOException in case of an IOException
     */
    protected static CloseableHttpResponse execute(final HttpUriRequest req) throws IOException {
        logger.debug("Executing: " + req.getMethod() + " to " + req.getURI());
        return client.execute(req);
    }

    /**
     * Execute an HTTP request and close the response.
     *
     * @param req the request to execute
     */
    protected static void executeAndClose(final HttpUriRequest req) {
        logger.debug("Executing: " + req.getMethod() + " to " + req.getURI());
        try {
            execute(req).close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Execute an HTTP request with preemptive basic authentication.
     *
     * @param request the request to execute
     * @param username usename to use
     * @param password password to use
     * @return the open responses
     * @throws IOException in case of IOException
     */
    @SuppressWarnings("resource")
    protected CloseableHttpResponse executeWithBasicAuth(final HttpUriRequest request, final String username,
            final String password) throws IOException {
        final HttpHost target = new HttpHost(HOSTNAME, SERVER_PORT, PROTOCOL);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(username, password));
        final CloseableHttpClient httpclient =
                HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        final AuthCache authCache = new BasicAuthCache();
        final BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);

        final HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        return httpclient.execute(request, localContext);
    }

    /**
     * Retrieve the HTTP status code from an open response.
     *
     * @param response the open response
     * @return the HTTP status code of the response
     */
    protected static int getStatus(final HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * Executes an HTTP request and returns the status code of the response, closing the response.
     *
     * @param req the request to execute
     * @return the HTTP status code of the response
     */
    protected static int getStatus(final HttpUriRequest req) {
        try (final CloseableHttpResponse response = execute(req)) {
            final int result = getStatus(response);
            if (!(result > 199) || !(result < 400)) {
                logger.warn("Got status {}", result);
                if (response.getEntity() != null) {
                    logger.trace(EntityUtils.toString(response.getEntity()));
                }
            }
            EntityUtils.consume(response.getEntity());
            return result;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes an HTTP request and returns the first Location header in the response, then closes the response.
     *
     * @param req the request to execute
     * @return the value of the first Location header in the response
     * @throws IOException in case of IOException
     */
    protected static String getLocation(final HttpUriRequest req) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            EntityUtils.consume(response.getEntity());
            return getLocation(response);
        }
    }

    /**
     * Retrieve the value of the first Location header from an open HTTP response.
     *
     * @param response the open response
     * @return the value of the first Location header in the response
     */
    protected static String getLocation(final HttpResponse response) {
        return response.getFirstHeader("Location").getValue();
    }

    protected String getContentType(final HttpUriRequest method) throws IOException {
        return getContentType(method, OK);
    }

    protected String getContentType(final HttpUriRequest method, final Status httpStatus) throws IOException {
        try (final CloseableHttpResponse response = execute(method)) {
            final int result = getStatus(response);
            assertEquals(httpStatus.getStatusCode(), result);
            EntityUtils.consume(response.getEntity());
            return response.getFirstHeader(CONTENT_TYPE).getValue();
        }
    }

    protected static Collection<String> getLinkHeaders(final HttpResponse response) {
        return getHeader(response, LINK);
    }

    protected Collection<String> getLinkHeaders(final HttpUriRequest method) throws IOException {
        try (final CloseableHttpResponse response = execute(method)) {
            return getLinkHeaders(response);
        }
    }

    protected static Collection<String> getHeader(final HttpResponse response, final String header) {
        return stream(response.getHeaders(header)).map(Header::getValue).collect(toList());
    }

    /**
     * Executes an HTTP request and parses the RDF found in the response, returning it in a
     * {@link CloseableDataset}, then closes the response.
     *
     * @param client the client to use
     * @param req the request to execute
     * @return the graph retrieved
     * @throws IOException in case of IOException
     */
    protected CloseableDataset getDataset(final CloseableHttpClient client, final HttpUriRequest req)
            throws IOException {
        if (!req.containsHeader(ACCEPT)) {
            req.addHeader(ACCEPT, "application/n-triples");
        }
        logger.debug("Retrieving RDF using mimeType: {}", req.getFirstHeader(ACCEPT));

        try (final CloseableHttpResponse response = client.execute(req)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            final CloseableDataset result = parseTriples(response.getEntity());
            logger.trace("Retrieved RDF: {}", result);
            return result;
        }

    }

    /**
     * Parses the RDF found in and HTTP response, returning it in a {@link CloseableDataset}.
     *
     * @param response the response to parse
     * @return the graph retrieved
     * @throws IOException in case of IOException
     */
    protected CloseableDataset getDataset(final HttpResponse response) throws IOException {
        assertEquals(OK.getStatusCode(), getStatus(response));
        final CloseableDataset result = parseTriples(response.getEntity());
        logger.trace("Retrieved RDF: {}", result);
        return result;
    }

    /**
     * Executes an HTTP request and parses the RDF found in the response, returning it in a
     * {@link CloseableDataset}, then closes the response.
     *
     * @param req the request to execute
     * @return the constructed graph
     * @throws IOException in case of IOException
     */
    protected CloseableDataset getDataset(final HttpUriRequest req) throws IOException {
        return getDataset(client, req);
    }

    protected CloseableHttpResponse createObject() {
        return createObject("");
    }

    protected CloseableHttpResponse createObject(final String pid) {
        final HttpPost httpPost = postObjMethod("/");
        if (pid.length() > 0) {
            httpPost.addHeader("Slug", pid);
        }
        try {
            final CloseableHttpResponse response = execute(httpPost);
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return response;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void createObjectAndClose(final String pid) {
        try {
            createObject(pid).close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void createDatastream(final String pid, final String dsid, final String content) throws IOException {
        logger.trace("Attempting to create datastream for object: {} at datastream ID: {}", pid, dsid);
        assertEquals(CREATED.getStatusCode(), getStatus(putDSMethod(pid, dsid, content)));
    }

    protected CloseableHttpResponse setProperty(final String pid, final String propertyUri, final String value)
            throws IOException {
        return setProperty(pid, null, propertyUri, value);
    }

    protected CloseableHttpResponse setProperty(final String id, final String txId, final String propertyUri,
            final String value) throws IOException {
        final HttpPatch postProp = new HttpPatch(serverAddress + (txId != null ? txId + "/" : "") + id);
        postProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT { <" + serverAddress + id + "> <" + propertyUri + "> \"" + value + "\" } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        final CloseableHttpResponse dcResp = execute(postProp);
        assertEquals(dcResp.getStatusLine().toString(), NO_CONTENT.getStatusCode(), getStatus(dcResp));
        postProp.releaseConnection();
        return dcResp;
    }

    /**
     * Creates a transaction, asserts that it's successful and returns the transaction location.
     *
     * @return string containing transaction location
     * @throws IOException exception thrown during the function
     */
    protected String createTransaction() throws IOException {
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        try (final CloseableHttpResponse response = execute(createTx)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return getLocation(response);
        }
    }

    protected static void addMixin(final String pid, final String mixinUrl) throws IOException {
        final HttpPatch updateObjectGraphMethod = new HttpPatch(serverAddress + pid);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity(
                "INSERT DATA { <> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + mixinUrl + "> . } "));
        try (final CloseableHttpResponse response = execute(updateObjectGraphMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }

    /**
     * Gets a random (but valid) id for use in testing. This id is guaranteed to be unique within runs of this
     * application.
     *
     * @return string containing new id
     */
    protected static String getRandomUniqueId() {
        return randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property name for use in testing.
     *
     * @return string containing random property name
     */
    protected static String getRandomPropertyName() {
        return randomUUID().toString();
    }

    protected static void assertDeleted(final String id) {
        final String location = serverAddress + id;
        assertThat("Expected object to be deleted", getStatus(new HttpHead(location)), is(GONE.getStatusCode()));
        assertThat("Expected object to be deleted", getStatus(new HttpGet(location)), is(GONE.getStatusCode()));
    }

    protected static void assertNotDeleted(final String id) {
        final String location = serverAddress + id;
        assertThat("Expected object not to be deleted", getStatus(new HttpHead(location)), is(OK.getStatusCode()));
        assertThat("Expected object not to be deleted", getStatus(new HttpGet(location)), is(OK.getStatusCode()));
    }

    protected static String getTTLThatUpdatesServerManagedTriples(final String createdBy, final Calendar created,
                                                                  final String modifiedBy, final Calendar modified) {
        final StringBuilder ttl = new StringBuilder();
        if (createdBy != null) {
            addClause(ttl, CREATED_BY.getURI(), "\"" + createdBy + "\"");
        }
        if (created != null) {
            addClause(ttl, CREATED_DATE.getURI(),
                    "\"" + DatatypeConverter.printDateTime(created)
                    + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
        }
        if (modifiedBy != null) {
            addClause(ttl, LAST_MODIFIED_BY.getURI(), "\"" + modifiedBy + "\"");
        }
        if (modified != null) {
            addClause(ttl, LAST_MODIFIED_DATE.getURI(),
                    "\"" + DatatypeConverter.printDateTime(modified)
                    + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
        }
        ttl.append(" .\n");
        return ttl.toString();

    }

    private static void addClause(final StringBuilder ttl, final String predicateUri, final String literal) {
        if (ttl.length() == 0) {
            ttl.append("<>");
        } else {
            ttl.append(" ;\n");
        }
        ttl.append(" <" + predicateUri + "> ");
        ttl.append(literal);
    }

    /**
     * Test a response for a specific LINK header
     *
     * @param response the HTTP response
     * @param uri the URI expected in the LINK header
     * @param rel the rel argument to check for
     */
    protected void checkForLinkHeader(final CloseableHttpResponse response, final String uri, final String rel) {
        assertEquals(1, countLinkHeader(response, uri, rel));
    }

    /**
     * Test a response for N instances of a specific LINK header
     *
     * @param response the HTTP response
     * @param uri the URI expected in the LINK header
     * @param rel the rel argument to check for
     * @param count how many LINK headers should exist
     */
    protected void checkForNLinkHeaders(final CloseableHttpResponse response, final String uri, final String rel,
        final int count) {
        assertEquals(count, countLinkHeader(response, uri, rel));
    }

    /**
     * Utility for counting LINK headers
     *
     * @param response the HTTP response
     * @param uri the URI expected in the LINK header
     * @param rel the rel argument to check for
     * @return the count of LINK headers.
     */
    private int countLinkHeader(final CloseableHttpResponse response, final String uri, final String rel) {
        final Link linkA = Link.valueOf("<" + uri + ">; rel=" + rel);
        return (int) Arrays.asList(response.getHeaders(LINK)).stream().filter(x -> {
            final Link linkB = Link.valueOf(x.getValue());
            return linkB.equals(linkA);
        }).count();
    }
}
