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

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_PAIRTREE;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
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
 */
public class FedoraExportIT extends AbstractResourceIT {

    @Test
    public void shouldRoundTripOneContainer() throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        createObject(objName);
        createDatastream(objName, "testDS", "stuff");

        testRoundtrip(objName);
    }

    @Test
    public void shouldRoundTripOnePairtree() throws IOException {
        // set up the object
        final HttpResponse response = createObject("");
        final String objName = response.getFirstHeader("Location").getValue();
        final String pairtreeName = objName.substring(serverAddress.length(), objName.lastIndexOf('/'));

        final GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + pairtreeName));
        assertTrue("Resource \"" + objName + " " + pairtreeName + "\" must be pairtree.", graphStore.contains(ANY,
                createResource(serverAddress + pairtreeName).asNode(),
                createURI(REPOSITORY_NAMESPACE + "mixinTypes"),
                createLiteral(FEDORA_PAIRTREE)));

        testRoundtrip(pairtreeName);
    }

    private void testRoundtrip(final String objName) throws IOException {
        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + objName + "/fcr:export");
        HttpResponse response = execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("application/xml", response.getEntity().getContentType()
                .getValue());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);

        // delete it
        execute(new HttpDelete(serverAddress + objName));
        assertDeleted(serverAddress + objName);
        final HttpResponse tombstoneResponse = execute(new HttpDelete(serverAddress + objName + "/fcr:tombstone"));
        assertEquals(204, tombstoneResponse.getStatusLine().getStatusCode());

        // try to import it
        final String parentName = objName.contains("/")
                ? objName.substring(0, objName.lastIndexOf('/')) : "";
        final HttpPost importMethod = new HttpPost(serverAddress + parentName + "/fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", 201, getStatus(importMethod));

        // check that we made it
        response = execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public
            void
            shouldMoveObjectToTheRootLevelUsingTheRepositoryWideApi()
                                                                     throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        createObject(objName);
        createDatastream(objName, "testDS", "stuff");

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        HttpResponse response = execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);

        // delete it
        execute(new HttpDelete(serverAddress + objName));
        assertDeleted(serverAddress + objName);
        execute(new HttpDelete(serverAddress + objName + "/fcr:tombstone"));

        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals("Couldn't import!", 201, getStatus(importMethod));

        // check that we made it
        response = execute(new HttpGet(serverAddress + objName));
        assertEquals(200, response.getStatusLine().getStatusCode());

    }

    @Test
    public void shouldFailToImportOverExistingNode() throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        createObject(objName);
        createDatastream(objName, "testDS", "stuff");

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export?skipBinary=false&recurse=true");
        final HttpResponse response = execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        // try to import it
        final HttpPost importMethod = new HttpPost(serverAddress + objName + "/fcr:import");
        importMethod.setEntity(new StringEntity(content));
        assertEquals( 409, getStatus(importMethod));
    }

    @Test
    public void shouldExportUsingTheRepositoryWideApi() throws IOException {
        final String objName = getRandomUniquePid();

        // set up the object
        createObject(objName);
        createDatastream(objName, "testDS", "stuff");

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);

    }

    @Test
    public void shouldExportObjectWithNoBinary() throws IOException {
        final String objName = getRandomUniquePid();
        final String binaryValue = "stuff";
        // set up the object
        createObject(objName);
        createDatastream(objName, "testDS", binaryValue);

        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export?recurse=true");
        HttpResponse response = execute(getObjMethod);
        assertEquals("application/xml", response.getEntity().getContentType()
                .getValue());
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);
        final char[] base64Value = Base64.encodeBytes(binaryValue.getBytes("UTF-8")).toCharArray();
        assertFalse(content.indexOf(String.valueOf(base64Value)) >= 0);

        // Contains the binary value otherwise
        final HttpGet getObjWithBinaryMethod = new HttpGet(
                serverAddress + objName + "/fcr:export?recurse=true&skipBinary=false");
        response = execute(getObjWithBinaryMethod);
        assertEquals("application/xml", response.getEntity().getContentType()
                .getValue());
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(response.getEntity()).indexOf(String.valueOf(base64Value)) >= 0);
    }

    @Test
    public void shouldExportObjectRecurse() throws IOException {
        final String objName = getRandomUniquePid();
        final String childName = "testDS";
        final String binaryValue = "stuff";
        // set up the object
        createObject(objName);
        createDatastream(objName, childName, binaryValue);
        // export it
        logger.debug("Attempting to export: " + objName);
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + objName + "/fcr:export");
        HttpResponse response = execute(getObjMethod);
        assertEquals("application/xml", response.getEntity().getContentType()
                .getValue());
        assertEquals(200, response.getStatusLine().getStatusCode());
        logger.debug("Successfully exported: " + objName);
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Found exported object: " + content);
        assertFalse(content.indexOf("sv:name=\"" + childName + "\"") > 0);

        // Contains the child node otherwise
        final HttpGet getObjWithBinaryMethod = new HttpGet(serverAddress + objName + "/fcr:export?recurse=true");
        response = execute(getObjWithBinaryMethod);
        assertEquals("application/xml", response.getEntity().getContentType()
                .getValue());
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(response.getEntity()).indexOf("sv:name=\"" + childName + "\"") > 0);
    }

    @Test
    public void importNonJCRXMLShouldFail() throws IOException {
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity("<test><this></this></test>"));
        assertEquals("Should not have been able to import non JCR/XML.", 400, getStatus(importMethod));
    }

    @Test
    public void importMalformedXMLShouldFail() throws IOException {
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(new StringEntity("this isn't xml at all."));
        assertEquals("Should not have been able to import malformed XML.", 400, getStatus(importMethod));
    }

    @Test
    public void importNonsensicalJCRXMLShouldFail() throws IOException {
        final HttpPost importMethod = new HttpPost(serverAddress + "fcr:import");
        importMethod.setEntity(
                new StringEntity("<sv:value xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\">just a value?</sv:value>"));
        assertEquals("Should not have been able to import nonsensical JCR/XML..", 400, getStatus(importMethod));
    }

    @Test
    public void testExportBinary() throws IOException {
        final String objName = getRandomUniquePid();
        createObject(objName);
        createDatastream(objName, "testDS", "stuff");
        final String dsName = objName + "/testDS";

        final HttpGet exportMethod = new HttpGet(serverAddress + dsName + "/fcr:export");
        final HttpResponse response = execute(exportMethod);
        assertEquals("Should not be able to export binary content.", 400, response.getStatusLine().getStatusCode());

    }
}
