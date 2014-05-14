/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.storage.policy;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Abstract AbstractResourceIT class.</p>
 *
 * @author osmandin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    protected static final int SERVER_PORT = Integer.parseInt(System
        .getProperty("test.port", "8080"));

    protected static final String HOSTNAME = "localhost";
    
    protected static final String SUFFIX = "fcr:storagepolicy";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
        SERVER_PORT + "/rest/";

    protected final PoolingClientConnectionManager connectionManager =
        new PoolingClientConnectionManager();

    protected static HttpClient client;

    public AbstractResourceIT() {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client = new DefaultHttpClient(connectionManager);
    }

    protected HttpGet HttpGetObjMethod(final String param) {
        HttpGet get = new HttpGet(serverAddress + param + "/" + SUFFIX);
        logger.debug("GET: {}", get.getURI());
        return get;
    }
    
    protected HttpPost HttpPostObjMethod(String param) {
        HttpPost post = new HttpPost(serverAddress + param + "/" + SUFFIX);
        logger.debug("POST: {}", post.getURI());
        return post;
    }
    
    protected HttpDelete HttpDeleteObjMethod(final String param) {
        HttpDelete delete = new HttpDelete(serverAddress + param + "/" + SUFFIX);
        logger.debug("DELETE: {}", delete.getURI());
        return delete;
    }
    
    protected HttpResponse execute(final HttpUriRequest method)
        throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
            method.getURI());
        return client.execute(method);
    }

    protected int getStatus(final HttpUriRequest method)
        throws IOException {
        HttpResponse response = execute(method);
        int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
        }
        return result;
    }

}
