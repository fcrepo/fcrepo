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


import edu.wisc.library.ocfl.api.OcflRepository;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.kernel.api.FedoraTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;

import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * @author awooods
 * @since 2020-03-04
 */
@TestExecutionListeners(listeners = { RebuildTestExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class RebuildIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildIT.class);

    private OcflRepository ocflRepository;

    @Before
    public void setUp() {
        ocflRepository = getBean(OcflRepository.class);
    }

    /**
     * This test rebuilds from a known set of OCFL content.
     * The OCFL storage root contains the following four resources:
     * - root
     * - /binary
     * - /test
     * - /test/child
     * and a deleted object
     * - /test/deleted-child
     *
     * The test verifies that these objects exist in the rebuilt repository.
     */
    @Test
    public void testRebuildOcfl() {

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        assertEquals(8, ocflRepository.listObjectIds().count());
        assertTrue("Should contain object with id: " + FedoraTypes.FEDORA_ID_PREFIX,
                ocflRepository.containsObject(FedoraTypes.FEDORA_ID_PREFIX));
        assertContains("binary");
        assertContains("test");
        assertContains("test_child");
        assertContains("test/deleted-child");
        assertContains("archival-group");
        assertContains("test_nested-archival-group");
        assertContains("test_nested-binary");

        assertNotContains("archival-group_binary");
        assertNotContains("archival-group_container");
        assertNotContains("junk");
    }

    @Test
    public void testRebuildWebapp() throws Exception {
        // Test against the Fedora API
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("test")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("binary")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("binary/" + FCR_METADATA)));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("archival-group")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("archival-group/binary")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("archival-group/container")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("test/child")));
        assertEquals(GONE.getStatusCode(), getStatus(getObjMethod("test/deleted-child")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("test/nested-archival-group")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("test/nested-binary")));

        final String testUri = serverAddress + "test";
        //verify containment relationships
        verifyContainment(serverAddress, asList("binary", "archival-group", "test"));
        verifyContainment(serverAddress + "archival-group", asList("binary", "container"));
        verifyContainment(testUri, asList("child", "nested-archival-group", "nested-binary"),
                Collections.singletonList("deleted-child"));

        // Get last version of test to see when deleted-child was not deleted
        final String mementoUri;
        try (final CloseableDataset dataset = getDataset(getObjMethod("test/" + FCR_VERSIONS))) {
            final var graph = dataset.asDatasetGraph();
            final var iter = graph.find(ANY, createURI(serverAddress + "test/" + FCR_VERSIONS), CONTAINS.asNode(), ANY);
            final Iterable<Quad> iterable = () -> iter;
            final List<String> versionList = StreamSupport.stream(iterable.spliterator(), false)
                    .map(Quad::getObject).map(Node::getURI).sorted().collect(Collectors.toList());
            mementoUri = versionList.get(versionList.size() - 1);
        }

        verifyContainment(mementoUri, testUri, asList("child", "nested-archival-group",
                "nested-binary", "deleted-child"));
    }

    private void verifyContainment(final String subjectUri, final List<String> children) throws Exception {
        verifyContainment(subjectUri, subjectUri, children);
    }

    private void verifyContainment(final String requestUri, final String subjectUri,
                                   final List<String> includeChildren) throws Exception {
        verifyContainment(requestUri, subjectUri, includeChildren, Collections.emptyList());
    }

    private void  verifyContainment(final String subjectUri, final List<String> includeChildren,
                                    final List<String> excludeChildren) throws Exception {
        verifyContainment(subjectUri, subjectUri, includeChildren, excludeChildren);
    }

    /**
     * Utility to verify containment triples.
     *
     * @param requestUri the URI to request the graph from (could be different as in mementos or binary descriptions)
     * @param subjectUri the URI to be the subject of the triples.
     * @param includeChildren list of children expected, only the final path part which is appended to subjectURI.
     * @param excludeChildren list of children not expected, only the final path part which is appended to subjectURI
     * @throws Exception
     */
    private void verifyContainment(final String requestUri, final String subjectUri, final List<String> includeChildren,
                                   final List<String> excludeChildren) throws Exception {
        final var subjectNode = createURI(subjectUri);
        try (final CloseableDataset dataset = getDataset(new HttpGet(requestUri))) {
            final var graph = dataset.asDatasetGraph();
            if (LOGGER.isDebugEnabled()) {
                graph.listGraphNodes().forEachRemaining(gn -> {
                    LOGGER.debug("Node = " + gn.toString());
                });
            }

            for (final String child : includeChildren) {
                final var childNode = createURI(subjectUri + (subjectUri.endsWith("/") ? "" : "/") + child);
                Assert.assertTrue(format("Triple not found: {0}, {1}, {2}", subjectUri,
                 CONTAINS, childNode),
                        graph.contains(ANY,
                        subjectNode,
                        CONTAINS.asNode(),
                        childNode));
            }
            for (final String child : excludeChildren) {
                final var childNode = createURI(subjectUri + (subjectUri.endsWith("/") ? "" : "/") + child);
                Assert.assertFalse(format("Triple found: {0}, {1}, {2}", subjectUri,
                        CONTAINS, childNode),
                        graph.contains(ANY,
                                subjectNode,
                                CONTAINS.asNode(),
                                childNode));
            }
        }
    }

    private void assertContains(final String id) {
        final var fedoraId = FedoraTypes.FEDORA_ID_PREFIX + "/" + id;
        assertTrue("Should contain object with id: " + fedoraId,
                ocflRepository.containsObject(fedoraId));
    }

    private void assertNotContains(final String id) {
        final var fedoraId = FedoraTypes.FEDORA_ID_PREFIX + "/" + id;
        assertFalse("Should NOT contain object with id: " + fedoraId,
                ocflRepository.containsObject(fedoraId));
    }

}
