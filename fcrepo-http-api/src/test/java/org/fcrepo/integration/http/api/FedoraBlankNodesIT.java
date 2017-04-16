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

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.tika.io.IOUtils;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;

/**
 * @author peichman
 */
public class FedoraBlankNodesIT extends AbstractResourceIT {
    private static final Logger LOGGER = getLogger(FedoraBlankNodesIT.class);
    private static StringEntity turtleEntity;
    private final int maxObjects = 5000;

    @Before
    public void setup() throws IOException {
        final InputStream turtleStream = this.getClass().getResourceAsStream("/test-blank-nodes/with_blank_nodes.ttl");
        final String turtleString = IOUtils.toString(turtleStream, "UTF-8");
        turtleEntity = new StringEntity(turtleString);
    }

    @Test
    public void testBlankNodes() {
        for (int i = 1; i <= maxObjects; i++) {
            try {
                createWithBlankNodes(turtleEntity);
            } catch (IOException e) {
                fail("Only loaded " + (i - 1) + " out of " + maxObjects + " objects");
            }
            if (i % 100 == 0) {
                LOGGER.info("Created " + Integer.toString(i));
            }
        }
    }

    private void createWithBlankNodes(final StringEntity entity) throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress);
        httpPost.addHeader("Content-Type", "text/turtle");
        httpPost.setEntity(entity);
        final HttpResponse response = client.execute(httpPost);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
    }
}
