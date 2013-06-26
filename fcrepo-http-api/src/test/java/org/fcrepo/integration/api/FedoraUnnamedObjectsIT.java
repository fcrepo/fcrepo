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

package org.fcrepo.integration.api;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraUnnamedObjectsIT extends AbstractResourceIT {

    @Test
    public void testIngestWithNew() throws Exception {
        final HttpPost method = postObjMethod("fcr:new");
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        int status = response.getStatusLine().getStatusCode();
        if (201 != status) {
            logger.error(content);
        }
        assertEquals(201, status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("new object did not mint a PID", !content
                .endsWith("/fcr:new"));
    }

    @Test
    public void testRepositoryLevelIngestWithNew() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "fcr:new");

        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        int status = response.getStatusLine().getStatusCode();
        if (201 != status) {
            logger.error(content);
        }
        assertEquals(201, status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("new object did not mint a PID", !content
                .endsWith("/fcr:new"));
    }

}
