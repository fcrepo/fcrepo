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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

import org.junit.Test;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.sparql.core.Quad;

/**
 * <p>FedoraFixityIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
public class FedoraFixityIT extends AbstractResourceIT {

    private static final RDFDatatype IntegerType = TypeMapper.getInstance().getTypeByClass(Integer.class);

    @Test
    public void testCheckDatastreamFixity() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "zxc", "foo");

        try (final CloseableGraphStore graphStore =
                getGraphStore(new HttpGet(serverAddress + id + "/zxc/fcr:fixity"))) {
            logger.debug("Got triples {}", graphStore);

            assertTrue(graphStore.contains(ANY,
                    createURI(serverAddress + id + "/zxc"), HAS_FIXITY_RESULT.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("SUCCESS")));
            assertTrue(graphStore.contains(ANY,
                    ANY, HAS_MESSAGE_DIGEST.asNode(), createURI("urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("3", IntegerType)));
        }
    }

    @Test
    public void testResponseContentTypes() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "zxc", "foo");

        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + id + "/zxc/fcr:fixity");
            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testBinaryVersionFixity() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "dsid", "foo");

        logger.debug("Creating binary content version v0 ...");
        postVersion(id + "/dsid", "v0");

        try (final CloseableGraphStore graphStore =
                getGraphStore(new HttpGet(serverAddress + id + "/dsid/fcr%3aversions/v0/fcr:fixity"))) {
            logger.debug("Got binary content versioned fixity triples {}", graphStore);
            final Iterator<Quad> stmtIt = graphStore.find(ANY, ANY, HAS_FIXITY_RESULT.asNode(), ANY);
            assertTrue(stmtIt.hasNext());
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("SUCCESS")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("3", IntegerType)));
        }
    }

    private static void postVersion(final String path, final String label) throws IOException {
        logger.debug("Posting version");
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions");
        postVersion.addHeader("Slug", label);
        try (final CloseableHttpResponse response = execute(postVersion)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            final String locationHeader = getLocation(response);
            assertNotNull("No version location header found", locationHeader);
        }
    }
}
