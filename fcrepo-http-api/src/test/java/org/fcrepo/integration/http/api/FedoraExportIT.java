/**
 * Copyright 2014 DuraSpace, Inc.
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * <p>FedoraExportIT class.</p>
 *
 * @author awoods
 */
public class FedoraExportIT extends AbstractResourceIT {

    @Test
    public void shouldRoundTripOneObject() throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        client.execute(postObjMethod(objName));
        client.execute(postDSMethod(objName, "testDS", "stuff"));

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        HttpResponse response = client.execute(getObjMethod);
        assertEquals("application/xml", response.getEntity().getContentType()
                .getValue());
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);

        // delete it
        client.execute(new HttpDelete(serverAddress + objName));
        response =
            client.execute(new HttpGet(serverAddress + objName));
        assertEquals(404, response.getStatusLine().getStatusCode());

        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", 201, getStatus(importMethod));

        // check that we made it
        response =
            client.execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public
            void
            shouldMoveObjectToTheRootLevelUsingTheRepositoryWideApi()
                                                                     throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        client.execute(postObjMethod(objName));
        client.execute(postDSMethod(objName, "testDS", "stuff"));

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);

        // delete it
        client.execute(new HttpDelete(serverAddress + objName));
        response =
            client.execute(new HttpGet(serverAddress + objName));
        assertEquals(404, response.getStatusLine().getStatusCode());

        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", 201, getStatus(importMethod));

        // check that we made it
        response =
            client.execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public void shouldFailToImportOverExistingNode() throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        client.execute(postObjMethod(objName));
        client.execute(postDSMethod(objName, "testDS", "stuff"));

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + objName + "/fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals( 409, getStatus(importMethod));
    }

    @Test
    public void shouldExportUsingTheRepositoryWideApi() throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        client.execute(postObjMethod(objName));
        client.execute(postDSMethod(objName, "testDS", "stuff"));

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);

    }

}
