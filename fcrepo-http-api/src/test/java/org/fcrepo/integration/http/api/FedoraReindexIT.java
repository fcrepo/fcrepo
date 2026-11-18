/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.persistence.ocfl.impl.ReindexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

import io.ocfl.api.OcflOption;
import io.ocfl.api.OcflRepository;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionNum;

/**
 * @author dbernstein
 * @since 12/01/20
 */
@TestExecutionListeners(
        listeners = {TestIsolationExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraReindexIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraReindexIT.class);

    private static final String PCDM_HAS_MEMBER = "http://pcdm.org/models#hasMember";
    private static final String PROXY_FOR = "http://example.com/proxyFor";

    private OcflRepository ocflRepository;
    private ReindexService reindexService;

    @BeforeEach
    public void setUp() {
        ocflRepository = getBean(OcflRepository.class);
        reindexService = getBean(ReindexService.class);
        reindexService.reset();
    }

    private void prepareContentForSideLoading(final String objectId, final String name) {
        final var id = FedoraId.create(objectId).getFullId();
        final var dir = Paths.get("src/test/resources", name);
        var currentVersion = VersionNum.fromInt(1);
        var currentDir = dir.resolve(currentVersion.toString());

        ocflRepository.purgeObject(id);

        while (Files.exists(currentDir)) {
            ocflRepository.putObject(ObjectVersionId.head(id), currentDir,
                    null, OcflOption.OVERWRITE);
            currentVersion = currentVersion.nextVersionNum();
            currentDir = dir.resolve(currentVersion.toString());
        }
    }

    @Test
    public void testReindexNewObjects() throws Exception {
        final var fedoraId = "container1";
        //validate that the fedora resource is not found (404)
        assertNotFound(fedoraId);

        prepareContentForSideLoading(fedoraId, "reindex-test");
        doReindex(fedoraId, HttpStatus.SC_NO_CONTENT);

        //validate the fedora resource is found (200)
        assertNotDeleted(fedoraId);

        //verify that updating and reindexing an existing resource returns a 204.
        prepareContentForSideLoading(fedoraId, "reindex-test-update");
        doReindex(fedoraId, SC_NO_CONTENT);
    }

    @Test
    public void reindexFailsWhenObjectInvalid() throws Exception {
        final var fedoraId = "container1";

        assertNotFound(fedoraId);

        prepareContentForSideLoading(fedoraId, "reindex-test-invalid");
        doReindex(fedoraId, HttpStatus.SC_BAD_REQUEST);

        assertNotFound(fedoraId);
    }

    @Test
    public void testReindexNonExistentObject() throws Exception {
        final var fedoraId = "container1";
        //validate that the fedora resource is not found (404)
        assertNotFound(fedoraId);
        doReindex(fedoraId, HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testReindexChildWithinArchivalGroup() throws Exception {
        final var parentId = "archival-group";
        final var fedoraId = parentId + "/child1";

        prepareContentForSideLoading(fedoraId, "reindex-test-ag");

        //validate that the fedora resource is not found (404)
        assertNotFound(fedoraId);
        assertNotFound(parentId);

        doReindex(fedoraId, HttpStatus.SC_BAD_REQUEST);

        //ensure the resource is still not found
        assertNotFound(fedoraId);
        assertNotFound(parentId);
    }

    @Test
    public void reindexSideloadedDirectContainerMembership() throws Exception {
        final var membershipId = "direct-membership";
        final var directId = membershipId + "/direct";
        final var member1Id = directId + "/member1";
        final var member2Id = directId + "/member2";

        createBasicContainer(membershipId);
        createDirectContainer(directId, membershipId);
        createBasicContainer(member1Id);
        createBasicContainer(member2Id);

        // Simulate sideloading by removing all indexes while retaining the OCFL object.
        reindexService.reset();
        doReindex(membershipId, SC_NO_CONTENT);

        assertHasMembers(membershipId, member1Id, member2Id);
    }

    @Test
    public void reindexSideloadedIndirectContainerMembership() throws Exception {
        final var membershipId = "indirect-membership";
        final var indirectId = membershipId + "/indirect";
        final var proxy1Id = indirectId + "/proxy1";
        final var proxy2Id = indirectId + "/proxy2";
        final var member1Id = indirectId + "/member1";
        final var member2Id = indirectId + "/member2";

        createBasicContainer(membershipId);
        createIndirectContainer(indirectId, membershipId);
        createBasicContainer(member1Id);
        createBasicContainer(member2Id);
        createProxy(proxy1Id, member1Id);
        createProxy(proxy2Id, member2Id);

        // Simulate sideloading by removing all indexes while retaining the OCFL object.
        reindexService.reset();
        doReindex(membershipId, SC_NO_CONTENT);

        assertHasMembers(membershipId, member1Id, member2Id);
    }

    private void createBasicContainer(final String id) throws IOException {
        assertEquals(SC_CREATED, getStatus(putObjMethod(id)));
    }

    private void createDirectContainer(final String id, final String membershipId) throws IOException {
        final var put = putObjMethod(id);
        put.setHeader("Link", "<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\"");
        put.setHeader("Content-Type", "text/turtle");
        put.setEntity(new StringEntity("PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "<> ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> ; " +
                "ldp:membershipResource <" + serverAddress + membershipId + "> ."));
        assertEquals(SC_CREATED, getStatus(put));
    }

    private void createIndirectContainer(final String id, final String membershipId) throws IOException {
        final var put = putObjMethod(id);
        put.setHeader("Link", "<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\"");
        put.setHeader("Content-Type", "text/turtle");
        put.setEntity(new StringEntity("PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "<> ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> ; " +
                "ldp:insertedContentRelation <" + PROXY_FOR + "> ; " +
                "ldp:membershipResource <" + serverAddress + membershipId + "> ."));
        assertEquals(SC_CREATED, getStatus(put));
    }

    private void createProxy(final String id, final String memberId) throws IOException {
        final var put = putObjMethod(id);
        put.setHeader("Content-Type", "text/turtle");
        put.setEntity(new StringEntity("<> <" + PROXY_FOR + "> <" + serverAddress + memberId + "> ."));
        assertEquals(SC_CREATED, getStatus(put));
    }

    private void assertHasMembers(final String membershipId, final String... memberIds) throws Exception {
        final Model model = getModel(membershipId);
        final var membershipResource = createResource(serverAddress + membershipId);
        final var hasMember = createProperty(PCDM_HAS_MEMBER);

        for (final var memberId : memberIds) {
            assertTrue(model.contains(membershipResource, hasMember, createResource(serverAddress + memberId)),
                    "Expected membership resource to contain " + memberId);
        }
    }

    private void doReindex(final String fedoraId, final int expectedStatus) throws IOException {
        //invoke reindex command
        final var httpPost = postObjMethod(getReindexEndpoint(fedoraId));

        assertEquals(expectedStatus, getStatus(httpPost));
    }

    private String getReindexEndpoint(final String fedoraId) {
        return fedoraId + "/fcr:reindex";
    }

    @Test
    public void testMethodNotAllowed() throws Exception {

        //test get
        try (final var response = execute(getObjMethod(getReindexEndpoint("fedoraId")))) {
            assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode(), "expected 405");
        }

        //test put
        try (final var response = execute(putObjMethod(getReindexEndpoint("fedoraId")))) {
            assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode(), "expected 405");
        }

        //test delete
        try (final var response = execute(deleteObjMethod(getReindexEndpoint("fedoraId")))) {
            assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode(), "expected 405");
        }
    }

}
