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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class FedoraVersionsIT extends AbstractResourceIT {

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        execute(postObjMethod(pid));
        final HttpGet method =
            new HttpGet(serverAddress + pid + "/fcr:versions");
        final HttpResponse resp = execute(method);
        final String profile = EntityUtils.toString(resp.getEntity());
        assertEquals("Failed to retrieve version profile!\n" + profile, 200,
                resp.getStatusLine().getStatusCode());
        logger.debug("Retrieved version profile:");
        final Model results = extract(profile);
        final Resource subject =
            createResource(serverAddress + pid + "/fcr:versions");
        assertTrue("Didn't find a version triple!", results.contains(subject,
                HAS_VERSION, (RDFNode) null));
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
        logger.info("Got version profile:");
        final Model results = extract(version);
        assertTrue("Found no version!", results.contains(null,
                HAS_PRIMARY_TYPE, "nt:frozenNode"));
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
