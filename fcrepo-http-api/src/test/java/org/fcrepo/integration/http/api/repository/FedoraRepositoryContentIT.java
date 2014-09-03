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
package org.fcrepo.integration.http.api.repository;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Test;

import com.sun.jersey.core.header.ContentDisposition;

/**
 * <p>FedoraContentIT class.</p>
 *
 * @author lsitu
 * @author awoods
 */
public class FedoraRepositoryContentIT extends AbstractResourceIT {

    @Test
    public void testGetDatastreamContent() throws Exception {
        final String pid = "";
        final String dsid = randomUUID().toString();

        createDatastream(pid, dsid, "marbles for everyone");

        final HttpGet method_test_get = new HttpGet(getServerPath(pid) + "/" + dsid + "/fcr:content");
        assertEquals(200, getStatus(method_test_get));

        final HttpResponse response = client.execute(method_test_get);

        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response.getEntity())));
        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df",
                response.getFirstHeader("ETag").getValue().replace("\"", ""));

        final ContentDisposition contentDisposition =
                new ContentDisposition(response.getFirstHeader("Content-Disposition").getValue());

        assertEquals("attachment", contentDisposition.getType());
        assertEquals(dsid, contentDisposition.getFileName());

        logger.debug("Content was correct.");
    }
}
