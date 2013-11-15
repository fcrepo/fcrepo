/**
 * Copyright 2013 DuraSpace, Inc.
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
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    protected static final int SERVER_PORT = parseInt(System.getProperty(
            "test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + "/";

    protected static HttpClient client;

    public AbstractResourceIT() {
        final HttpClientBuilder b =
            HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                    .setMaxConnTotal(MAX_VALUE);
        client = b.build();
    }

    protected static HttpPost postObjMethod(final String pid) {
        return new HttpPost(serverAddress + pid);
    }

    protected static HttpPost postObjMethod(final String pid, final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + pid);
        } else {
            return new HttpPost(serverAddress + pid + "?" + query);
        }
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

    protected int getStatus(final HttpUriRequest method)
        throws ClientProtocolException, IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
        }
        return result;
    }


    protected GraphStore getGraphStore(final HttpClient client, final HttpUriRequest method) throws IOException {

        if (method.getFirstHeader("Accept") == null) {
            method.addHeader("Accept", "text/n3");
        }

        final HttpResponse response = client.execute(method);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        return parseTriples(response.getEntity());

    }

    protected GraphStore getGraphStore(final HttpUriRequest method) throws IOException {
        return getGraphStore(client, method);
    }

    protected HttpResponse createObject(final String pid) throws IOException {
        final HttpResponse response = client.execute(postObjMethod(pid));
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

    protected HttpResponse createDatastream(final String pid, final String dsid, final String content) throws IOException {
        logger.trace(
                "Attempting to create datastream for object: {} at datastream ID: {}",
                pid, dsid);
        final HttpResponse response =
            client.execute(postDSMethod(pid, dsid, content));
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

}
