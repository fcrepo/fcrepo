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
package org.fcrepo.auth.integration;

import org.apache.http.HttpResponse;
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
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * <p>Abstract AbstractResourceIT class.</p>
 *
 * @author gregjan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    protected static final int SERVER_PORT = Integer.parseInt(System
            .getProperty("fcrepo.dynamic.test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME +
            ":" + SERVER_PORT + "/";

    protected static HttpClient client;

    public AbstractResourceIT() {
        client =
            HttpClientBuilder.create().setMaxConnPerRoute(5).setMaxConnTotal(
                    Integer.MAX_VALUE).build();
    }

    protected static HttpPost postObjMethod(final String pid) {
        return new HttpPost(serverAddress + pid);
    }

    protected static HttpPut putObjMethod(final String pid) {
        return new HttpPut(serverAddress + pid);
    }

    protected static HttpPost postObjMethod(final String pid,
            final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + pid);
        }
        return new HttpPost(serverAddress + pid + "?" + query);
    }

    protected static HttpPost postDSMethod(final String pid,
            final String ds, final String content)
            throws UnsupportedEncodingException {
        final HttpPost post =
                new HttpPost(serverAddress + pid + "/" + ds +
                        "/jcr:content");
        post.setEntity(new StringEntity(content));
        return post;
    }

    protected static HttpPut putDSMethod(final String pid,
            final String ds, final String content)
            throws UnsupportedEncodingException {
        final HttpPut put =
                new HttpPut(serverAddress + pid + "/" + ds +
                        "/jcr:content");

        put.setEntity(new StringEntity(content));
        return put;
    }

    protected HttpResponse execute(final HttpUriRequest method)
            throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                method.getURI());
        return client.execute(method);
    }

    protected int getStatus(final HttpUriRequest method)
            throws IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
        }
        return result;
    }

    /**
     * Gets a random (but valid) pid for use in testing. This pid is guaranteed
     * to be unique within runs of this application.
     *
     * @return  A string containing the new Pid
     */
    protected static String getRandomUniquePid() {
        return UUID.randomUUID().toString();
    }
}
