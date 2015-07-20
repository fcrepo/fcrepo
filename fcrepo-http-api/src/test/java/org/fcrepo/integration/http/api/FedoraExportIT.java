/**
 * Copyright 2015 DuraSpace, Inc.
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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.modeshape.common.util.Base64;

/**
 * <p>FedoraExportIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
public class FedoraExportIT extends AbstractResourceIT {

    @Test
    public void shouldRoundTripOneContainer() throws IOException {
        final String objName = getRandomUniqueId();
        // set up the object
        createObjectAndClose(objName);
        createDatastream(objName, "testDS", "stuff");
        testRoundtrip(objName);
    }

    @Test
    public void shouldRoundTripOnePairtree() throws IOException {
        // set up the object
        final String objName = getLocation(postObjMethod());
        final String pairtreeName = objName.substring(serverAddress.length(), objName.lastIndexOf('/'));

        try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(serverAddress + pairtreeName))) {
            assertTrue("Resource \"" + objName + " " + pairtreeName + "\" must be pairtree.",
                    graphStore.contains(ANY, createURI(serverAddress + pairtreeName),
                            createURI(REPOSITORY_NAMESPACE + "mixinTypes"), createLiteral(FEDORA_PAIRTREE)));
        }
        testRoundtrip(pairtreeName);
    }

    private static void testRoundtrip(final String objName) throws IOException {
        // export it
        logger.debug("Attempting to export: " + objName);
        final String content;
        try (CloseableHttpResponse response = execute(new HttpGet(serverAddress + objName + "/fcr:export"))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertEquals("application/xml", response.getEntity().getContentType().getValue());
            logger.debug("Successfully exported: " + objName);
            content = EntityUtils.toString(response.getEntity());
            logger.debug("Found exported object: " + content);
        }
        // delete it
        executeAndClose(new HttpDelete(serverAddress + objName));
        assertDeleted(objName);
        assertEquals(NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + objName + "/fcr:tombstone")));

        // try to import it
        final String parentName = objName.contains("/") ? objName.substring(0, objName.lastIndexOf('/')) : "";
        final HttpPost importMethod = new HttpPost(serverAddress + parentName + "/fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", CREATED.getStatusCode(), getStatus(importMethod));

        // check that we made it
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + objName)));
    }

    @Test
    public void shouldMoveObjectToTheRootLevelUsingTheRepositoryWideApi() throws IOException {
        final String objName = getRandomUniqueId();

        // set up the object
        createObjectAndClose(objName);
        createDatastream(objName, "testDS", "stuff");

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod = new HttpGet(serverAddress + objName + "/fcr:export");
        final String content;
        try (CloseableHttpResponse response = execute(getObjMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            logger.debug("Successfully exported: " + objName);
            content = EntityUtils.toString(response.getEntity());
            logger.debug("Found exported object: " + content);
        }
        // delete it
        executeAndClose(new HttpDelete(serverAddress + objName));
        assertDeleted(objName);
        executeAndClose(new HttpDelete(serverAddress + objName + "/fcr:tombstone"));

        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", CREATED.getStatusCode(), getStatus(importMethod));

        // check that we made it
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + objName)));
    }

    @Test
    public void shouldFailToImportOverExistingNode() throws IOException {
        final String objName = getRandomUniqueId();

        // set up the object
        createObjectAndClose(objName);
        createDatastream(objName, "testDS", "stuff");

        // export it
        logger.debug("Attempting to export: " + objName);
        final String content;
        try (final CloseableHttpResponse response =
                execute(new HttpGet(serverAddress + objName + "/fcr:export?skipBinary=false&recurse=true"))) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            content = EntityUtils.toString(response.getEntity());
        }
        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + objName + "/fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals(CONFLICT.getStatusCode(), getStatus(importMethod));
    }

    @Test
    public void shouldExportUsingTheRepositoryWideApi() throws IOException {
        final String objName = getRandomUniqueId();

        // set up the object
        createObjectAndClose(objName);
        createDatastream(objName, "testDS", "stuff");

        // export it
        logger.debug("Attempting to export: " + objName);
        try (final CloseableHttpResponse response =
                client.execute(new HttpGet(serverAddress + objName + "/fcr:export"))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            logger.debug("Successfully exported: " + objName);
            final String content = EntityUtils.toString(response.getEntity());
            logger.debug("Found exported object: " + content);
        }
    }

    @Test
    public void shouldExportObjectWithNoBinary() throws IOException {
        final String objName = getRandomUniqueId();
        final String binaryValue = "stuff";
        // set up the object
        createObjectAndClose(objName);
        createDatastream(objName, "testDS", binaryValue);

        // export it
        logger.debug("Attempting to export: " + objName);
        final char[] base64Value;
        try (CloseableHttpResponse response =
                execute(new HttpGet(serverAddress + objName + "/fcr:export?recurse=true"))) {
            assertEquals("application/xml", response.getEntity().getContentType().getValue());
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            logger.debug("Successfully exported: " + objName);
            final String content = EntityUtils.toString(response.getEntity());
            logger.debug("Found exported object: " + content);
            base64Value = Base64.encodeBytes(binaryValue.getBytes("UTF-8")).toCharArray();
            assertFalse(content.contains(String.valueOf(base64Value)));
        }
        // Contains the binary value otherwise
        try (CloseableHttpResponse response = execute(new HttpGet(
                serverAddress + objName + "/fcr:export?recurse=true&skipBinary=false"))) {
            assertEquals("application/xml", response.getEntity().getContentType().getValue());
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertTrue(EntityUtils.toString(response.getEntity()).contains(String.valueOf(base64Value)));
        }
    }

    @Test
    public void shouldExportObjectRecurse() throws IOException {
        final String objName = getRandomUniqueId();
        final String childName = "testDS";
        final String binaryValue = "stuff";
        // set up the object
        createObjectAndClose(objName);
        createDatastream(objName, childName, binaryValue);
        // export it
        logger.debug("Attempting to export: " + objName);
        try (CloseableHttpResponse response = execute(new HttpGet(serverAddress + objName + "/fcr:export"))) {
            assertEquals("application/xml", response.getEntity().getContentType().getValue());
            assertEquals(OK.getStatusCode(), getStatus(response));
            logger.debug("Successfully exported: " + objName);
            final String content = EntityUtils.toString(response.getEntity());
            logger.debug("Found exported object: " + content);
            assertFalse(content.contains("sv:name=\"" + childName + "\""));
        }
        // Contains the child node otherwise
        try (CloseableHttpResponse response =
                execute(new HttpGet(serverAddress + objName + "/fcr:export?recurse=true"))) {
            assertEquals("application/xml", response.getEntity().getContentType().getValue());
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertTrue(EntityUtils.toString(response.getEntity()).contains("sv:name=\"" + childName + "\""));
        }
    }

    @Test
    public void importNonJCRXMLShouldFail() throws IOException {
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity("<test><this></this></test>"));
        assertEquals("Should not have been able to import non JCR/XML.",
                BAD_REQUEST.getStatusCode(), getStatus(importMethod));
    }

    @Test
    public void importMalformedXMLShouldFail() throws IOException {
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity("this isn't xml at all."));
        assertEquals("Should not have been able to import malformed XML.",
                BAD_REQUEST.getStatusCode(), getStatus(importMethod));
    }

    @Test
    public void importNonsensicalJCRXMLShouldFail() throws IOException {
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(
                new StringEntity("<sv:value xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\">just a value?</sv:value>"));
        assertEquals("Should not have been able to import nonsensical JCR/XML..",
                BAD_REQUEST.getStatusCode(), getStatus(importMethod));
    }

    @Test
    public void testExportBinary() throws IOException {
        final String objName = getRandomUniqueId();
        createObjectAndClose(objName);
        createDatastream(objName, "testDS", "stuff");
        final String dsName = objName + "/testDS";

        assertEquals("Should not be able to export binary content.", BAD_REQUEST.getStatusCode(),
                getStatus(new HttpGet(serverAddress + dsName + "/fcr:export")));
    }
}
