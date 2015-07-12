/**
 * Copyright 2015 DuraSpace, Inc.
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


import org.apache.http.client.methods.HttpGet;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

import org.junit.Test;

import java.io.IOException;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.Class;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>FedoraNodeTypesIT class.</p>
 *
 * @author cbeer
 */
public class FedoraNodeTypesIT  extends AbstractResourceIT {

    @Test
    public void itShouldContainFcrepoClasses() throws IOException {
        final HttpGet httpGet = new HttpGet(serverAddress + "/fcr:nodetypes");
        httpGet.addHeader("Accept", "application/n-triples");
        try (final CloseableGraphStore graphStore = getGraphStore(httpGet)) {
            assertTrue(graphStore.contains(ANY,
                    createURI(REPOSITORY_NAMESPACE + "Resource"), type.asNode(), Class.asNode()));
            assertTrue(graphStore.contains(ANY,
                    createURI(REPOSITORY_NAMESPACE + "Container"), type.asNode(), Class.asNode()));
            assertTrue(graphStore.contains(ANY,
                    createURI(REPOSITORY_NAMESPACE + "NonRdfSourceDescription"), type.asNode(), Class.asNode()));
        }
    }

    @Test
    public void testResponseContentTypes() throws IOException {
        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + "fcr:nodetypes");
            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
     }
}
