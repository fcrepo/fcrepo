/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ocfl.api.OcflOption;
import io.ocfl.api.OcflRepository;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;
import io.ocfl.api.model.VersionNum;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Quad;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.impl.TransactionManagerImpl;
import org.fcrepo.persistence.ocfl.RepositoryInitializer;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.impl.ReindexService;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author awooods
 * @since 2020-03-04
 */
@TestExecutionListeners(listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class RebuildIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildIT.class);

    private OcflRepository ocflRepository;
    private RepositoryInitializer initializer;
    private ReindexService reindexService;
    private ObjectMapper objectMapper;
    private FedoraPropsConfig fedoraPropsConfig;
    private FedoraToOcflObjectIndex index;
    private Transaction readOnlyTx;
    private TransactionManagerImpl txManager;

    private void setBeans() {
        ocflRepository = getBean(OcflRepository.class);
        initializer = getBean(RepositoryInitializer.class);
        reindexService = getBean(ReindexService.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        fedoraPropsConfig = getBean(FedoraPropsConfig.class);
        index = getBean("ocflIndexImpl", FedoraToOcflObjectIndex.class);
        readOnlyTx = ReadOnlyTransaction.INSTANCE;
        txManager = getBean(TransactionManagerImpl.class);
    }

    @BeforeEach
    public void setUp() {
        setBeans();
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
        rebuild("test-rebuild-ocfl/objects");

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        assertEquals(8, ocflRepository.listObjectIds().count());
        assertTrue(ocflRepository.containsObject(FedoraTypes.FEDORA_ID_PREFIX),
                "Should contain object with id: " + FedoraTypes.FEDORA_ID_PREFIX);
        assertContains("binary");
        assertContains("test");
        assertContains("test/child");
        assertContains("test/deleted-child");
        assertContains("archival-group");
        assertContains("test/nested-archival-group");
        assertContains("test/nested-binary");

        assertNotContains("archival-group_binary");
        assertNotContains("archival-group_container");
        assertNotContains("junk");
    }

    @Test
    public void testRebuildContinue() throws Exception {
        assertFalse(fedoraPropsConfig.isRebuildContinue(), "rebuild on start is disabled");
        rebuild("test-rebuild-ocfl/objects");

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        assertTrue(ocflRepository.containsObject(FedoraTypes.FEDORA_ID_PREFIX),
                "Should contain object with id: " + FedoraTypes.FEDORA_ID_PREFIX);
        assertContains("binary");
        assertContains("test");

        final var binaryId = FedoraId.create(FedoraTypes.FEDORA_ID_PREFIX + "/binary");
        this.ocflRepository.purgeObject(binaryId.getFullId());

        //verify that the index st
        this.index.getMapping(readOnlyTx, binaryId);

        //set rebuild on start flag before initializing
        restartContainer();
        setBeans();
        initializer.initialize();

        //ocfl knows it is now gone
        assertNotContains("binary");

        //but the index does not know is it gone because no rebuild occurred.
        this.index.getMapping(readOnlyTx, binaryId);

        // Clear the index but retain the empty database and try again
        reindexService.reset();
        //restart the container again, but initialize after setting the rebuild continue
        restartContainer();
        setBeans();
        fedoraPropsConfig.setRebuildContinue(true);
        //give the container a few moments to start up and get the database setup.
        TimeUnit.MILLISECONDS.sleep(2000);
        initializer.initialize();

        assertThrows(FedoraOcflMappingNotFoundException.class, () -> this.index.getMapping(readOnlyTx, binaryId));
    }

    @Test
    public void testRebuildWebapp() throws Exception {
        // Set how long tx will live for so that any txs created by the rebuild expire before we attempt to
        // get the resources they created.
        propsConfig.setSessionTimeout(Duration.ofSeconds(5));

        rebuild("test-rebuild-ocfl/objects");

        // Wait for txs to expire and ensure they're cleaned up
        TimeUnit.SECONDS.sleep(5);
        txManager.cleanupClosedTransactions();

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

    @Test
    public void rebuildFailsWhenObjectFailsValidation() {
        rebuild("test-rebuild-invalid");

        assertEquals(HttpStatus.SC_NOT_FOUND, getStatus(getObjMethod("test")));
        assertEquals(HttpStatus.SC_NOT_FOUND, getStatus(getObjMethod("binary")));
    }

    private final String PCDM_HAS_MEMBER = "http://pcdm.org/models#hasMember";
    private final Property PCDM_HAS_MEMBER_PROP = createProperty(PCDM_HAS_MEMBER);

    @Test
    public void testRebuildMembership() throws Exception {
        rebuild("test-rebuild-membership");

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        assertEquals(8, ocflRepository.listObjectIds().count());
        assertTrue(ocflRepository.containsObject(FedoraTypes.FEDORA_ID_PREFIX),
                "Should contain object with id: " + FedoraTypes.FEDORA_ID_PREFIX);
        assertContains("work_container");
        assertContains("work_container/indirect");
        assertContains("work_container/indirect/member_proxy");
        assertContains("work_member");
        assertContains("direct_test");
        assertContains("direct_test/direct");
        assertContains("direct_test/direct/direct_member");

        assertEquals(OK.getStatusCode(), getStatus(getObjMethod("work_container")));

        final String indirectContainerUri = serverAddress + "work_container";
        final String indirectMemberUri = serverAddress + "work_member";
        assertHasMembership(indirectContainerUri, PCDM_HAS_MEMBER_PROP, indirectMemberUri);

        final String directContainerUri = serverAddress + "direct_test";
        final String directMemberUri = serverAddress + "direct_test/direct/direct_member";
        assertHasMembership(directContainerUri, PCDM_HAS_MEMBER_PROP, directMemberUri);
    }

    // Test intended to replicate FCREPO-3950
    @Test
    public void testRebuildContainersWithWebacs() throws Exception {
        rebuild("test-rebuild-webac-containers");

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        assertEquals(8, ocflRepository.listObjectIds().count());
        assertTrue(ocflRepository.containsObject(FedoraTypes.FEDORA_ID_PREFIX),
                "Should contain object with id: " + FedoraTypes.FEDORA_ID_PREFIX);
        assertContains("webac_indirect_outer");
        assertContains("webac_indirect_outer/indirect");
        assertContains("webac_indirect_outer/indirect/basic_child");
        assertContains("webac_direct");
        assertContains("webac_direct/direct_child");
        assertContains("webac_basic");
        assertContains("webac_basic/basic_child");

        final String indirectContainerUri = serverAddress + "webac_indirect_outer/indirect";
        final String indirectMemberUri = serverAddress + "webac_indirect_outer/indirect/basic_child";
        final String indirectWebacPath = "webac_indirect_outer/indirect/fcr:acl";
        final String indirectWebacUri = serverAddress + indirectWebacPath;
        assertHasMembership(indirectContainerUri, LDP_MEMBER, indirectMemberUri);
        verifyContainment(indirectContainerUri, asList("basic_child"), asList("fcr:acl"));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(indirectWebacPath)));
        assertDoesNotHaveMembership(indirectContainerUri, LDP_MEMBER, indirectWebacUri);

        final String directContainerUri = serverAddress + "webac_direct";
        final String directMemberUri = serverAddress + "webac_direct/direct_child";
        final String directWebacPath = "webac_direct/fcr:acl";
        final String directWebacUri = serverAddress + directWebacPath;
        assertHasMembership(directContainerUri, LDP_MEMBER, directMemberUri);
        verifyContainment(directContainerUri, asList("direct_child"), asList("fcr:acl"));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(directWebacPath)));
        assertDoesNotHaveMembership(indirectContainerUri, LDP_MEMBER, directWebacUri);

        final String basicContainerUri = serverAddress + "webac_basic";
        final String basicWebacPath = "webac_basic/fcr:acl";
        verifyContainment(basicContainerUri, asList("basic_child"), asList("fcr:acl"));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(basicWebacPath)));
    }

    private void assertHasMembership(final String subjectUri, final Property property, final String memberUri)
            throws Exception {
        final var subjectNode = createURI(subjectUri);
        final var memberNode = createURI(memberUri);
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectUri))) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY, subjectNode, property.asNode(), memberNode),
                    "Membership triple not present");
        }
    }

    private void assertDoesNotHaveMembership(final String subjectUri, final Property property, final String memberUri)
            throws Exception {
        final var subjectNode = createURI(subjectUri);
        final var memberNode = createURI(memberUri);
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectUri))) {
            final var graph = dataset.asDatasetGraph();
            assertFalse(graph.contains(ANY, subjectNode, property.asNode(), memberNode),
                    "Membership triple should not be present");
        }
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
                assertTrue(graph.contains(ANY, subjectNode, CONTAINS.asNode(), childNode),
                        format("Triple not found: {0}, {1}, {2}", subjectUri, CONTAINS, childNode));
            }
            for (final String child : excludeChildren) {
                final var childNode = createURI(subjectUri + (subjectUri.endsWith("/") ? "" : "/") + child);
                assertFalse(graph.contains(ANY, subjectNode, CONTAINS.asNode(), childNode),
                        format("Triple found: {0}, {1}, {2}", subjectUri, CONTAINS, childNode));
            }
        }
    }

    private void assertContains(final String id) {
        final var fedoraId = FedoraTypes.FEDORA_ID_PREFIX + "/" + id;
        assertTrue(ocflRepository.containsObject(fedoraId),
                "Should contain object with id: " + fedoraId);
    }

    private void assertNotContains(final String id) {
        final var fedoraId = FedoraTypes.FEDORA_ID_PREFIX + "/" + id;
        assertFalse(ocflRepository.containsObject(fedoraId),
                "Should NOT contain object with id: " + fedoraId);
    }

    private void rebuild(final String name) {
        // Since we are truncating but not deleting the database, set continue flag so it will rebuild
        fedoraPropsConfig.setRebuildContinue(true);
        copyToOcfl(name);
        reindexService.reset();
        initializer.initialize();
    }

    private void copyToOcfl(final String name) {
        try {
            // this is necessary so that the cache is cleared
            ocflRepository.listObjectIds().forEach(ocflRepository::purgeObject);

            try (final var list = Files.list(Paths.get("src/test/resources", name))) {
                list.filter(Files::isDirectory).forEach(dir -> {
                    final var objectId = URLDecoder.decode(dir.getFileName().toString(), StandardCharsets.UTF_8);
                    var currentVersion = VersionNum.fromInt(1);
                    var currentDir = dir.resolve(currentVersion.toString());

                    while (Files.exists(currentDir)) {
                        try {
                            final var headers = objectMapper.readValue(
                                    currentDir.resolve(".fcrepo/fcr-root.json").toFile(), ResourceHeaders.class);
                            ocflRepository.putObject(ObjectVersionId.head(objectId), currentDir,
                                    new VersionInfo().setCreated(
                                            headers.getLastModifiedDate().atOffset(ZoneOffset.UTC)),
                                    OcflOption.OVERWRITE);
                            currentVersion = currentVersion.nextVersionNum();
                            currentDir = dir.resolve(currentVersion.toString());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
