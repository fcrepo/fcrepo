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
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;

public class FedoraVersionsIT extends AbstractResourceIT {

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        createObject(pid);
        final HttpGet getVersion =
            new HttpGet(serverAddress + pid + "/fcr:versions");
        getVersion.addHeader("Accept", RDFMediaType.RDF_XML);
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
            createResource(serverAddress + pid + "/fcr:versions");
        assertTrue("Didn't find a version triple!",
            results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));
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
        getVersion.addHeader("Accept", RDFMediaType.RDF_XML);
        logger.info("Got version profile:");
        final GraphStore results = getGraphStore(getVersion);

        assertTrue("Didn't find a version triple!",
                      results.contains(Node.ANY, Node.ANY, HAS_PRIMARY_TYPE.asNode(), NodeFactory.createLiteral("nt:frozenNode")));
    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {

        final String pid = randomUUID().toString();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpGet getVersion =
            new HttpGet(serverAddress
                    + pid + "/ds1/fcr:versions/lastVersion");
        final HttpResponse resp = execute(getVersion);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }
}
