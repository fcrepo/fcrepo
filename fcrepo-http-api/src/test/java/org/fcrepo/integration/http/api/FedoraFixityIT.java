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
package org.fcrepo.integration.http.api;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.api.FedoraTypes.DEFAULT_DIGEST_ALGORITHM;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST_ALGORITHM;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.jena.sparql.core.Quad;
import org.fcrepo.http.commons.test.util.CloseableDataset;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 * <p>FedoraFixityIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
public class FedoraFixityIT extends AbstractResourceIT {

    private static final RDFDatatype IntegerType = TypeMapper.getInstance().getTypeByClass(Integer.class);
    private static final RDFDatatype StringType = TypeMapper.getInstance().getTypeByClass(String.class);

    @Test
    public void testCheckDatastreamFixity() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "zxc", "foo");

        try (final CloseableDataset dataset =
                getDataset(new HttpGet(serverAddress + id + "/zxc/fcr:fixity"))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            logger.debug("Got triples {}", graphStore);

            assertTrue(graphStore.contains(ANY,
                    createURI(serverAddress + id + "/zxc"), HAS_FIXITY_RESULT.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("SUCCESS")));
            assertTrue(graphStore.contains(ANY,
                    ANY, HAS_MESSAGE_DIGEST.asNode(), createURI("urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST_ALGORITHM.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("3", IntegerType)));
        }
    }

    @Test
    public void testCheckDatastreamFixityMD5() throws IOException {
        final String id = getRandomUniqueId();
        final String childId = id + "/child";

        final HttpPut put = putDSMethod(id, "child", "text-body");
        put.setHeader("Digest", "md5=888c09337d6869fa48bbeff802ddd05a");
        assertEquals("Did not create successfully!", CREATED.getStatusCode(), getStatus(put));

        // Set default digest algorithm
        final HttpPatch patch = patchObjMethod(childId + "/fcr:metadata");
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString = "PREFIX fedoraconfig: <http://fedora.info/definitions/v4/config#>\n" +
                "INSERT DATA { <> " + DEFAULT_DIGEST_ALGORITHM + " \"md5\" }";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Did not update successfully!", NO_CONTENT.getStatusCode(), getStatus(patch));

        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + childId + "/fcr:fixity"))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            logger.debug("Got triples {}", graphStore);

            assertTrue(graphStore.contains(ANY,
                    createURI(serverAddress + childId), HAS_FIXITY_RESULT.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("SUCCESS")));
            assertTrue(graphStore.contains(ANY,
                    ANY, HAS_MESSAGE_DIGEST.asNode(), createURI("urn:md5:888c09337d6869fa48bbeff802ddd05a")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST_ALGORITHM.asNode(),
                    createLiteral("MD5", StringType)));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("9", IntegerType)));
        }
    }

    @Test
    public void testFixityHeaders() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "zxc", "foo");

        final Link RDF_SOURCE_LINK = fromUri(RDF_SOURCE.getURI()).rel(type.getLocalName()).build();

        final HttpHead head = new HttpHead(serverAddress + id + "/zxc/fcr:fixity");

        try (final CloseableHttpResponse response = execute(head)) {
            assertEquals(OK.getStatusCode(), getStatus(response));

            final Collection<String> linkHeaders = getLinkHeaders(response);

            final Set<Link> resultSet = linkHeaders.stream().map(Link::valueOf).flatMap(link -> {
                final String linkRel = link.getRel();
                final URI linkUri = link.getUri();
                if (linkRel.equals(RDF_SOURCE_LINK.getRel()) && linkUri.equals(RDF_SOURCE_LINK.getUri())) {
                    // Found RdfSource!
                    return of(RDF_SOURCE_LINK);
                }
                return empty();
            }).collect(Collectors.toSet());
            assertTrue("No link headers found!", !linkHeaders.isEmpty());
            assertTrue("Didn't find RdfSource link header! " + RDF_SOURCE_LINK + " ?= " + linkHeaders,
                resultSet.contains(RDF_SOURCE_LINK));
        }
    }

    @Test
    public void testResponseContentTypes() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "zxc", "foo");

        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + id + "/zxc/fcr:fixity");
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    @Ignore("Until implemented with Memento")
    public void testBinaryVersionFixity() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "dsid", "foo");

        logger.debug("Creating binary content version v0 ...");
        postVersion(id + "/dsid", "v0");

        try (final CloseableDataset dataset =
                     getDataset(new HttpGet(serverAddress + id + "/dsid/fcr%3aversions/v0/fcr:fixity"))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
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
