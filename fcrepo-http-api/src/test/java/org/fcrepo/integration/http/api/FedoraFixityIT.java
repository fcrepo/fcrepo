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
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_CHECKSUM;
import static org.fcrepo.kernel.RdfLexicon.HAS_COMPUTED_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.RdfLexicon.IS_FIXITY_RESULT_OF;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import com.hp.hpl.jena.update.GraphStore;

public class FedoraFixityIT extends AbstractResourceIT {

    @Test
    public void testCheckDatastreamFixity() throws Exception {
        createObject("FedoraDatastreamsTest11");
        createDatastream("FedoraDatastreamsTest11", "zxc", "foo");

        final HttpGet method =
            new HttpGet(serverAddress
                    + "FedoraDatastreamsTest11/zxc/fcr:fixity");

        final GraphStore graphStore = getGraphStore(method);
        logger.info("Got triples {}", graphStore);

        assertTrue(graphStore.contains(ANY, ANY, IS_FIXITY_RESULT_OF.asNode(),
                createResource(serverAddress + "FedoraDatastreamsTest11/zxc")
                        .asNode()));
        assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(),
                createPlainLiteral("SUCCESS").asNode()));

        assertTrue(graphStore.contains(ANY, ANY,
                HAS_COMPUTED_CHECKSUM.asNode(), createResource(
                        "urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33")
                        .asNode()));
        assertTrue(graphStore.contains(ANY, ANY, HAS_COMPUTED_SIZE.asNode(),
                createTypedLiteral(3).asNode()));
    }
}
