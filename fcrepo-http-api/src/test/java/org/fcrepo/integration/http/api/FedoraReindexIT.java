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

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * @author dbernstein
 * @since 12/01/20
 */
@TestExecutionListeners(
        listeners = {TestIsolationExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraReindexIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraReindexIT.class);

    private void prepareContentForSideLoading(final String testObjectPath) throws Exception {
        //move ocfl directory into place on disk
        final var src = Path.of(testObjectPath).toFile();
        final var dest = Path.of("target/fcrepo-home/data/ocfl-root").toFile();
        LOGGER.info("copying {} to {}", src.toString(), dest.toString());
        FileUtils.copyDirectory(src, dest);
    }

    @Test
    public void testReindexNewObjects() throws Exception {
        final var fedoraId = "container1";
        //validate that the fedora resource is not found (404)
        assertNotFound(fedoraId);

        prepareContentForSideLoading("src/test/resources/reindex-test");
        final HttpPost httpPost = doReindex(fedoraId, HttpStatus.SC_NO_CONTENT);

        //validate the the fedora resource is found (200)
        assertNotDeleted(fedoraId);

        //verify that attempting to index an already existing resource returns a CONFLICT.
        try (final var response = execute(httpPost)) {
            assertEquals("expected " + HttpStatus.SC_CONFLICT + " on retry after successful indexing",
                    HttpStatus.SC_CONFLICT,
                    response.getStatusLine().getStatusCode());
        }
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

        prepareContentForSideLoading("src/test/resources/reindex-test-ag");

        //validate that the fedora resource is not found (404)
        assertNotFound(fedoraId);
        assertNotFound(parentId);

        doReindex(fedoraId, HttpStatus.SC_BAD_REQUEST);

        //ensure the resource is still not found
        assertNotFound(fedoraId);
        assertNotFound(parentId);
    }

    private HttpPost doReindex(final String fedoraId, final int expectedStatus) throws IOException {
        //invoke reindex command
        final var httpPost = postObjMethod(getReindexEndpoint(fedoraId));

        try (final var response = execute(httpPost)) {
            assertEquals("expected " + expectedStatus, expectedStatus, response.getStatusLine().getStatusCode());
        }
        return httpPost;
    }

    private String getReindexEndpoint(final String fedoraId) {
        return fedoraId + "/fcr:reindex";
    }

    @Test
    public void testMethodNotAllowed() throws Exception {

        //test get
        try (final var response = execute(getObjMethod(getReindexEndpoint("fedoraId")))) {
            assertEquals("expected 405", HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
        }

        //test put
        try (final var response = execute(putObjMethod(getReindexEndpoint("fedoraId")))) {
            assertEquals("expected 405", HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
        }

        //test delete
        try (final var response = execute(deleteObjMethod(getReindexEndpoint("fedoraId")))) {
            assertEquals("expected 405", HttpStatus.SC_METHOD_NOT_ALLOWED, response.getStatusLine().getStatusCode());
        }
    }

}
