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

import static java.util.UUID.randomUUID;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import com.hp.hpl.jena.update.GraphStore;

public class FedoraWorkspacesIT extends AbstractResourceIT {

    @Test
    public void testGetWorkspaces() throws Exception {
        final HttpGet httpGet = new HttpGet(serverAddress + "fcr:workspaces");
        httpGet.setHeader("Accept", "text/html");
        final HttpResponse response = execute(httpGet);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final InputStream in = response.getEntity().getContent();
        final List<String> lines = IOUtils.readLines(in);
        boolean found = false;
        for (final String line : lines) {
            if (line.contains(serverAddress + "workspace:default")) {
                found = true;
                break;
            }
        }
        assertTrue(serverAddress + "workspace:default, not found", found);
    }

    @Test
    public void shouldDemonstratePathsAndWorkspaces() throws IOException,
        RepositoryException {

        final String workspace = randomUUID().toString();
        final String pid = randomUUID().toString();

        final HttpPost httpCreateWorkspace =
                new HttpPost(serverAddress + "fcr:workspaces/" + workspace);
        final HttpResponse createWorkspaceResponse =
                execute(httpCreateWorkspace);
        assertEquals(201, createWorkspaceResponse.getStatusLine()
                .getStatusCode());

        final HttpPost httpPost =
            new HttpPost(serverAddress + "workspace:" + workspace + "/" + pid);
        final HttpResponse response = execute(httpPost);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final HttpGet httpGet =
            new HttpGet(serverAddress + "workspace:" + workspace + "/" + pid);
        httpGet.setHeader("Accept", "application/n3");
        final HttpResponse profileResponse = execute(httpGet);
        assertEquals(200, profileResponse.getStatusLine().getStatusCode());
        final GraphStore graphStore =
            parseTriples(profileResponse.getEntity().getContent());
        logger.info(graphStore.toString());
    }
}
