/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;

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
            assertTrue(contains(html, "class=\"fcrepo_root\""), html);
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
