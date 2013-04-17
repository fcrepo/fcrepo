
package org.fcrepo.integration.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    protected Logger logger;

    protected JAXBContext context;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Before
    public void setContext() throws JAXBException {
        context =
                JAXBContext.newInstance(ObjectProfile.class,
                        ObjectDatastreams.class, DatastreamProfile.class,
                        DatastreamFixity.class);
    }

    protected static final int SERVER_PORT = Integer.parseInt(System
            .getProperty("test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

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

    protected static HttpPost postObjMethod(final String pid) {
        return new HttpPost(serverAddress + "objects/" + pid);
    }

    protected static HttpPost
            postObjMethod(final String pid, final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + "objects/" + pid);
        } else {
            return new HttpPost(serverAddress + "objects/" + pid + "?" + query);
        }
    }

    protected static HttpPost postDSMethod(final String pid, final String ds,
            final String content) throws UnsupportedEncodingException {
        final HttpPost post =
                new HttpPost(serverAddress + "objects/" + pid +
                        "/datastreams/" + ds);
        post.setEntity(new StringEntity(content));
        return post;
    }

    protected static HttpPut putDSMethod(final String pid, final String ds) {
        return new HttpPut(serverAddress + "objects/" + pid + "/datastreams/" +
                ds);
    }

    protected HttpResponse execute(HttpUriRequest method)
            throws ClientProtocolException, IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                method.getURI());
        return client.execute(method);
    }

    protected int getStatus(HttpUriRequest method)
            throws ClientProtocolException, IOException {
        return execute(method).getStatusLine().getStatusCode();
    }

    protected ObjectProfile getObject(String pid)
            throws ClientProtocolException, IOException, JAXBException {
        final HttpGet get = new HttpGet(serverAddress + "objects/" + pid);
        HttpResponse resp = execute(get);
        Unmarshaller um = context.createUnmarshaller();
        return (ObjectProfile) um.unmarshal(resp.getEntity().getContent());
    }
}
