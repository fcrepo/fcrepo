/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.apache.commons.lang3.StringUtils.contains;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * <p>FedoraHtmlIT class.</p>
 *
 * @author awoods
 */
public class FedoraHtmlIT extends AbstractResourceIT {

    @Test
    public void testGetRoot() {

        final HttpGet method = new HttpGet(serverAddress);
        method.addHeader("Accept", "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetNode() {

        final String pid = getRandomUniqueId();
        createObject(pid);

        final HttpGet method = new HttpGet(serverAddress + pid);
        method.addHeader("Accept", "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetDatastreamNode() throws IOException {

        final String pid = getRandomUniqueId();
        createObject(pid);

        createDatastream(pid, "ds1", "foo");

        final HttpGet method =
            new HttpGet(serverAddress + pid + "/ds1");

        method.addHeader("Accept", "text/plain");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testConcurrentGetDatastreamNode() throws IOException, InterruptedException {
        final String pid = getRandomUniqueId();
        createObject(pid);

        final File content = File.createTempFile("content", ".txt");
        content.deleteOnExit();

        FileWriter fw = null;
        try {
            fw = new FileWriter(content);
            for (int i = 0; i < 1000000; i++) {
                fw.write("fooooooooooooooooooooooooooooooooooooooooooooooooooo");
            }
        } finally {
            if (fw != null) {
                fw.close();
            }
        }

        final String ds = "ds2";
        final Thread t1 = createSNSDatastreamThread(pid, ds, content);
        final Thread t2 = createSNSDatastreamThread(pid, ds, content);
        t1.start();
        Thread.sleep(100);
        t2.start();
        t1.join();
        t2.join();

        Thread.sleep(1000);
        for (int i = 0; i < 2; i++) {
            String dsName = ds;
            if (i > 0) {
                dsName = ds + URLEncoder.encode("[" + (i + 1) + "]", "UTF-8");
            }

            final HttpGet method = new HttpGet(serverAddress + pid + "/" + dsName);
            method.addHeader("Accept", "text/plain");
            final int status = getStatus(method);
            if (i == 0) {
                assertEquals(200, status);
            } else {
                assertEquals(404, status);
            }
        }

        // Verify that the binary content can be replaced successfully.
        final HttpPut putMethod = putDSFileMethod(pid, ds, content);
        assertEquals(204, getStatus(putMethod));
    }

    @Test
    public void testGetContainerTemplate() throws IOException {
        final String pid = getRandomUniqueId();
        createObject(pid);
        addMixin(pid, REPOSITORY_NAMESPACE + "Container");

        final HttpGet method = new HttpGet(serverAddress + pid);
        method.addHeader("Accept", "text/html");
        try (final CloseableHttpResponse response = execute(method)) {
            final String html = EntityUtils.toString(response.getEntity());
            assertTrue(contains(html, "class=\"fcrepo_resource\""));
        }
    }

    @Test
    public void testGetBinaryTemplate() throws IOException {
        final String pid = getRandomUniqueId();
        createDatastream(pid, "file", "binary content");

        final HttpGet method = new HttpGet(serverAddress + pid + "/file/fcr:metadata");
        method.addHeader("Accept", "text/html");
        try (final CloseableHttpResponse response = execute(method)) {
            final String html = EntityUtils.toString(response.getEntity());
            assertTrue(contains(html, "class=\"fcrepo_binary\""));
        }
    }

    @Test
    public void testGetRootTemplate() throws IOException {

        final HttpGet method = new HttpGet(serverAddress);
        method.addHeader("Accept", "text/html");
        try (final CloseableHttpResponse response = execute(method)) {
            final String html = EntityUtils.toString(response.getEntity());
            assertTrue(contains(html, "class=\"fcrepo_root\""));
        }
    }

}
