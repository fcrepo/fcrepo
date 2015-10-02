/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static com.google.common.io.Files.createTempDir;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * <p>
 * FedoraBackupIT class.
 * </p>
 *
 * @author cbeer
 * @author ajs6f
 */
public class FedoraBackupIT extends AbstractResourceIT {

    private final static String text =
            "Logic, like whiskey, loses its beneficial effect when taken in too large quantities.";

    @Test
    public void shouldRoundTripBackups() throws Exception {
        final String objName = randomUUID().toString();

        // Create object
        createObjectAndClose(objName);
        // Create datastream
        createDatastream(objName, "testDS", text);
        // Verify object exists
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + objName)));

        // create a named version of it with spaces
        final HttpPost httpPost = new HttpPost(serverAddress + objName + "/testDS/fcr:versions");
        httpPost.setHeader("Slug", "version name with spaces");
        assertEquals(CREATED.getStatusCode(), getStatus(httpPost));

        // back it up
        final File requestedDir = createTempDir();
        logger.debug("Backing up repository to {}", requestedDir.getCanonicalPath());
        final HttpPost backupRequest = new HttpPost(serverAddress + "fcr:backup");
        backupRequest.setEntity(new StringEntity(requestedDir.getCanonicalPath()));
        final String usedDir;
        try (CloseableHttpResponse backupResponse = execute(backupRequest)) {
            assertEquals(OK.getStatusCode(), getStatus(backupResponse));
            usedDir = EntityUtils.toString(backupResponse.getEntity());
            assertEquals(requestedDir.getCanonicalPath(), usedDir);
            logger.debug("Back up directory was {}", usedDir);
        }
        // delete it
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + objName)));
        // Verify object removed
        assertDeleted(objName);

        // try to restore it
        final HttpPost restoreMethod = new HttpPost(serverAddress + "fcr:restore");
        restoreMethod.setEntity(new StringEntity(usedDir));
        assertEquals("Couldn't import!", NO_CONTENT.getStatusCode(), getStatus(restoreMethod));
        // check that we made it
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + objName)));
    }
}
