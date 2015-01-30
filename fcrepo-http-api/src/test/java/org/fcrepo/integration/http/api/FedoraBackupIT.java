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
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * <p>FedoraBackupIT class.</p>
 *
 * @author cbeer
 */
public class FedoraBackupIT extends AbstractResourceIT {

    @Test
    public void shouldRoundTripBackups() throws Exception {
        final String objName = randomUUID().toString();

        // set up the object
        final StringBuilder text = new StringBuilder();
        for (int x = 0; x < 1000; ++x) {
            text.append("data-" + x);
        }

        HttpResponse response;
        // Create object
        createObject(objName);

        // Create datastream
        createDatastream(objName, "testDS", text.toString());

        // Verify object exists
        response = execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

        // create a named version of it with spaces
        final HttpPost httpPost = new HttpPost(serverAddress + objName + "/testDS/fcr:versions");
        httpPost.setHeader("Slug", "version name with spaces");
        assertEquals(204, getStatus(httpPost));

        // back it up
        final File dir = createTempDir();
        logger.debug("Backing up repository to {}", dir.getCanonicalPath());
        final HttpPost backupMethod =
                new HttpPost(serverAddress + "fcr:backup");
        backupMethod.setEntity(new StringEntity(dir.getCanonicalPath()));
        response = execute(backupMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());
        assertEquals(dir.getCanonicalPath(), content);
        logger.debug("Back up directory was {}", content);

        // delete it
        response = execute(new HttpDelete(serverAddress + objName));
        assertEquals(204, response.getStatusLine().getStatusCode());

        // Verify object removed
        assertDeleted(serverAddress + objName);

        // try to restore it
        final HttpPost restoreMethod =
                new HttpPost(serverAddress + "fcr:restore");
        restoreMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", 204, getStatus(restoreMethod));

        //check that we made it
        response = execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

}

