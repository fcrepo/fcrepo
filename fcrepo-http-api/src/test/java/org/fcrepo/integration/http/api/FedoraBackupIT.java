/**
 * Copyright 2013 DuraSpace, Inc.
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

public class FedoraBackupIT extends AbstractResourceIT {

	@Test
	public void shouldRoundTripBackups() throws Exception {
        final String objName = randomUUID().toString();

		// set up the object
        final StringBuilder text = new StringBuilder();
        for (int x = 0; x < 1000; ++x) {
            text.append("data-" + x);
        }

        // Create object
        HttpResponse response = client.execute(postObjMethod(objName));
        assertEquals(201, response.getStatusLine().getStatusCode());

        // Create datastream
        response = client.execute(postDSMethod(objName, "testDS", text.toString()));
        assertEquals(201, response.getStatusLine().getStatusCode());

        // Verify object exists
        response = client.execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

		// back it up
        final File dir = createTempDir();
        logger.debug("Backing up repository to {}", dir.getCanonicalPath());
		final HttpPost backupMethod =
				new HttpPost(serverAddress + "fcr:backup");
        backupMethod.setEntity(new StringEntity(dir.getCanonicalPath()));
        response = client.execute(backupMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());
        assertEquals(dir.getCanonicalPath(), content);
        logger.debug("Back up directory was {}", content);

        // delete it
        response = client.execute(new HttpDelete(serverAddress + objName));
        assertEquals(204, response.getStatusLine().getStatusCode());

        // Verify object removed
		response = client.execute(new HttpGet(serverAddress + objName));
		assertEquals(404, response.getStatusLine().getStatusCode());

		// try to restore it
        final HttpPost restoreMethod =
                new HttpPost(serverAddress + "fcr:restore");
        restoreMethod.setEntity(new StringEntity(content));
		assertEquals("Couldn't import!", 204, getStatus(restoreMethod));

		//check that we made it
		response = client.execute(new HttpGet(serverAddress + objName));
		assertEquals(200, response.getStatusLine().getStatusCode());

    }

}

