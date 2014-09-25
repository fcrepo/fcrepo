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

import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.EMBED_CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.VERSIONING_POLICY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>FedoraVersionsIT class.</p>
 *
 * @author awoods
 */
public class FedoraVersionsIT extends AbstractResourceIT {

    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        enableVersioning(pid);
        final HttpGet getVersion =
            new HttpGet(serverAddress + pid + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject = createResource(serverAddress + pid);
        assertTrue("Didn't find a version triple!",
                results.contains(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY));
    }

    private void enableVersioning(final String pid) {
        final HttpPut createTx = new HttpPut(serverAddress + pid + "/fcr:versions");
        try {
            final HttpResponse response = execute(createTx);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetUnversionedObjectVersionProfile() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final HttpGet getVersion = new HttpGet(serverAddress + pid + "/fcr:versions");
        assertEquals(404, getStatus(getVersion));
    }

    @Test
    public void testAddAndRetrieveVersion() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        enableVersioning(pid);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + pid, DC_TITLE.getURI(), "First Title");

        final GraphStore nodeResults = getContent(serverAddress + pid);
        assertTrue("Should find original title", nodeResults.contains(ANY,
                                                                      ANY,
                                                                      DC_TITLE.asNode(),
                                                                      createLiteral("First Title")));

        logger.debug("Posting version v0.0.1");
        postObjectVersion(pid, "v0.0.1");

        logger.debug("Replacing the title");
        patchLiteralProperty(serverAddress + pid, DC_TITLE.getURI(), "Second Title");

        final GraphStore versionResults = getContent(serverAddress + pid + "/fcr:versions/v0.0.1");
        logger.debug("Got version profile:");

        assertTrue("Didn't find a version triple!",
                      versionResults.contains(ANY,
                                              ANY, HAS_PRIMARY_TYPE.asNode(),
                                              createLiteral("nt:frozenNode")));

