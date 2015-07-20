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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static com.google.common.collect.Iterators.size;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.api.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>
 * FedoraVersionsIT class.
 * </p>
 *
 * @author awoods
 * @author ajs6f
 */
public class FedoraVersionsIT extends AbstractResourceIT {

    private static final String[] jcrVersioningTriples = new String[] {
        REPOSITORY_NAMESPACE + "baseVersion",
        REPOSITORY_NAMESPACE + "isCheckedOut",
        REPOSITORY_NAMESPACE + "predecessors",
        REPOSITORY_NAMESPACE + "versionHistory" };

    @Test
    public void testGetObjectVersionProfile() throws IOException {
        final String id = getRandomUniqueId();
        final String label = "v0.0.1";

        createObjectAndClose(id);
        enableVersioning(id);
        postObjectVersion(id, label);
        logger.debug("Retrieved version profile:");
        try (final CloseableGraphStore results = getGraphStore(new HttpGet(serverAddress + id + "/fcr:versions"))) {
            assertEquals("Expected exactly 3 triples!", 3, countTriples(results));
            final Resource subject = createResource(serverAddress + id);
            final Iterator<Quad> hasVersionTriple = results.find(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY);
            assertTrue("Didn't find a version triple!", hasVersionTriple.hasNext());
            final Node versionURI = hasVersionTriple.next().getObject();
            assertFalse("Found extra version triple!", hasVersionTriple.hasNext());
            assertTrue("Version label wasn't presented!",
                    results.contains(ANY, versionURI, HAS_VERSION_LABEL.asNode(), createLiteral(label)));
            assertTrue("Version creation date wasn't present!",
                    results.contains(ANY, versionURI, CREATED_DATE.asNode(), ANY));
        }
    }

    private static int countTriples(final GraphStore g) {
        return size(g.find());
    }

    private static void enableVersioning(final String pid) {
        final HttpPut createTx = new HttpPut(serverAddress + pid + "/fcr:versions");
        try {
            execute(createTx);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetUnversionedObjectVersionProfile() {
        final String pid = getRandomUniqueId();

        createObject(pid);

        final HttpGet getVersion = new HttpGet(serverAddress + pid + "/fcr:versions");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getVersion));
    }

