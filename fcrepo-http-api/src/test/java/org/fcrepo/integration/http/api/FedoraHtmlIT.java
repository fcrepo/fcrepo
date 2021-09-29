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

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * <p>FedoraHtmlIT class.</p>
 *
 * @author awoods
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraHtmlIT extends AbstractResourceIT {

    @Test
    public void testGetRoot() {

        final HttpGet method = new HttpGet(serverAddress);
        method.addHeader(ACCEPT, "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetNode() {

        final String pid = getRandomUniqueId();
        createObject(pid);

        final HttpGet method = new HttpGet(serverAddress + pid);
        method.addHeader(ACCEPT, "text/html");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetDatastreamNode() throws IOException {

        final String pid = getRandomUniqueId();
        createObject(pid);

        createDatastream(pid, "ds1", "foo");

        final HttpGet method =
            new HttpGet(serverAddress + pid + "/ds1");

        method.addHeader(ACCEPT, "text/plain");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testGetContainerTemplate() throws IOException {
        final String pid = getRandomUniqueId();
        createObject(pid);

        final HttpGet method = new HttpGet(serverAddress + pid);
        method.addHeader(ACCEPT, "text/html");
        try (final CloseableHttpResponse response = execute(method)) {
            final String html = EntityUtils.toString(response.getEntity());
            assertTrue(contains(html, "class=\"fcrepo_resource\""));
        }
    }

    @Test
    public void testGetBinaryTemplate() throws IOException {
        final String pid = getRandomUniqueId();
        createObject(pid);
        createDatastream(pid, "file", "binary content");

        final HttpGet method = new HttpGet(serverAddress + pid + "/file/fcr:metadata");
        method.addHeader(ACCEPT, "text/html");
        try (final CloseableHttpResponse response = execute(method)) {
            final String html = EntityUtils.toString(response.getEntity());
            assertTrue(contains(html, "class=\"fcrepo_binary\""));
        }
    }

    @Test
    public void testGetRootTemplate() throws IOException {

        final HttpGet method = new HttpGet(serverAddress);
        method.addHeader(ACCEPT, "text/html");
        try (final CloseableHttpResponse response = execute(method)) {
            final String html = EntityUtils.toString(response.getEntity());
            assertTrue(html, contains(html, "class=\"fcrepo_root\""));
        }
    }

    @Test
    public void testGetBlankNodesInHtml() throws IOException {
        final String blankNodeTtl = "@prefix test:  <info:fedora/test/> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix fedora: <http://fedora.info/definitions/v4/repository#> .\n" +
                "@prefix ldp:   <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix dc:    <http://purl.org/dc/elements/1.1/> .\n" +
                "\n" +
                "<> rdfs:label  \"dsa\" ;\n" +
                "   test:test     _:b0 .\n" +
                "\n" +
                "_:b0 rdfs:label  \"r\" ;\n" +
                "   dc:title    \"tq\" .";
        final String pid = getRandomUniqueId();
        final HttpPut putMethod = putObjMethod(pid, "text/turtle", blankNodeTtl);
        assertEquals(CREATED.getStatusCode(), getStatus(putMethod));
        // Can get as text/turtle
        final HttpGet getTtl = getObjMethod(pid);
        getTtl.addHeader(ACCEPT, "text/turtle");
        assertEquals(OK.getStatusCode(), getStatus(getTtl));
        // Can get with text/html
        final HttpGet getHtml = getObjMethod(pid);
        getHtml.addHeader(ACCEPT, "text/html");
        assertEquals(OK.getStatusCode(), getStatus(getHtml));
    }

}