        assertTrue("Should find a title in historic version",
                   versionResults.contains(ANY, ANY, DC_TITLE.asNode(), ANY));
        assertTrue("Should find original title in historic version",
                   versionResults.contains(ANY,
                                           ANY,
                                           DC_TITLE.asNode(),
                                           createLiteral("First Title")));
        assertFalse("Should not find the updated title in historic version",
                    versionResults.contains(ANY,
                                            ANY,
                                            DC_TITLE.asNode(),
                                            createLiteral("Second Title")));
    }

    @Test
    public void testVersioningANodeWithAVersionableChild() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        enableVersioning(pid);

        logger.debug("Adding a child");
        createDatastream(pid, "ds", "This DS will not be versioned");

        logger.debug("Posting version");
        postObjectVersion(pid);

        final HttpGet getVersion =
                new HttpGet(serverAddress + pid + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + pid);
        assertTrue("Didn't find a version triple!",
                results.contains(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY));

        final Iterator<Quad> versionIt = results.find(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY);

        while (versionIt.hasNext()) {
            final String url = versionIt.next().getObject().getURI();
            assertEquals("Version " + url + " isn't accessible!",
                    200, getStatus(new HttpGet(url)));
        }
    }

    @Test
    public void testCreateUnlabeledVersion() throws Exception {
        logger.debug("Creating an object");
        final String objId = getRandomUniquePid();
        createObject(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "Example Title");

        logger.debug("Posting an unlabeled version");
        postObjectVersion(objId);
    }

    @Test
    public void testCreateTwoVersionsWithSameLabel() throws Exception {
        logger.debug("creating an object");
        final String objId = getRandomUniquePid();
        createObject(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "First title");

        logger.debug("Posting a version with label \"label\"");
        postObjectVersion(objId, "label");

        logger.debug("Resetting the title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "Second title");

        logger.debug("Posting a version with label \"label\"");
        postObjectVersion(objId, "label");

        logger.debug("Resetting the title");
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "Third title");

        final GraphStore versionResults = getContent(serverAddress + objId + "/fcr:versions/label");

        logger.debug("Got version profile:");
        assertTrue("Should find a title.", versionResults.contains(ANY, ANY, DC_TITLE.asNode(), ANY));
        assertTrue("Should find the title from the last version tagged with the label \"label\"",
                versionResults.contains(ANY,
                                        ANY,
                                        DC_TITLE.asNode(),
                                        createLiteral("Second title")));
    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        enableVersioning(pid);
        createDatastream(pid, "ds1", "foo");
        enableVersioning(pid + "/ds1");

        final HttpGet getVersion =
            new HttpGet(serverAddress
                    + pid + "/ds1/fcr:versions/lastVersion");
        final HttpResponse resp = execute(getVersion);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }

    public void mutateDatastream(final String objName, final String dsName,
                                 final String contentText) throws IOException {
        final HttpPut mutateDataStreamMethod =
                putDSMethod(objName, dsName, contentText);
        final HttpResponse response = execute(mutateDataStreamMethod);
        final int status = response.getStatusLine().getStatusCode();
        if (status != NO_CONTENT.getStatusCode()) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals("Couldn't mutate a datastream!", NO_CONTENT.getStatusCode(), status);

    }

    @Test
    public void isAutoVersionedContentStillAccessible() throws Exception {
        final String objName = getRandomUniquePid();
        final String dsName = getRandomUniquePid();
        final String firstVersionText = "foo";
        final String secondVersionText = "bar";

        createObject(objName);
        createDatastream(objName, dsName, firstVersionText);
        enableVersioning(objName + "/" + dsName);
        setAutoVersioning(serverAddress + objName + "/" + dsName + "/" + FCR_METADATA);
        mutateDatastream(objName, dsName, secondVersionText);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName);
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY));

        verifyVersions(results, subject.asNode(), firstVersionText);
    }

    @Test
    public void testAddMixinAutoVersioning() throws IOException {
        postNodeTypeCNDSnippet("[fedora:autoVersioned] mixin\n" +
                "  - fedoraconfig:versioningPolicy (STRING) = \"auto-version\" autocreated");
        final String objName = getRandomUniquePid();

        createObject(objName);
        addMixin(objName, "http://fedora.info/definitions/v4/rest-api#autoVersioned");

        final GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy", initialVersion
                .contains(ANY,
                        createResource(serverAddress + objName).asNode(),
                        VERSIONING_POLICY.asNode(),
                        createLiteral("auto-version")));
    }

    @Test
    public void testRepositoryWideAutoVersioning() throws IOException {
        final String autoVersionedType = "[fedora:autoVersioned] mixin\n" +
                "  - fedoraconfig:versioningPolicy (STRING) = \"auto-version\" autocreated";
        final String autoVersionedResource = "[fedora:resource] > fedora:relations, mix:created, mix:lastModified, " +
                "mix:lockable, mix:versionable, fedora:autoVersioned, dc:describable mixin\n" +
                "- rdf:type (URI) multiple\n" +
                "- * (undefined) multiple\n" +
                "- * (undefined)";
        final String defaultResource = "[fedora:resource] > fedora:relations, mix:created, mix:lastModified, " +
                "mix:lockable, mix:referenceable, dc:describable mixin\n" +
                "- rdf:type (URI) multiple\n" +
                "- * (undefined) multiple\n" +
                "- * (undefined)";

        postNodeTypeCNDSnippet(autoVersionedType);
        postNodeTypeCNDSnippet(autoVersionedResource);

        final String objName = getRandomUniquePid();
        final String dsName = "datastream";

        createObject(objName);

        final GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy",
                initialVersion.contains(ANY,
                                        createResource(serverAddress + objName).asNode(),
                                        VERSIONING_POLICY.asNode(),
                                        createLiteral("auto-version")));

        testDatastreamContentUpdatesCreateNewVersions(objName, dsName);

        // undo auto-versioning
        postNodeTypeCNDSnippet(defaultResource);
    }

    @Test
    public void testInvalidVersionReversion() throws Exception {
        final String objId = getRandomUniquePid();
        createObject(objId);

        enableVersioning(objId);

        final HttpPatch patch = new HttpPatch(serverAddress + objId + "/fcr:versions/invalid-version-label");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testVersionReversion() throws Exception {
        final String objId = getRandomUniquePid();
        final Resource subject = createResource(serverAddress + objId);
        final String title1 = "foo";
        final String firstVersionLabel = "v1";
        final String title2 = "bar";
        final String secondVersionLabel = "v2";

        createObject(objId);
        enableVersioning(objId);
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), title1);
        postObjectVersion(objId, firstVersionLabel);

        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), title2);
        postObjectVersion(objId, secondVersionLabel);

        final GraphStore preRollback = getGraphStore(new HttpGet(serverAddress + objId));
        assertTrue("First title must be present!", preRollback.contains(ANY, subject.asNode(), DC_TITLE.asNode(),
                createLiteral(title1)));
        assertTrue("Second title must be present!", preRollback.contains(ANY, subject.asNode(), DC_TITLE.asNode(),
                createLiteral(title2)));

        revertToVersion(objId, firstVersionLabel);

        final GraphStore postRollback = getGraphStore(new HttpGet(serverAddress + objId));
        assertTrue("First title must be present!", postRollback.contains(ANY, subject.asNode(), DC_TITLE.asNode(),
                createLiteral(title1)));
        assertFalse("Second title must NOT be present!",
                    postRollback.contains(ANY,
                                          subject.asNode(),
                                          DC_TITLE.asNode(),
                                          createLiteral(title2)));

        /*
         * Make the sure the node is checked out and able to be updated.
         *
         * Because the JCR concept of checked-out is something we don't
         * intend to expose through Fedora in the future, the following
         * line is simply to test that writes can be completed after a
         * reversion.
         */
        patchLiteralProperty(serverAddress + objId, DC_TITLE.getURI(), "additional change");
    }

    @Test
    public void testRemoveVersion() throws Exception {
        // create an object and a named version
        final String objId = getRandomUniquePid();
        final String versionLabel1 = "versionLabelNumberOne";
        final String versionLabel2 = "versionLabelNumberTwo";

        createResource(serverAddress + objId);
        createObject(objId);
        enableVersioning(objId);
        postObjectVersion(objId, versionLabel1);
        postObjectVersion(objId, versionLabel2);

        // make sure the version exists
        final HttpGet get1 = new HttpGet(serverAddress + objId + "/fcr:versions/" + versionLabel1);
        assertEquals(OK.getStatusCode(), getStatus(get1));

        // remove the version we created
        final HttpDelete remove = new HttpDelete(serverAddress + objId + "/fcr:versions/" + versionLabel1);
        assertEquals(NO_CONTENT.getStatusCode(),getStatus(remove));

        // make sure the version is gone
        final HttpGet get2 = new HttpGet(serverAddress + objId + "/fcr:versions/" + versionLabel1);
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(get2));
    }

    @Test
    public void testRemoveInvalidVersion() throws Exception {
        // create an object
        final String objId = getRandomUniquePid();
        createObject(objId);
        enableVersioning(objId);

        // removing a non-existent version should 404
        final HttpDelete delete = new HttpDelete(serverAddress + objId + "/fcr:versions/invalid-version-label");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(delete));
    }

    @Test
    public void testRemoveCurrentVersion() throws Exception {
        // create an object
        final String versionLabel = "testVersionNumberUno";
        final String objId = getRandomUniquePid();
        createObject(objId);
        enableVersioning(objId);
        postObjectVersion(objId, versionLabel);

        // removing the current version should 400
        final HttpDelete delete = new HttpDelete(serverAddress + objId + "/fcr:versions/" + versionLabel);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(delete));
    }

    @Test
    public void testVersionOperationAddsVersionableMixin() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final GraphStore originalObjectProperties = getContent(serverAddress + pid);
        assertFalse("Node must not have versionable mixin.",
                originalObjectProperties.contains(ANY, createResource(serverAddress + pid).asNode(),
                createURI(RDF_TYPE), createURI(MIX_NAMESPACE + "versionable")));

        postObjectVersion(pid);

        final GraphStore updatedObjectProperties = getContent(serverAddress + pid);
        assertTrue("Node is expected to have versionable mixin.",
                updatedObjectProperties.contains(ANY, createResource(serverAddress + pid).asNode(),
                createURI(RDF_TYPE), createURI(MIX_NAMESPACE + "versionable")));
    }

    @Test
    public void testAutoVersionEventAddsVersionableMixin() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final GraphStore originalObjectProperties = getContent(serverAddress + pid);
        assertFalse("Node must not have versionable mixin.",
                originalObjectProperties.contains(ANY, createResource(serverAddress + pid).asNode(),
                createURI(RDF_TYPE), createURI(MIX_NAMESPACE + "versionable")));

        setAutoVersioning(serverAddress + pid);

        final GraphStore updatedObjectProperties = getContent(serverAddress + pid);
        assertTrue("Node is expected to have versionable mixin.",
                updatedObjectProperties.contains(ANY, createResource(serverAddress + pid).asNode(),
                createURI(RDF_TYPE), createURI(MIX_NAMESPACE + "versionable")));
    }

    @Test
    public void testIndexResponseContentTypes() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        enableVersioning(pid);

        for (final String type : RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method =
                    new HttpGet(serverAddress + pid + "/fcr:versions");

            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testGetVersionResponseContentTypes() throws Exception {
        final String pid = getRandomUniquePid();
        final String versionName = "v1";

        createObject(pid);
        enableVersioning(pid);
        postObjectVersion(pid, versionName);

        for (final String type : RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method =
                    new HttpGet(serverAddress + pid + "/fcr:versions/" + versionName);

            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testOmissionOfJCRCVersionRDF() throws IOException {
        final String pid = getRandomUniquePid();
        createObject(pid);
        enableVersioning(pid);
        final GraphStore rdf = getGraphStore(new HttpGet(serverAddress + pid));

        final Resource subject = createResource(serverAddress + pid);
        final String [] jcrVersioningTriples = new String[] {
                REPOSITORY_NAMESPACE + "baseVersion",
                REPOSITORY_NAMESPACE + "isCheckedOut",
                REPOSITORY_NAMESPACE + "predecessors",
                REPOSITORY_NAMESPACE + "versionHistory" };

        for (String prohibitedProperty : jcrVersioningTriples) {
            assertFalse(prohibitedProperty + " must not appear in RDF for version-enabled node!",
                    rdf.contains(
                    ANY,
                    subject.asNode(),
                    createResource(prohibitedProperty).asNode(),
                    ANY));
        }

    }

    private void testDatastreamContentUpdatesCreateNewVersions(final String objName, final String dsName)
            throws IOException {
        final String firstVersionText = "foo";
        final String secondVersionText = "bar";
        createDatastream(objName, dsName, firstVersionText);
        final GraphStore dsInitialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versoning policy",
                dsInitialVersion.contains(ANY,
                                          createResource(serverAddress + objName + "/" + dsName).asNode(),
                                          VERSIONING_POLICY.asNode(),
                                          createLiteral("auto-version")));

        mutateDatastream(objName, dsName, secondVersionText);
        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName);
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        logger.debug("Retrieved version profile:");

        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY));

        verifyVersions(results, subject.asNode(), firstVersionText, secondVersionText);
    }

    /**
     * Verifies that one version exists with each supplied value.  This method
     * makes assertions that each of the provided values is the content of a
     * version node and nothing else.  Order isn't important, and no assumption
     * is made about whether extra versions exist.
     */
    private void verifyVersions(final GraphStore graph, final Node subject, final String ... values)
            throws IOException {
        final ArrayList<String> remainingValues = newArrayList(values);
        final Iterator<Quad> versionIt = graph.find(ANY, subject, HAS_VERSION.asNode(), ANY);

        while (versionIt.hasNext() && !remainingValues.isEmpty()) {
            final String value =
                    EntityUtils.toString(execute(new HttpGet(versionIt.next().getObject().getURI()))
                            .getEntity());
            remainingValues.remove(value);
        }

        if (!remainingValues.isEmpty()) {
            fail(remainingValues.get(0) + " was not preserved in the version history!");
        }
    }

    public void postNodeTypeCNDSnippet(final String snippet) throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress + "/fcr:nodetypes");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(snippet.getBytes()));
        httpPost.setEntity(entity);
        final HttpResponse response = execute(httpPost);

        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    private static void setAutoVersioning(final String url) throws IOException {
        patchLiteralProperty(url,
                "http://fedora.info/definitions/v4/config#versioningPolicy",
                "auto-version");
    }

    private static void patchLiteralProperty(final String url, final String predicate, final String literal)
            throws IOException {
        final HttpPatch updateObjectGraphMethod =
                new HttpPatch(url);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");

        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <" + predicate + "> \"" + literal + "\" } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);

        final HttpResponse response = execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    private GraphStore getContent(final String url) throws IOException {
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
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions" + (label == null ? "" : "/" + label));
        final HttpResponse response = execute(postVersion);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode() );
        final String locationHeader = response.getFirstHeader("Location").getValue();
        assertNotNull( "No version location header found", locationHeader );
        if ( label != null ) {
            assertEquals( "Version location header doesn't match requested version label",
                    serverAddress + path + "/fcr:versions/" + label, locationHeader );
        }
    }

    private void revertToVersion(final String objId, final String versionLabel) throws IOException {
        final HttpPatch patch = new HttpPatch(serverAddress + objId + "/fcr:versions/" + versionLabel);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));
    }
}
