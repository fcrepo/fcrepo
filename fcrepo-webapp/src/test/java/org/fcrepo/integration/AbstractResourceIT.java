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
package org.fcrepo.integration;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;

import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.slf4j.Logger;

/**
 * Base class for ITs
 * @author awoods
 * @author escowles
**/
public abstract class AbstractResourceIT {

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    protected static final int SERVER_PORT = parseInt(System.getProperty(
            "fcrepo.dynamic.test.port", "8080"));

    private static final String CONTEXT_PATH = System
            .getProperty("fcrepo.test.context.path");

    protected static final String HOSTNAME = "localhost";

    protected static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH + "rest/";

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

    protected static HttpPost postObjMethod(final String pid, final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + pid);
        }
        return new HttpPost(serverAddress + pid + "?" + query);
    }

    protected static HttpPost postDSMethod(final String pid, final String ds,
        final String content) throws UnsupportedEncodingException {
        final HttpPost post =
                new HttpPost(serverAddress + pid + "/" + ds +
                        "/fcr:content");
        post.setEntity(new StringEntity(content));
        return post;
    }

    protected static HttpPut putDSMethod(final String pid, final String ds,
        final String content) throws UnsupportedEncodingException {
        final HttpPut put =
                new HttpPut(serverAddress + pid + "/" + ds +
                        "/fcr:content");

        put.setEntity(new StringEntity(content));
        return put;
    }

    protected HttpResponse execute(final HttpUriRequest method)
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
        try (final CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {

            final AuthCache authCache = new BasicAuthCache();
            final BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);

            final HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            final CloseableHttpResponse response = httpclient.execute(request, localContext);
            return response;
        }
    }


    protected int getStatus(final HttpUriRequest method)
        throws ClientProtocolException, IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
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
            client.execute(postDSMethod(pid, dsid, content));
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
     *
     * @return a random UUID
     */
    protected static String getRandomUniquePid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property name for use in testing.
     *
     * @return a random property name
     */
    protected static String getRandomPropertyName() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property value for use in testing.
     *
     * @return a random property value
     */
    protected static String getRandomPropertyValue() {
        return UUID.randomUUID().toString();
    }


}
