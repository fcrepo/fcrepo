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
package org.fcrepo.auth.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Test;


/**
 * <p>FedoraResponseCodesIT class.</p>
 *
 * @author gregjan
 */
public class FedoraResponseCodesIT extends AbstractResourceIT {

    @Test
    public void testAllowedAddDatastream() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";
        final HttpPut objMethod = putObjMethod(pid);
        assertEquals(201, getStatus(objMethod));

        final HttpPost method = postDSMethod(pid, "zxcpermit", "foo");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals("Got wrong URI in Location header for datastream creation!", serverAddress + pid +
                "/zxcpermit/jcr:content", location);
    }

    @Test
    public void testDeniedAddDatastream() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";
        final HttpPut objMethod = putObjMethod(pid);

        assertEquals(201, getStatus(objMethod));

        final HttpPut obj2Method = putObjMethod(pid + "/FedoraDatastreamsTest2Deny");
        assertEquals(201, getStatus(obj2Method));

        final HttpPost method = postDSMethod(pid + "/FedoraDatastreamsTest2Deny", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testAllowedAddDeepDatastream() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";
        final HttpPut method =
                putDSMethod(pid + "/does_permit/not_permit/exist_permit/yet_permit", "zxc_permit", "foo");

        final HttpResponse response = client.execute(method);
        final String location =
                response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals("Got wrong URI in Location header for datastream creation!", serverAddress + pid +
                "/does_permit/not_permit/exist_permit/yet_permit/zxc_permit/jcr:content", location);
    }

    @Test
    public void testDeniedAddDeepDatastream() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";
        final HttpPut method =
                putDSMethod(
                        pid + "/does_permit/not_permit/exist_permit/yet_permit/allowed_child",
                        "zxc", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testAllowedPutDatastream() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";
        final HttpPut objMethod = putObjMethod(pid);
        assertEquals(201, getStatus(objMethod));
        final HttpPut method = putDSMethod(pid, "zxc_permit", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(201, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testDeniedPutDatastream() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";

        final HttpPut objMethod = putObjMethod(pid + "/allowed_child");
        assertEquals(201, getStatus(objMethod));

        final HttpPut method = putDSMethod(pid + "/allowed_child", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    // @Test
    public void testGetDatastreamContent() throws Exception {
        final String pid = getRandomUniquePid() + "Permit";
        // TODO requires Grizzly client authN, see:
        // https://java.net/projects/jersey/sources/svn/content/trunk/jersey/samples/https-clientserver-grizzly/src
        // /main/java/com/sun/jersey/samples/https_grizzly/Server.java?rev=5853
        // https://java.net/projects/jersey/sources/svn/content/trunk/jersey/samples/https-clientserver-grizzly/src
        // /main/java/com/sun/jersey/samples/https_grizzly/auth/SecurityFilter.java?rev=5853

        final HttpPut objMethod =
            putObjMethod(pid);

        assertEquals(201, getStatus(objMethod));

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/jcr:content");
        assertEquals(200, getStatus(method_test_get));

        final HttpResponse response = client.execute(method_test_get);

        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response.getEntity())));

        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df",
                response.getFirstHeader("ETag").getValue().replace("\"", ""));

        logger.debug("Content was correct.");
    }
}
