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

package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

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
                new HttpGet(serverAddress + "FedoraHtmlObject");
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
                new HttpGet(serverAddress + "FedoraHtmlObject2/ds1");
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
