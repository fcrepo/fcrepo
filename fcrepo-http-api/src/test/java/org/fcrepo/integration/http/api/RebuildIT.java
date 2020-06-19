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
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.persistence.ocfl.api.OCFLPersistenceConstants.DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * @author awooods
 * @since 2020-03-04
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class RebuildIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildIT.class);

    private OcflRepository ocflRepository;

    private IndexBuilder indexBuilder;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(OcflPropsConfig.FCREPO_OCFL_ROOT, "target/test-classes/test-rebuild-ocfl/ocfl-root");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(OcflPropsConfig.FCREPO_OCFL_ROOT);
    }

    @Before
    public void setUp() {
        ocflRepository = getBean(OcflRepository.class);
        indexBuilder = getBean(IndexBuilder.class);
        indexBuilder.rebuild();
    }

    /**
     * This test rebuilds from a known set of OCFL content.
     * The OCFL storage root contains the following four resources:
     * - root
     * - /binary
     * - /test
     * - /test/child
     *
     * The test verifies that these objects exist in the rebuilt repository.
     */
    @Test
    public void testRebuildOCFL() {

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        assertEquals(7, ocflRepository.listObjectIds().count());
        assertTrue("Should contain object with id: " + DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID,
                ocflRepository.containsObject(DEFAULT_REPOSITORY_ROOT_OCFL_OBJECT_ID));
        assertTrue("Should contain object with id: binary", ocflRepository.containsObject("binary"));
        assertTrue("Should contain object with id: test", ocflRepository.containsObject("test"));
        assertTrue("Should contain object with id: test_child", ocflRepository.containsObject("test_child"));
        assertTrue("Should contain object with id: archival-group", ocflRepository.containsObject("archival-group"));
        assertTrue("Should contain object with id: test_nested-archival-group", ocflRepository.containsObject(
                "test_nested-archival-group"));
        assertTrue("Should contain object with id: test_nested-binary", ocflRepository.containsObject(
                "test_nested-binary"));

        assertFalse("Should NOT contain object with id: archival-group_binary", ocflRepository.containsObject(
                "archival-group_container"));
        assertFalse("Should NOT contain object with id: archival-group_container", ocflRepository.containsObject(
                "archival-group_binary"));
        assertFalse("Should NOT contain object with id: junk", ocflRepository.containsObject("junk"));
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
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("test/nested-archival-group")));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("test/nested-binary")));

        //verify containment relationships
        verifyContainment("", asList("binary", "archival-group", "test"));
        verifyContainment("archival-group", asList("binary", "container"));
        verifyContainment("test", asList("child", "nested-archival-group", "nested-binary"));
    }

    private void verifyContainment(final String parent, final List<String> children) throws Exception {
        final var containsNode = createURI(LDP_NAMESPACE + "contains");
        final var subjectUri = serverAddress + parent;
        final var subjectNode = createURI(subjectUri);
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectUri))) {
            final var graph = dataset.asDatasetGraph();
            if (LOGGER.isDebugEnabled()) {
                graph.listGraphNodes().forEachRemaining(gn -> {
                    LOGGER.debug("Node = " + gn.toString());
                });
            }

            for (final String child : children) {
                final var childNode = createURI(subjectUri + (subjectUri.endsWith("/") ? "" : "/") + child);
                Assert.assertTrue(format("Triple not found: {0}, {1}, {2}", subjectUri,
                 containsNode, childNode),
                        graph.contains(ANY,
                        subjectNode,
                        containsNode,
                        childNode));
            }
        }
    }

}
