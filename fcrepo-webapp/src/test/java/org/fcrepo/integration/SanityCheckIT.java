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
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Link;

/**
 * <p>SanityCheckIT class.</p>
 *
 * @author fasseg
 */
public class SanityCheckIT {

    /**
     * The server port of the application, set as system property by
     * maven-failsafe-plugin.
     */
    private static final String SERVER_PORT = System.getProperty("fcrepo.dynamic.test.port");

    /**
    * The context path of the application (including the leading "/"), set as
    * system property by maven-failsafe-plugin.
    */
    private static final String CONTEXT_PATH = System
            .getProperty("fcrepo.test.context.path");

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH;

    protected static HttpClient client;

    static {
        client =
                HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                        .setMaxConnTotal(MAX_VALUE).build();
    }

    @Test
    public void doASanityCheck() throws IOException {
        executeAndVerify(new HttpGet(serverAddress + "rest/"), SC_OK);
    }

    private HttpResponse executeAndVerify(final HttpUriRequest method, final int statusCode) throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " + method.getURI());
        final HttpResponse response = client.execute(method);

        assertEquals(statusCode, response.getStatusLine().getStatusCode());
        return response;
    }

    @Test
    public void testConstraintLink() throws Exception {
        // Create a resource
        final HttpPost post = new HttpPost(serverAddress + "rest/");
        final HttpResponse postResponse = executeAndVerify(post, SC_CREATED);

        final String location = postResponse.getFirstHeader("Location").getValue();
        logger.debug("new resource location: {}", location);

        // GET the new resource
        final HttpGet get = new HttpGet(location);
        final HttpResponse getResponse = executeAndVerify(get, SC_OK);

        final String body = EntityUtils.toString(getResponse.getEntity());
        logger.debug("new resource body: {}", body);

        // PUT the exact body back to the new resource... successfully
        final HttpPut put = new HttpPut(location);
        put.setEntity(new StringEntity(body));
        put.setHeader("Content-Type", "text/turtle");
        executeAndVerify(put, SC_NO_CONTENT);

        // Update a server managed property in the resource body... not allowed!
        final String body2 = body.replaceFirst("fedora:created \"2\\d\\d\\d", "fedora:created \"1999");

        // PUT the erroneous body back to the new resource... unsuccessfully
        final HttpPut put2 = new HttpPut(location);
        put2.setEntity(new StringEntity(body2));
        put2.setHeader("Content-Type", "text/turtle");
        final HttpResponse put2Response = executeAndVerify(put2, SC_CONFLICT);

        // Verify the returned LINK header
        final String linkHeader = put2Response.getFirstHeader("Link").getValue();
        final Link link = Link.valueOf(linkHeader);
        logger.debug("constraint linkHeader: {}", linkHeader);

        // Verify the LINK rel
        final String linkRel = link.getRel();
        assertEquals(CONSTRAINED_BY.getURI(), linkRel);

        // Verify the LINK URI by fetching it
        final URI linkURI = link.getUri();
        logger.debug("constraint linkURI: {}", linkURI);

        final HttpGet getLink = new HttpGet(linkURI);
        executeAndVerify(getLink, SC_OK);
    }

}
