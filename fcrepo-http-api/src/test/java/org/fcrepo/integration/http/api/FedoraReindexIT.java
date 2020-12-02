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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * @author dbernstein
 * @since 12/01/20
 */
@TestExecutionListeners(
        listeners = {LinuxTestIsolationExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraReindexIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraReindexIT.class);

    @Test
    public void testSideLoading() throws Exception {
        final var fedoraId = "container1";
        //validate that the fedora resource is not found (404)
        assertNotFound(fedoraId);
        //move ocfl directory into place on disk
        final var src = Path.of("src/test/resources/reindex-test").toFile();
        final var dest = Path.of("target/fcrepo-home/data/ocfl-root").toFile();
        LOGGER.info("copying {} to {}", src.toString(), dest.toString());
        FileUtils.copyDirectory(src, dest);

        //invoke reindex command
        final var httpPost = postObjMethod(fedoraId + "/fcr:reindex");

        try (final var response = execute(httpPost)) {
            assertEquals("expected 204", HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
        }
        //validate the the fedora resource is found (200)
        assertNotDeleted("container1");

       //verify that attempting to index an already existing resource returns a CONFLICT.
        try (final var response = execute(httpPost)) {
            assertEquals("expected " + HttpStatus.SC_CONFLICT + " on retry after successful indexing",
                    HttpStatus.SC_CONFLICT,
                    response.getStatusLine().getStatusCode());
        }
    }
}
