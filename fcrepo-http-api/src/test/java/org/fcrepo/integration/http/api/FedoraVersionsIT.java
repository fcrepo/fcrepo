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

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraVersionsIT extends AbstractResourceIT {

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest1"));
        final HttpGet method =
            new HttpGet(serverAddress + "FedoraDatastreamsTest1/fcr:versions");
        final HttpResponse resp = execute(method);
        final String profile = EntityUtils.toString(resp.getEntity());
        assertEquals("Failed to retrieve version profile!\n" + profile, 200,
                resp.getStatusLine().getStatusCode());
        logger.debug("Retrieved version profile: \n{}", profile);
    }

    @Test
    public void testAddVersion() throws Exception {
        execute(postObjMethod("FedoraVersioningTest2"));

        final HttpPost postVersion =
            postObjMethod("FedoraVersioningTest2/fcr:versions/v0.0.1");
        execute(postVersion);
        assertEquals(204, getStatus(postVersion));

        final HttpGet getVersion =
            new HttpGet(serverAddress
                    + "FedoraVersioningTest2/fcr:versions/v0.0.1");
        final HttpResponse resp = execute(getVersion);
        final String version = EntityUtils.toString(resp.getEntity());
        assertEquals("Failed to retrieve new version!\n" + version, 200, resp
                .getStatusLine().getStatusCode());
        logger.info("Got version profile: {}", version);
    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest1"));

        final HttpPost postDs = postDSMethod("FedoraDatastreamsTest1", "ds1", "foo");
        execute(postDs);
        assertEquals(201, getStatus(postDs));

        final HttpGet getVersion =
            new HttpGet(serverAddress
                    + "FedoraDatastreamsTest1/ds1/fcr:versions/lastVersion");
        final HttpResponse resp = execute(getVersion);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }
}
