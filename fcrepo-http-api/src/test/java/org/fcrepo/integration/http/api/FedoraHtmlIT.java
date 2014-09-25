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

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

/**
 * <p>FedoraHtmlIT class.</p>
 *
 * @author awoods
 */
public class FedoraHtmlIT extends AbstractResourceIT {

    @Test
    public void testGetRoot() throws Exception {

        final HttpGet method = new HttpGet(serverAddress);
        method.addHeader("Accept", "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetNode() throws Exception {

        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpGet method = new HttpGet(serverAddress + pid);
        method.addHeader("Accept", "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetDatastreamNode() throws Exception {

        final String pid = getRandomUniquePid();
        createObject(pid);

        createDatastream(pid, "ds1", "foo");

        final HttpGet method =
            new HttpGet(serverAddress + pid + "/ds1");

        method.addHeader("Accept", "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetNamespaces() throws Exception {

        final HttpGet method = new HttpGet(serverAddress + "fcr:namespaces");
        method.addHeader("Accept", "text/html");
        assertEquals(200, getStatus(method));
    }
}
