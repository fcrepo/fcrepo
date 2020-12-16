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

import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static javax.ws.rs.core.Response.Status.CREATED;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_FIXITY;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.jena.sparql.core.Quad;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.http.commons.test.util.CloseableDataset;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.junit.Before;
import org.junit.Test;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.sparql.core.DatasetGraph;
import org.springframework.test.context.TestExecutionListeners;

/**
 * <p>FedoraFixityIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraFixityIT extends AbstractResourceIT {

    private static final RDFDatatype IntegerType = TypeMapper.getInstance().getTypeByClass(Integer.class);
    private static final RDFDatatype StringType = TypeMapper.getInstance().getTypeByClass(String.class);

    private OcflPropsConfig ocflConfig;
    private OcflRepository ocflRepo;

    @Before
    public void setup() {
        ocflConfig = getBean(OcflPropsConfig.class);
        ocflRepo = getBean(OcflRepository.class);
    }

    @Test
    public void testCheckDatastreamFixity() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "zxc", "foo");

        final String digestValue = "0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33";

        final HttpGet httpGet = getObjMethod(id + "/zxc");
        httpGet.setHeader("Want-Digest", "sha");
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertDigestEquals(response, "sha", digestValue);
        }
    }

    @Test
    public void testCheckDatastreamFixityMD5() throws IOException {
        final String id = getRandomUniqueId();
        final String digestValue = "888c09337d6869fa48bbeff802ddd05a";

        final HttpPut put = putObjMethod(id, "text/plain", "text-body");
        put.setHeader("Digest", "md5=" + digestValue);
        assertEquals("Did not create successfully!", CREATED.getStatusCode(), getStatus(put));

        final HttpGet getFixity = getObjMethod(id);
        getFixity.setHeader("Want-Digest", "md5");
        try (final CloseableHttpResponse response = execute(getFixity)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertDigestEquals(response, "md5", digestValue);
        }
    }

    @Test
    public void testMultipleAlgorithms() throws IOException {
        final String id = getRandomUniqueId();
        final String payload = "This is some wonderful test text payload";
        final Map<String, String> shas = Map.of("sha","dce87c75d05bb3893ff5e72f0596754bcf47cfed",
            "sha-256", "55bf3e1931dffbf16fa7bfb80c000c04e6f17c75471a622af3b11a3511e17843",
            "md5", "6ddfc43540f3e0abdb5c54ca832bc9e3");

        final HttpPut httpPut = putObjMethod(id,"text/plain", payload);
        httpPut.setHeader("Digest", shas.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")));
        assertEquals(CREATED.getStatusCode(), getStatus(httpPut));

        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Want-Digest", String.join(",", shas.keySet()));
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            for (final Map.Entry<String, String> digest : shas.entrySet()) {
                assertDigestEquals(response, digest.getKey(), digest.getValue());
            }
        }

    }

    @Test
    public void testBinaryFixity() throws IOException {
        final String id = getRandomUniqueId();
        final FedoraId resourceId = FedoraId.create(id);
        final String uri = createDatastream(id, "foo");

        try (final CloseableDataset dataset =
                     getDataset(new HttpGet(uri + "/" + FCR_FIXITY))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            logger.debug("Got binary content fixity triples {}", graphStore);
            final Iterator<Quad> stmtIt = graphStore.find(ANY, ANY, HAS_FIXITY_RESULT.asNode(), ANY);
            assertTrue(stmtIt.hasNext());
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("SUCCESS")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("3", IntegerType)));
        }


        final var ocflDesc = ocflRepo.describeVersion(
                ObjectVersionId.head(resourceId.getResourceId()));
        final var storagePath = ocflDesc.getFile(id).getStorageRelativePath();
        Files.writeString(ocflConfig.getOcflRepoRoot().resolve(storagePath),
                "corrupted!", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        try (final CloseableDataset dataset =
                     getDataset(new HttpGet(uri + "/" + FCR_FIXITY))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            logger.debug("Got binary content fixity triples {}", graphStore);
            final Iterator<Quad> stmtIt = graphStore.find(ANY, ANY, HAS_FIXITY_RESULT.asNode(), ANY);
            assertTrue(stmtIt.hasNext());
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("BAD_CHECKSUM")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("3", IntegerType)));
        }
    }


    @Test
    public void testBinaryVersionFixity() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id,"foo");

        logger.debug("Creating binary content version ...");
        final String mementoUri = postVersion(id);

        try (final CloseableDataset dataset =
                     getDataset(new HttpGet(mementoUri + "/" + FCR_FIXITY))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            logger.debug("Got binary content versioned fixity triples {}", graphStore);
            final Iterator<Quad> stmtIt = graphStore.find(ANY, ANY, HAS_FIXITY_RESULT.asNode(), ANY);
            assertTrue(stmtIt.hasNext());
            assertTrue(graphStore.contains(ANY, ANY, HAS_FIXITY_STATE.asNode(), createLiteral("SUCCESS")));
            assertTrue(graphStore.contains(ANY, ANY, HAS_MESSAGE_DIGEST.asNode(), ANY));
            assertTrue(graphStore.contains(ANY, ANY, HAS_SIZE.asNode(), createLiteral("3", IntegerType)));
        }
    }

    private static String postVersion(final String path) throws IOException {
        logger.debug("Posting version");
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions");
        try (final CloseableHttpResponse response = execute(postVersion)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            final String locationHeader = getLocation(response);
            assertNotNull("No version location header found", locationHeader);
            return locationHeader;
        }
    }

    private static void assertDigestEquals(final CloseableHttpResponse response, final String digestName,
                                    final String digestValue) {
        final String digest = response.getFirstHeader("Digest").getValue();
        final Map<String, String> digestHeaders = decodeDigestHeader(digest);
        assertTrue(digestHeaders.containsKey(digestName));
        assertEquals("Mismatch on " + digestName + " value", digestValue, digestHeaders.get(digestName));
    }

}
