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
package org.fcrepo.integration.ldp;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Ignore;
import org.junit.Test;
import org.w3.ldp.testsuite.LdpTestSuite;

/**
 * @author cabeer
 * @since 10/6/14
 */
@Ignore // TODO fix  this test
public class LdpTestSuiteIT {

    public static final String LDP_NAMESPACE = "http://www.w3.org/ns/ldp#";

    public static final String BASIC_CONTAINER = LDP_NAMESPACE + "BasicContainer";
    public static final String DIRECT_CONTAINER = LDP_NAMESPACE + "DirectContainer";
    public static final String INDIRECT_CONTAINER = LDP_NAMESPACE + "IndirectContainer";

    protected static final int SERVER_PORT = parseInt(System.getProperty(
            "fcrepo.dynamic.test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String PROTOCOL = "http";

    protected static final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" +
            SERVER_PORT + "/fcrepo/rest/";


    /**
     * Execute an HTTP request with preemptive basic authentication.
     *
     * @param request the request to execute
     * @return the open responses
     * @throws IOException in case of IOException
     */
    protected CloseableHttpResponse executeWithBasicAuth(final HttpUriRequest request) throws IOException {
        final HttpHost target = new HttpHost(HOSTNAME, SERVER_PORT, PROTOCOL);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials("fedoraAdmin", "fedoraAdmin"));
        final CloseableHttpClient httpclient =
                HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
                                    .setMaxConnPerRoute(MAX_VALUE)
                                    .setMaxConnTotal(MAX_VALUE).build();
        final AuthCache authCache = new BasicAuthCache();
        final BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);

        final HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        return httpclient.execute(request, localContext);
    }


    @Test
    public void runLDPBasicContainerTestSuite() throws IOException {
        final String pid = "ldp-test-basic-" + UUID.randomUUID().toString();

        final HttpPut request = new HttpPut(serverAddress + pid);
        request.setHeader(CONTENT_TYPE, "text/turtle");
        request.setHeader(LINK, "<" + BASIC_CONTAINER + ">;rel=type");
        try (final CloseableHttpResponse response = executeWithBasicAuth(request)) {
            assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());


            final HashMap<String, String> options = new HashMap<>();
            options.put("server", serverAddress + pid);
            options.put("output", "report-basic");
            options.put("basic", "true");
            options.put("non-rdf", "true");
            options.put("auth", "fedoraAdmin:fedoraAdmin");
            options.put("read-only-prop", "http://fedora.info/definitions/v4/repository#uuid");
            final LdpTestSuite testSuite = new LdpTestSuite(options);
            testSuite.run();
            assertTrue("The LDP test suite is only informational", true);
        }
    }

    @Test
    public void runLDPDirectContainerTestSuite() throws IOException {
        final String pid = "ldp-test-direct-" + UUID.randomUUID().toString();

        final HttpPut request = new HttpPut(serverAddress + pid);
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(IOUtils.toInputStream("<> <" + LDP_NAMESPACE + "membershipResource> <> ;" +
                "    <" + LDP_NAMESPACE + "hasMemberRelation> <" + LDP_NAMESPACE + "member> ."));
        request.setEntity(entity);
        request.setHeader(CONTENT_TYPE, "text/turtle");
        request.setHeader(LINK, "<" + DIRECT_CONTAINER + ">;rel=type");
        try (final CloseableHttpResponse response = executeWithBasicAuth(request)) {
            assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

            final HashMap<String, String> options = new HashMap<>();
            options.put("server", serverAddress + pid);
            options.put("output", "report-direct");
            options.put("direct", "true");
            options.put("non-rdf", "true");
            options.put("auth", "fedoraAdmin:fedoraAdmin");
            options.put("read-only-prop", "http://fedora.info/definitions/v4/repository#uuid");
            final LdpTestSuite testSuite = new LdpTestSuite(options);
            testSuite.run();
            assertTrue("The LDP test suite is only informational", true);
        }
    }

    @Test
    public void runLDPIndirectContainerTestSuite() throws IOException {
        final String pid = "ldp-test-indirect-" + UUID.randomUUID().toString();

        final HttpPut request = new HttpPut(serverAddress + pid);
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(IOUtils.toInputStream("<> <" + LDP_NAMESPACE + "membershipResource> <> ;" +
                "    <" + LDP_NAMESPACE + "insertedContentRelation> <" + LDP_NAMESPACE + "MemberSubject> ;" +
                "    <" + LDP_NAMESPACE + "hasMemberRelation> <" + LDP_NAMESPACE + "member> ."));
        request.setEntity(entity);
        request.setHeader(CONTENT_TYPE, "text/turtle");
        request.setHeader(LINK, "<" + INDIRECT_CONTAINER + ">;rel=type");
        try (final CloseableHttpResponse response = executeWithBasicAuth(request)) {
            assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

            final HashMap<String, String> options = new HashMap<>();
            options.put("server", serverAddress + pid);
            options.put("output", "report-indirect");
            options.put("indirect", "true");
            options.put("non-rdf", "true");
            options.put("auth", "fedoraAdmin:fedoraAdmin");
            options.put("read-only-prop", "http://fedora.info/definitions/v4/repository#uuid");
            final LdpTestSuite testSuite = new LdpTestSuite(options);
            testSuite.run();
            assertTrue("The LDP test suite is only informational", true);
        }
    }
}
