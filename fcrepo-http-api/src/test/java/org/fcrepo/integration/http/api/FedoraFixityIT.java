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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_CHECKSUM;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.RdfLexicon.IS_FIXITY_RESULT_OF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraFixityIT extends AbstractResourceIT {

    @Test
    public void testCheckDatastreamFixity() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest11");
        assertEquals(201, getStatus(objMethod));
        final HttpPost method1 =
                postDSMethod("FedoraDatastreamsTest11", "zxc", "foo");
        assertEquals(201, getStatus(method1));
        final HttpGet method2 =
                new HttpGet(serverAddress +
                        "FedoraDatastreamsTest11/zxc/fcr:fixity");
        method2.setHeader("Accept", "application/n3");
        final HttpResponse response = execute(method2);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final HttpEntity entity = response.getEntity();
        final GraphStore graphStore = parseTriples(entity.getContent());

        logger.info("Got triples {}", graphStore);

        assertTrue(graphStore.contains(ANY, ANY, IS_FIXITY_RESULT_OF.asNode(),
                createResource(serverAddress + "FedoraDatastreamsTest11/zxc")
                        .asNode()));
        assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(),
                createPlainLiteral("SUCCESS").asNode()));

        assertTrue(graphStore.contains(ANY, ANY, HAS_COMPUTED_CHECKSUM.asNode(),
                createResource("urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33").asNode()));
        assertTrue(graphStore.contains(ANY, ANY,
                HAS_COMPUTED_SIZE.asNode(), ResourceFactory
                        .createTypedLiteral(3).asNode()));
    }
}
