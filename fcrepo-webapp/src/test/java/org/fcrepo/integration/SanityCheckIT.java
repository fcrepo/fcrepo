
package org.fcrepo.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanityCheckIT {

    /**
     * The server port of the application, set as system property by
     * maven-failsafe-plugin.
     */
    private static final String SERVER_PORT = System.getProperty("test.port");

    /**
    * The context path of the application (including the leading "/"), set as
    * system property by maven-failsafe-plugin.
    */
    private static final String CONTEXT_PATH = System
            .getProperty("test.context.path");

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + CONTEXT_PATH;

    protected static final PoolingClientConnectionManager connectionManager =
            new PoolingClientConnectionManager();

    protected static HttpClient client;

    static {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client = new DefaultHttpClient(connectionManager);
    }

    @Test
    public void doASanityCheck() throws IOException {
        assertEquals(200, getStatus(new HttpGet(serverAddress + "rest/")));
    }

    protected int getStatus(final HttpUriRequest method)
            throws ClientProtocolException, IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                method.getURI());
        return client.execute(method).getStatusLine().getStatusCode();
    }
}
