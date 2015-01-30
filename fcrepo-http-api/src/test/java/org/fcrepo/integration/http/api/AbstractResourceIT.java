/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.integration.http.api;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>Abstract AbstractResourceIT class.</p>
 *
 * @author awoods
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    public static final String RDF_TYPE = RDF_NAMESPACE + "type";

    protected static Logger logger;

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    protected static final int SERVER_PORT = parseInt(System.getProperty(
            "fcrepo.test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" +
            SERVER_PORT + "/";

    protected static HttpClient client = createClient();

    protected static HttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                .setMaxConnTotal(MAX_VALUE).build();
    }

    protected static HttpPost postObjMethod(final String pid) {
        return new HttpPost(serverAddress + pid);
    }

    protected static HttpPut putObjMethod(final String pid) {
        return new HttpPut(serverAddress + pid);
    }

    protected static HttpGet getObjMethod(final String pid) {
        return new HttpGet(serverAddress + pid);
    }

    protected static HttpDelete deleteObjMethod(final String pid) {
        return new HttpDelete(serverAddress + pid);
    }

    protected static HttpPatch patchObjMethod(final String pid) {
        return new HttpPatch(serverAddress + pid);
    }

    protected static HttpPost postObjMethod(final String pid, final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + pid);
        }
        return new HttpPost(serverAddress + pid + "?" + query);
    }

    protected static HttpPut putDSMethod(final String pid, final String ds,
        final String content) throws UnsupportedEncodingException {
        final HttpPut put =
                new HttpPut(serverAddress + pid + "/" + ds);

        put.setEntity(new StringEntity(content));
        put.setHeader("Content-Type", TEXT_PLAIN);
        return put;
    }

    protected static HttpGet getDSMethod(final String pid, final String ds) {
            final HttpGet get =
                    new HttpGet(serverAddress + pid + "/" + ds);
            return get;
        }

    protected static HttpResponse execute(final HttpUriRequest method)
        throws ClientProtocolException, IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                         method.getURI());
        return client.execute(method);
    }

    // Executes requests with preemptive basic authentication
    protected HttpResponse executeWithBasicAuth(final HttpUriRequest request,
                                                final String username,
                                                final String password)
        throws IOException {
        final HttpHost target = new HttpHost(HOSTNAME, SERVER_PORT, PROTOCOL);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(username, password));
        final CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();

        final AuthCache authCache = new BasicAuthCache();
        final BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);

        final HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);

        try (final CloseableHttpResponse response = httpclient.execute(request, localContext)) {
            return response;
        }
    }

    protected static int getStatus(final HttpUriRequest method)
        throws ClientProtocolException, IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn("Got status {}", result);
            if (response.getEntity() != null) {
                logger.warn(EntityUtils.toString(response.getEntity()));
            }
        }
        return result;
    }

    protected String getContentType(final HttpUriRequest method)
        throws ClientProtocolException, IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        assertEquals(OK.getStatusCode(), result);
        return response.getFirstHeader("Content-Type").getValue();
    }

    protected GraphStore getGraphStore(final HttpClient client, final HttpUriRequest method) throws IOException {

        if (method.getFirstHeader("Accept") == null) {
            method.addHeader("Accept", "application/n-triples");
        } else {
            logger.debug("Retrieving RDF in mimeType: {}", method
                    .getFirstHeader("Accept"));
        }

        final HttpResponse response = client.execute(method);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final GraphStore result = parseTriples(response.getEntity());
        logger.trace("Retrieved RDF: {}", result);
        return result;

    }
    protected GraphStore getGraphStore(final HttpResponse response) throws IOException {
        assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
        final GraphStore result = parseTriples(response.getEntity());
        logger.trace("Retrieved RDF: {}", result);
        return result;
    }

    protected GraphStore getGraphStore(final HttpUriRequest method) throws IOException {
        return getGraphStore(client, method);
    }

    protected HttpResponse createObject(final String pid) throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        if (pid.length() > 0) {
            httpPost.addHeader("Slug", pid);
        }
        final HttpResponse response = client.execute(httpPost);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

    protected HttpResponse createDatastream(final String pid, final String dsid, final String content)
            throws IOException {
        logger.trace(
                "Attempting to create datastream for object: {} at datastream ID: {}",
                pid, dsid);
        final HttpResponse response =
            client.execute(putDSMethod(pid, dsid, content));
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

    protected HttpResponse setProperty(final String pid,
                                       final String propertyUri,
                                       final String value) throws IOException {
        return setProperty(pid, null, propertyUri, value);
    }

    protected HttpResponse setProperty(final String pid, final String txId,
                                       final String propertyUri,
                                       final String value) throws IOException {
        final HttpPatch postProp = new HttpPatch(serverAddress
                + (txId != null ? txId + "/" : "") + pid);
        postProp.setHeader("Content-Type", "application/sparql-update");
        final String updateString =
                "INSERT { <"
                        + serverAddress + pid
                        + "> <" + propertyUri + "> \"" + value + "\" } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        final HttpResponse dcResp = execute(postProp);
        assertEquals(dcResp.getStatusLine().toString(),
                204, dcResp.getStatusLine().getStatusCode());
        postProp.releaseConnection();
        return dcResp;
    }

    /**
     * Creates a transaction, asserts that it's successful and
     * returns the transaction location.
     * @return
     * @throws IOException
     */
    protected String createTransaction() throws IOException {
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());
        return response.getFirstHeader("Location").getValue();
    }

    protected static void addMixin(final String pid, final String mixinUrl) throws IOException {
        final HttpPatch updateObjectGraphMethod =
                new HttpPatch(serverAddress + pid);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();

        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + mixinUrl + "> . } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    /**
     * Gets a random (but valid) pid for use in testing.  This pid
     * is guaranteed to be unique within runs of this application.
     */
    protected static String getRandomUniquePid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property name for use in testing.
     */
    protected static String getRandomPropertyName() {
        return randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property value for use in testing.
     */
    protected static String getRandomPropertyValue() {
        return randomUUID().toString();
    }

    protected static void assertDeleted(final String location) throws IOException {
        assertThat("Expected object to be deleted", getStatus(new HttpHead(location)), is(410));
        assertThat("Expected object to be deleted", getStatus(new HttpGet(location)), is(410));
    }
}
