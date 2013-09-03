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

package org.fcrepo.auth.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

public class FedoraResponseCodesIT extends AbstractResourceIT {

    private static final String faulkner1 =
            "The past is never dead. It's not even past.";

    @Before
    public void setup() throws ClientProtocolException, IOException {
        final HttpPost objMethod = postObjMethod("Permit");
        assertEquals(201, getStatus(objMethod));
    }

    @Test
    public void testAllowedAddDatastream() throws Exception {
        final HttpPost objMethod =
                postObjMethod("FedoraDatastreamsTest2Permit");

        assertEquals(201, getStatus(objMethod));
        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest2Permit", "zxcpermit",
                        "foo");
        final HttpResponse response = client.execute(method);
        final String location =
                response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress +
                        "FedoraDatastreamsTest2Permit/zxcpermit/fcr:content",
                location);
    }

    @Test
    public void testDeniedAddDatastream() throws Exception {
        final HttpPost objMethod =
                postObjMethod("FedoraDatastreamsTest2Permit");

        assertEquals(201, getStatus(objMethod));
        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest2Permit", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testAllowedAddDeepDatastream() throws Exception {
        final HttpPost method =
                postDSMethod(
                        "FedoraDatastreamsTest2_permit/does_permit/not_permit/exist_permit/yet_permit",
                        "zxc_permit", "foo");
        final HttpResponse response = client.execute(method);
        final String location =
                response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress +
                        "FedoraDatastreamsTest2_permit/does_permit/not_permit/exist_permit/yet_permit/zxc_permit/fcr:content",
                location);
    }

    @Test
    public void testDeniedAddDeepDatastream() throws Exception {
        final HttpPost method =
                postDSMethod(
                        "FedoraDatastreamsTest2_permit/does_permit/not_permit/exist_permit/yet_permit",
                        "zxc", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testAllowedPutDatastream() throws Exception {
        final HttpPost objMethod =
                postObjMethod("FedoraDatastreamsTestPut_permit");
        assertEquals(201, getStatus(objMethod));
        final HttpPut method =
                putDSMethod("FedoraDatastreamsTestPut_permit",
                        "zxc_permit", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(204, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testDeniedPutDatastream() throws Exception {
        final HttpPost objMethod =
                postObjMethod("FedoraDatastreamsTestPut_permit");
        assertEquals(201, getStatus(objMethod));
        final HttpPut method =
                putDSMethod("FedoraDatastreamsTestPut_permit", "zxc",
                        "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    // @Test
    public void testGetDatastreamContent() throws Exception {
        // TODO requires Grizzly client authN, see:
        // https://java.net/projects/jersey/sources/svn/content/trunk/jersey/samples/https-clientserver-grizzly/src/main/java/com/sun/jersey/samples/https_grizzly/Server.java?rev=5853
        // https://java.net/projects/jersey/sources/svn/content/trunk/jersey/samples/https-clientserver-grizzly/src/main/java/com/sun/jersey/samples/https_grizzly/auth/SecurityFilter.java?rev=5853
        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest6");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest6", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));
        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "FedoraDatastreamsTest6/ds1/fcr:content");
        assertEquals(200, getStatus(method_test_get));
        final HttpResponse response = client.execute(method_test_get);
        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response.getEntity())));

        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df",
                response.getFirstHeader("ETag").getValue().replace("\"",
                        ""));

        logger.debug("Content was correct.");
    }
}
