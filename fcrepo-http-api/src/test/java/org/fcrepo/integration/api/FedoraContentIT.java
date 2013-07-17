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
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraContentIT extends AbstractResourceIT {

    private static final String faulkner1 =
            "The past is never dead. It's not even past.";

    @Test
    public void testAddDatastream() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest2");
        assertEquals(201, getStatus(objMethod));
        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest2", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + OBJECT_PATH.replace("/", "") +
                        "/FedoraDatastreamsTest2/zxc/fcr:content", location);
    }

    @Test
    public void testAddDeepDatastream() throws Exception {
        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest2/does/not/exist/yet",
                        "zxc", "foo");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + OBJECT_PATH.replace("/", "") +
                        "/FedoraDatastreamsTest2/does/not/exist/yet/zxc/fcr:content",
                location);
    }

    @Test
    public void testPutDatastream() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTestPut");
        assertEquals(201, getStatus(objMethod));
        final HttpPut method =
                putDSMethod("FedoraDatastreamsTestPut", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + OBJECT_PATH.replace("/", "") +
                        "/FedoraDatastreamsTestPut/zxc/fcr:content", location);
    }

    @Test
    public void testMutateDatastream() throws Exception {
        final HttpPost createObjectMethod =
                postObjMethod("FedoraDatastreamsTest3");
        assertEquals("Couldn't create an object!", 201,
                getStatus(createObjectMethod));

        final HttpPost createDataStreamMethod =
                postDSMethod("FedoraDatastreamsTest3", "ds1", "foo");
        assertEquals("Couldn't create a datastream!", 201,
                getStatus(createDataStreamMethod));

        final HttpPut mutateDataStreamMethod =
                putDSMethod("FedoraDatastreamsTest3", "ds1", "bar");
        mutateDataStreamMethod.setEntity(new StringEntity(faulkner1, "UTF-8"));
        final HttpResponse response = client.execute(mutateDataStreamMethod);
        int status = response.getStatusLine().getStatusCode();
        if (status != 204) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals("Couldn't mutate a datastream!", 204, status);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest3/ds1/fcr:content");
        assertTrue("Datastream didn't accept mutation!", faulkner1
                .equals(EntityUtils.toString(client.execute(
                        retrieveMutatedDataStreamMethod).getEntity())));
    }

    @Test
    public void testGetDatastreamContent() throws Exception {
        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest6");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest6", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));
        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest6/ds1/fcr:content");
        assertEquals(200, getStatus(method_test_get));
        final HttpResponse response = client.execute(method_test_get);
        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response.getEntity())));

        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df",
                response.getFirstHeader("ETag").getValue().replace("\"", ""));

        logger.debug("Content was correct.");
    }

    @Test
    public void testRefetchingDatastreamContent() throws Exception {

        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest61");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest61", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final SimpleDateFormat format =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest61/ds1/fcr:content");
        method_test_get.setHeader("If-None-Match",
                "\"urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df\"");
        method_test_get.setHeader("If-Modified-Since", format
                .format(new Date()));

        assertEquals(304, getStatus(method_test_get));

    }

    @Test
    public void testConditionalPutOfDatastreamContent() throws Exception {

        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest62");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest62", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpPut method_test_put =
                new HttpPut(serverAddress +
                        "objects/FedoraDatastreamsTest62/ds1/fcr:content");
        method_test_put.setHeader("If-Match",
                "\"urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df\"");
        method_test_put.setHeader("If-Unmodified-Since",
                "Sat, 29 Oct 1994 19:43:31 GMT");

        method_test_put.setEntity(new StringEntity("asdf"));

        assertEquals(412, getStatus(method_test_put));

    }

    @Test
    public void testRangeRequest() throws Exception {
        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest63");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest63", "ds1",
                                    "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                                    "objects/FedoraDatastreamsTest63/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=1-3");
        assertEquals(206, getStatus(method_test_get));
        final HttpResponse response = client.execute(method_test_get);
        logger.debug("Returned from HTTP GET, now checking content...");
        assertEquals("Got the wrong content back!", "arb",EntityUtils.toString(response.getEntity()));
        assertEquals("bytes 1-3/20", response.getFirstHeader("Content-Range").getValue());

    }

    @Test
    public void testRangeRequestBadRange() throws Exception {
        final HttpPost createObjMethod =
            postObjMethod("FedoraDatastreamsTest64");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
            postDSMethod("FedoraDatastreamsTest64", "ds1",
                            "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get =
            new HttpGet(serverAddress +
                            "objects/FedoraDatastreamsTest64/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=50-100");
        assertEquals(416, getStatus(method_test_get));
        final HttpResponse response = client.execute(method_test_get);
        assertEquals("bytes 50-100/20", response.getFirstHeader("Content-Range").getValue());

    }
}
