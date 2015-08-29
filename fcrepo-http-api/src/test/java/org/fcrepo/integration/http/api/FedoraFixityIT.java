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
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.RdfLexicon.HAS_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.junit.Test;

import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>FedoraFixityIT class.</p>
 *
 * @author awoods
 */
public class FedoraFixityIT extends AbstractResourceIT {

    @Test
    public void testCheckDatastreamFixity() throws Exception {
        final String pid = getRandomUniqueId();

        createObject(pid);
        createDatastream(pid, "zxc", "foo");

        final HttpGet method = new HttpGet(serverAddress + pid + "/zxc/fcr:fixity");
        final GraphStore graphStore = getGraphStore(method);
        logger.debug("Got triples {}", graphStore);

        assertTrue(graphStore.contains(ANY,
                                          createResource(serverAddress + pid + "/zxc").asNode(),
                                          HAS_FIXITY_RESULT.asNode(),
                                          ANY
                ));
        assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(),
                createPlainLiteral("SUCCESS").asNode()));

        assertTrue(graphStore.contains(ANY, ANY,
                HAS_MESSAGE_DIGEST.asNode(), createResource(
                        "urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33")
                        .asNode()));
        assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(),
                createTypedLiteral(3).asNode()));
    }

    @Test
    public void testResponseContentTypes() throws Exception {
        final String pid = getRandomUniqueId();
        createObject(pid);
        createDatastream(pid, "zxc", "foo");

        for (final String type : RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method =
                    new HttpGet(serverAddress + pid + "/zxc/fcr:fixity");

            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testBinaryVersionFixity() throws Exception {
        final String pid = getRandomUniqueId();

        createObject(pid);
        createDatastream(pid, "dsid", "foo");

        logger.debug("Creating binary content version v0 ...");
        postVersion(pid + "/dsid", "v0");

        final HttpGet method = new HttpGet(serverAddress + pid + "/dsid/fcr%3aversions/v0/fcr:fixity");
        final GraphStore graphStore = getGraphStore(method);
        logger.debug("Got binary content versioned fixity triples {}", graphStore);
        final Iterator<Quad> stmtIt = graphStore.find(ANY, ANY, HAS_FIXITY_RESULT.asNode(), ANY);
        assertTrue(stmtIt.hasNext());
        assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(),
                createPlainLiteral("SUCCESS").asNode()));

        assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST.asNode(), ANY));
        assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(),
                createTypedLiteral(3).asNode()));
    }

    private static void postVersion(final String path, final String label) throws IOException {
        logger.debug("Posting version");
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions");
        postVersion.addHeader("Slug", label);
        final HttpResponse response = execute(postVersion);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode() );
        final String locationHeader = response.getFirstHeader("Location").getValue();
        assertNotNull( "No version location header found", locationHeader );
    }
}
