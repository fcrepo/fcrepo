/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Property;
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
    private static final String ARCHIVAL_GROUP_LINK_HEADER = "<" + ARCHIVAL_GROUP.getURI() + ">; rel=\"type\"";
    private static final Property PCDM_HAS_MEMBER_PROP = createProperty(PCDM_HAS_MEMBER);

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

        putBasicContainer(membershipId);
        putDirectContainer(directId, serverAddress + membershipId, PCDM_HAS_MEMBER_PROP, false);
        putBasicContainer(member1Id);
        putBasicContainer(member2Id);

        // Simulate sideloading by removing all indexes while retaining the OCFL object.
        reindexService.reset();
        doReindex(membershipId, SC_NO_CONTENT);
        doReindex(member1Id, SC_NO_CONTENT);
        doReindex(member2Id, SC_NO_CONTENT);
        // Index the membership container after its members have populated the containment index.
        doReindex(directId, SC_NO_CONTENT);

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
    }

    @Test
    public void reindexSideloadedArchivalGroupDirectContainerMembership() throws Exception {
        final var membershipId = "archival-direct-membership";
        final var directId = membershipId + "/direct";
        final var member1Id = directId + "/member1";
        final var member2Id = directId + "/member2";

        createObjectAndClose(membershipId, BASIC_CONTAINER_LINK_HEADER, ARCHIVAL_GROUP_LINK_HEADER);
        putDirectContainer(directId, serverAddress + membershipId, PCDM_HAS_MEMBER_PROP, false);
        putBasicContainer(member1Id);
        putBasicContainer(member2Id);

        // All resources are stored in the archival group's OCFL object.
        reindexService.reset();
        doReindex(membershipId, SC_NO_CONTENT);

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
    }

    @Test
    public void reindexSideloadedIndirectContainerMembership() throws Exception {
        final var membershipId = "indirect-membership";
        final var indirectId = membershipId + "/indirect";
        final var proxy1Id = indirectId + "/proxy1";
        final var proxy2Id = indirectId + "/proxy2";
        final var member1Id = indirectId + "/member1";
        final var member2Id = indirectId + "/member2";

        putBasicContainer(membershipId);
        putIndirectContainer(indirectId, serverAddress + membershipId, PCDM_HAS_MEMBER_PROP, PROXY_FOR, false);
        putBasicContainer(member1Id);
        putBasicContainer(member2Id);
        putProxy(proxy1Id, serverAddress + member1Id, PROXY_FOR, null);
        putProxy(proxy2Id, serverAddress + member2Id, PROXY_FOR, null);

        // Simulate sideloading by removing all indexes while retaining the OCFL object.
        reindexService.reset();
        doReindex(membershipId, SC_NO_CONTENT);
        doReindex(member1Id, SC_NO_CONTENT);
        doReindex(member2Id, SC_NO_CONTENT);
        doReindex(proxy1Id, SC_NO_CONTENT);
        doReindex(proxy2Id, SC_NO_CONTENT);
        // Index the membership container after its proxies have populated the containment index.
        doReindex(indirectId, SC_NO_CONTENT);

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
    }

    @Test
    public void reindexSideloadedExternalIndirectContainerMembership() throws Exception {
        final var membershipId = "external-indirect-membership";
        final var indirectId = "external-indirect-container";
        final var proxy1Id = indirectId + "/proxy1";
        final var proxy2Id = indirectId + "/proxy2";
        final var member1Id = "external-indirect-member1";
        final var member2Id = "external-indirect-member2";

        putBasicContainer(membershipId);
        putIndirectContainer(indirectId, serverAddress + membershipId, PCDM_HAS_MEMBER_PROP, PROXY_FOR, false);
        putProxy(proxy1Id, serverAddress + member1Id, PROXY_FOR, null);
        putProxy(proxy2Id, serverAddress + member2Id, PROXY_FOR, null);
        putBasicContainer(member1Id);
        putBasicContainer(member2Id);

        reindexService.reset();
        doReindex(indirectId, SC_NO_CONTENT);
        doReindex(proxy1Id, SC_NO_CONTENT);
        doReindex(proxy2Id, SC_NO_CONTENT);
        doReindex(member1Id, SC_NO_CONTENT);
        doReindex(member2Id, SC_NO_CONTENT);
        // Reindexing the target uses its inbound membershipResource reference to repopulate the container.
        doReindex(membershipId, SC_NO_CONTENT);

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
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
