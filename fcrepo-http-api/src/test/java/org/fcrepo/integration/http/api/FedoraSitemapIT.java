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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

public class FedoraSitemapIT extends AbstractResourceIT {

    @Test
    public void testGetSitemapIndex() throws Exception {

        getStatus(postObjMethod("fcr:new"));
        final HttpGet httpGet = new HttpGet(serverAddress + "sitemap");

        assertEquals(200, getStatus(httpGet));

        logger.trace(IOUtils.toString(execute(httpGet).getEntity().getContent()));

    }

    @Test
    public void testGetSitemap() throws Exception {

        getStatus(postObjMethod("test:1"));
        getStatus(postObjMethod("fcr:new"));

        final HttpGet httpGet = new HttpGet(serverAddress + "sitemap/1");

        assertEquals(200, getStatus(httpGet));

        final String s =
            IOUtils.toString(execute(httpGet).getEntity().getContent());

        logger.trace("Got sitemap response: {}", s);
        assertTrue(s.contains("/test:1</sitemap:loc>"));

    }

}
