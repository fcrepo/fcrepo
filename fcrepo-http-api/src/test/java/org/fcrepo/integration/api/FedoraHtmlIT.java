
package org.fcrepo.integration.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FedoraHtmlIT extends AbstractResourceIT {

    @Test
    public void testGetRoot() throws Exception {

        final HttpGet method = new HttpGet(serverAddress);
        method.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testGetNode() throws Exception {

        final HttpPost postMethod = postObjMethod("FedoraHtmlObject");
        final HttpResponse postResponse = client.execute(postMethod);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        final HttpGet method =
                new HttpGet(serverAddress + "objects/FedoraHtmlObject");
        method.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testGetDatastreamNode() throws Exception {

        final HttpPost postMethod = postObjMethod("FedoraHtmlObject2");

        final HttpResponse postResponse = client.execute(postMethod);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        final HttpPost postDsMethod =
                postDSMethod("FedoraHtmlObject2", "ds1", "foo");
        assertEquals(201, getStatus(postDsMethod));

        final HttpGet method =
                new HttpGet(serverAddress + "objects/FedoraHtmlObject2/ds1");
        method.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testGetNamespaces() throws Exception {

        final HttpGet method = new HttpGet(serverAddress + "fcr:namespaces");
        method.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());

    }
}