    @Test
    public void testAddAndRetrieveVersion() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + id, DC_TITLE.getURI(), "First Title");

        try (final CloseableGraphStore nodeResults = getContent(serverAddress + id)) {
            assertTrue("Should find original title", nodeResults.contains(ANY,
                    ANY, DC_TITLE.asNode(), createLiteral("First Title")));
        }
        logger.debug("Posting version v0.0.1");
        postObjectVersion(id, "v0.0.1");

        logger.debug("Replacing the title");
        patchLiteralProperty(serverAddress + id, DC_TITLE.getURI(), "Second Title");

        try (final CloseableGraphStore versionResults = getContent(serverAddress + id + "/fcr:versions/v0.0.1")) {
            logger.debug("Got version profile:");

            assertTrue("Didn't find a version triple!", versionResults.contains(ANY,
                    ANY, HAS_PRIMARY_TYPE.asNode(), createLiteral("nt:frozenNode")));
            assertTrue("Should find a title in historic version", versionResults.contains(ANY,
                    ANY, DC_TITLE.asNode(), ANY));
            assertTrue("Should find original title in historic version", versionResults.contains(ANY,
                    ANY, DC_TITLE.asNode(), createLiteral("First Title")));
            assertFalse("Should not find the updated title in historic version", versionResults.contains(ANY,
                    ANY, DC_TITLE.asNode(), createLiteral("Second Title")));
        }
    }

    @Test
    public void testVersioningANodeWithAVersionableChild() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);
        logger.debug("Adding a child");
        createDatastream(id, "ds", "This DS will not be versioned");
        logger.debug("Posting version");
        postObjectVersion(id, "label");

        logger.debug("Retrieved version profile:");
        try (final CloseableGraphStore results = getGraphStore(new HttpGet(serverAddress + id + "/fcr:versions"))) {
            final Node subject = createURI(serverAddress + id);
            assertTrue("Didn't find a version triple!", results.contains(ANY, subject, HAS_VERSION.asNode(), ANY));
            final Iterator<Quad> versionIt = results.find(ANY, subject, HAS_VERSION.asNode(), ANY);
            while (versionIt.hasNext()) {
                final String url = versionIt.next().getObject().getURI();
                assertEquals("Version " + url + " isn't accessible!", OK.getStatusCode(), getStatus(new HttpGet(url)));
            }
        }
    }

    @Test
    public void testCreateLabeledVersion() throws IOException {
        logger.debug("Creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "Example Title");
        logger.debug("Posting a labeled version");
        postObjectVersion(objId, "label");
    }

    @Test
    public void testCreateUnlabeledVersion() throws IOException {
        logger.debug("Creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);
        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "Example Title");

        logger.debug("Posting an unlabeled version");
        final HttpPost postVersion = postObjMethod(objId + "/fcr:versions");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(postVersion));
    }

    @Test
    public void testVersionLabelWithSpace() throws IOException {
        final String label = "label with space";

        logger.debug("creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        logger.debug("Posting a version with label \"" + label + "\"");
        postObjectVersion(objId, label);
    }

    @Test
    public void testVersionLabelWithInvalidCharacters() {
        final String label = "\"label with quotes";
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);
        final HttpPost postVersion = postObjMethod(objId + "/fcr:versions");
        postVersion.addHeader("Slug", label);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(postVersion));
    }

    @Test
    public void testCreateTwoVersionsWithSameLabel() throws IOException {
        final String label1 = "label";
        final String label2 = "different-label";
        logger.debug("creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "First title");
        logger.debug("Posting a version with label \"" + label1 + "\"");
        postObjectVersion(objId, label1);
        logger.debug("Resetting the title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "Second title");

        logger.debug("Posting a version with label \"" + label1 + "\"");
        final HttpPost postVersion = postObjMethod(objId + "/fcr:versions");
        postVersion.addHeader("Slug", "label");
        assertEquals("Must not be allowed to create a version with a duplicate label!",
                CONFLICT.getStatusCode(), getStatus(postVersion));

        final HttpGet getVersions = new HttpGet(serverAddress + objId + "/fcr:versions");
        logger.debug("Retrieved versions");
        try (final CloseableGraphStore results = getGraphStore(getVersions)) {
            assertEquals("Expected exactly 3 triples!", 3, countTriples(results));
            logger.debug("Posting a version with label \"" + label2 + "\"");
            postObjectVersion(objId, label2);

            logger.debug("Deleting first labeled version \"" + label1 + "\"");
            final HttpDelete remove = new HttpDelete(serverAddress + objId + "/fcr:versions/" + label1);
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(remove));

            /**
             * This is the behavior we support... allowing a label to be reused if it's been deleted.
             */
            logger.debug("Making a new version with label \"" + label1 + "\"");
            postObjectVersion(objId, label1);
        }
    }

    /**
     * This test just makes sure that while an object may not have two versions with the same label, two different
     * objects may have versions with the same label.
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    // TODO this test requires some kind of assertion or negative assertion to make its intent more clear
            public
            void testCreateTwoObjectsWithVersionsWithTheSameLabel() throws IOException {
        final String label = "label";
        logger.debug("creating an object");
        final String objId1 = getRandomUniqueId();
        createObjectAndClose(objId1);
        enableVersioning(objId1);

        logger.debug("Posting a version with label \"" + label + "\"");
        postObjectVersion(objId1, label);

        logger.debug("creating another object");
        final String objId2 = getRandomUniqueId();
        createObjectAndClose(objId2);
        enableVersioning(objId2);

        logger.debug("Posting a version with label \"" + label + "\"");
        postObjectVersion(objId2, label);
    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {
        final String pid = getRandomUniqueId();

        createObjectAndClose(pid);
        enableVersioning(pid);
        createDatastream(pid, "ds1", "foo");
        enableVersioning(pid + "/ds1");

        final HttpGet getVersion = new HttpGet(serverAddress + pid + "/ds1/fcr:versions/lastVersion");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getVersion));
    }

    public void mutateDatastream(final String objName, final String dsName, final String contentText)
            throws IOException {
        assertEquals("Couldn't mutate a datastream!",
                NO_CONTENT.getStatusCode(), getStatus(putDSMethod(objName, dsName, contentText)));
    }

    @Test
    public void testInvalidVersionReversion() {
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);
        assertEquals(NOT_FOUND.getStatusCode(),
                getStatus(new HttpPatch(serverAddress + objId + "/fcr:versions/invalid-version-label")));
    }

    @Test
    public void testVersionReversion() throws IOException {
        final String objId = getRandomUniqueId();
        final Node subject = createURI(serverAddress + objId);
        final String title1 = "foo";
        final String firstVersionLabel = "v1";
        final String title2 = "bar";
        final String secondVersionLabel = "v2";

        createObjectAndClose(objId);
        enableVersioning(objId);
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), title1);
        postObjectVersion(objId, firstVersionLabel);

        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), title2);
        postObjectVersion(objId, secondVersionLabel);

        try (final CloseableGraphStore preRollback = getGraphStore(new HttpGet(serverAddress + objId))) {
            assertTrue("First title must be present!", preRollback.contains(ANY,
                    subject, DC_TITLE.asNode(), createLiteral(title1)));
            assertTrue("Second title must be present!", preRollback.contains(ANY,
                    subject, DC_TITLE.asNode(), createLiteral(title2)));
        }
        revertToVersion(objId, firstVersionLabel);
        try (final CloseableGraphStore postRollback = getGraphStore(new HttpGet(serverAddress + objId))) {
            assertTrue("First title must be present!", postRollback.contains(ANY,
                    subject, DC_TITLE.asNode(), createLiteral(title1)));
            assertFalse("Second title must NOT be present!", postRollback.contains(ANY,
                    subject, DC_TITLE.asNode(), createLiteral(title2)));
        }
        /*
         * Make the sure the node is checked out and able to be updated. Because the JCR concept of checked-out is
         * something we don't intend to expose through Fedora in the future, the following line is simply to test that
         * writes can be completed after a reversion.
         */
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "additional change");
    }

    @Test
    public void testRemoveVersion() throws IOException {
        // create an object and a named version
        final String objId = getRandomUniqueId();
        final String versionLabel1 = "versionLabelNumberOne";
        final String versionLabel2 = "versionLabelNumberTwo";

        createResource(serverAddress + objId);
        createObjectAndClose(objId);
        enableVersioning(objId);
        postObjectVersion(objId, versionLabel1);
        postObjectVersion(objId, versionLabel2);

        // make sure the version exists
        assertEquals(OK.getStatusCode(),
                getStatus(new HttpGet(serverAddress + objId + "/fcr:versions/" + versionLabel1)));

        // remove the version we created
        assertEquals(NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + objId + "/fcr:versions/" + versionLabel1)));

        // make sure the version is gone
        assertEquals(NOT_FOUND.getStatusCode(),
                getStatus(new HttpGet(serverAddress + objId + "/fcr:versions/" + versionLabel1)));
    }

    @Test
    public void testRemoveInvalidVersion() {
        // create an object
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        // removing a non-existent version should 404
        assertEquals(NOT_FOUND.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + objId + "/fcr:versions/invalid-version-label")));
    }

    @Test
    public void testRemoveCurrentVersion() throws IOException {
        // create an object
        final String versionLabel = "testVersionNumberUno";
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);
        postObjectVersion(objId, versionLabel);

        // removing the current version should 400
        assertEquals(BAD_REQUEST.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + objId + "/fcr:versions/" + versionLabel)));
    }

    @Test
    public void testVersionOperationAddsVersionableMixin() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final Node subject = createURI(serverAddress + id);
        try (final CloseableGraphStore originalObjectProperties = getContent(serverAddress + id)) {
            assertFalse("Node must not have versionable mixin.", originalObjectProperties.contains(ANY,
                    subject, type.asNode(), createURI(MIX_NAMESPACE + "versionable")));
        }
        postObjectVersion(id, "label");
        try (final CloseableGraphStore updatedObjectProperties = getContent(serverAddress + id)) {
            assertTrue("Node is expected to have versionable mixin.", updatedObjectProperties.contains(ANY,
                    subject, type.asNode(), createURI(MIX_NAMESPACE + "versionable")));
        }
    }

    @Test
    public void testDatastreamAutoMixinAndRevert() throws IOException {
        final String pid = getRandomUniqueId();
        final String dsid = "ds1";
        createObjectAndClose(pid);

        final String originalContent = "This is the original content";
        final String versionLabel = "ver1";
        createDatastream(pid, dsid, originalContent);

        // datastream should not have fcr:versions endpoint
        assertEquals(NOT_FOUND.getStatusCode(),
                getStatus(new HttpGet(serverAddress + pid + "/" + dsid + "/fcr:versions")));

        // datastream should not be versionable
        try (final CloseableGraphStore originalObjectProperties =
                getContent(serverAddress + pid + "/" + dsid + "/fcr:metadata")) {
            assertFalse("Node must not have versionable mixin.",
                    originalObjectProperties.contains(ANY, createURI(serverAddress + pid + "/" + dsid),
                            type.asNode(), createURI(MIX_NAMESPACE + "versionable")));
        }
        // creating a version should succeed
        final HttpPost httpPost = new HttpPost(serverAddress + pid + "/" + dsid + "/fcr:versions");
        httpPost.setHeader("Slug", versionLabel);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPost));
        // datastream should then have versions endpoint
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + pid + "/" + dsid + "/fcr:versions")));
        // datastream should then be versionable
        try (final CloseableGraphStore updatedDSProperties =
                getContent(serverAddress + pid + "/" + dsid + "/fcr:metadata")) {
            assertTrue("Node must have versionable mixin.", updatedDSProperties.contains(ANY,
                    createURI(serverAddress + pid + "/" + dsid + "/fcr:metadata"), type.asNode(),
                    createURI(MIX_NAMESPACE + "versionable")));
        }
        // update the content
        final String updatedContent = "This is the updated content";
        executeAndClose(putDSMethod(pid, dsid, updatedContent));
        try (final CloseableHttpResponse dsResponse = execute(getDSMethod(pid, dsid))) {
            assertEquals(updatedContent, EntityUtils.toString(dsResponse.getEntity()));
        }
        // revert to the original content
        revertToVersion(pid + "/" + dsid, versionLabel);
        try (final CloseableHttpResponse dsResponse2 = execute(getDSMethod(pid, dsid))) {
            assertEquals(originalContent, EntityUtils.toString(dsResponse2.getEntity()));
        }
    }

    @Test
    public void testIndexResponseContentTypes() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);

        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + id + "/fcr:versions");
            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testGetVersionResponseContentTypes() throws IOException {
        final String id = getRandomUniqueId();
        final String versionName = "v1";

        createObjectAndClose(id);
        enableVersioning(id);
        postObjectVersion(id, versionName);

        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + id + "/fcr:versions/" + versionName);
            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testOmissionOfJCRCVersionRDF() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);
        try (final CloseableGraphStore rdf = getGraphStore(new HttpGet(serverAddress + id))) {
            final Node subject = createURI(serverAddress + id);
            for (final String prohibitedProperty : jcrVersioningTriples) {
                assertFalse(prohibitedProperty + " must not appear in RDF for version-enabled node!",
                        rdf.contains(ANY, subject, createURI(prohibitedProperty), ANY));
            }
        }
    }

    @Test
    public void testInabilityToExportJCRXML() throws IOException {
        final String versionLabel = "l1";
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);
        postObjectVersion(id, versionLabel);

        try (final CloseableGraphStore rdf =
                getGraphStore(new HttpGet(serverAddress + id + "/fcr:versions/" + versionLabel))) {
            assertFalse("Historic version must not have any serialization defined.",
                    rdf.contains(ANY, ANY, HAS_SERIALIZATION.asNode(), ANY));
        }
    }

    private static void patchLiteralProperty(final String url, final String predicate, final String literal)
            throws IOException {
        final HttpPatch updateObjectGraphMethod = new HttpPatch(url);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity(
                "INSERT DATA { <> <" + predicate + "> \"" + literal + "\" } "));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
    }

    private CloseableGraphStore getContent(final String url) throws IOException {
        final HttpGet getVersion = new HttpGet(url);
        getVersion.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINS.toString() + "\"");
        return getGraphStore(getVersion);
    }

    public void postObjectVersion(final String pid) throws IOException {
        postVersion(pid, null);
    }

    public void postObjectVersion(final String pid, final String versionLabel) throws IOException {
        postVersion(pid, versionLabel);
    }

    public void postDsVersion(final String pid, final String dsId) throws IOException {
        postVersion(pid + "/" + dsId, null);
    }

    public void postVersion(final String path, final String label) throws IOException {
        logger.debug("Posting version");
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions");
        postVersion.addHeader("Slug", label);
        try (final CloseableHttpResponse response = execute(postVersion)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            assertNotNull("No version location header found", getLocation(response));
        }
    }

    private static void revertToVersion(final String objId, final String versionLabel) {
        assertEquals(NO_CONTENT.getStatusCode(),
                getStatus(new HttpPatch(serverAddress + objId + "/fcr:versions/" + versionLabel)));
    }
}
