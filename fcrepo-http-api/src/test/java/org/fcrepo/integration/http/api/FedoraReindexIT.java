/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.http.HttpStatus;
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

    private OcflRepository ocflRepository;

    @BeforeEach
    public void setUp() {
        ocflRepository = getBean(OcflRepository.class);
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
