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

import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.DC.title;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static com.google.common.collect.Iterators.size;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.methods.HttpHead;

import org.fcrepo.http.commons.test.util.CloseableDataset;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetGraph;

import javax.ws.rs.core.Link;

/**
 * <p>
 * FedoraVersionsIT class.
 * </p>
 *
 * @author awoods
 * @author ajs6f
 */
@Ignore("Until implemented with Memento")
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
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + id + "/fcr:versions"))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertEquals("Expected exactly 3 triples!", 3, countTriples(graph));
            final Resource subject = createResource(serverAddress + id);
//            final Iterator<Quad> hasVersionTriple = graph.find(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY);
//            assertTrue("Didn't find a version triple!", hasVersionTriple.hasNext());
//            final Node versionURI = hasVersionTriple.next().getObject();
//            assertFalse("Found extra version triple!", hasVersionTriple.hasNext());
//            assertTrue("Version label wasn't presented!",
//                    graph.contains(ANY, versionURI, HAS_VERSION_LABEL.asNode(), createLiteral(label)));
//            assertTrue("Version creation date wasn't present!",
//                    graph.contains(ANY, versionURI, CREATED_DATE.asNode(), ANY));
        }
    }

    private static int countTriples(final DatasetGraph g) {
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
        patchLiteralProperty(serverAddress + id, title.getURI(), "First Title");

        try (final CloseableDataset dataset = getContent(serverAddress + id)) {
            assertTrue("Should find original title", dataset.asDatasetGraph().contains(ANY,
                    ANY, title.asNode(), createLiteral("First Title")));
        }
        logger.debug("Posting version v0.0.1");
        postObjectVersion(id, "v0.0.1");

        logger.debug("Replacing the title");
        patchLiteralProperty(serverAddress + id, title.getURI(), "Second Title");

        try (final CloseableDataset dataset = getContent(serverAddress + id + "/fcr:versions/v0.0.1")) {
            logger.debug("Got version profile:");
            final DatasetGraph versionResults = dataset.asDatasetGraph();

            assertTrue("Didn't find a version triple!", versionResults.contains(ANY,
                    ANY, type.asNode(), createURI(REPOSITORY_NAMESPACE + "Version")));
            assertTrue("Should find a title in historic version", versionResults.contains(ANY,
                    ANY, title.asNode(), ANY));
            assertTrue("Should find original title in historic version", versionResults.contains(ANY,
                    ANY, title.asNode(), createLiteral("First Title")));
            assertFalse("Should not find the updated title in historic version", versionResults.contains(ANY,
                    ANY, title.asNode(), createLiteral("Second Title")));
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
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + id + "/fcr:versions"))) {
            final DatasetGraph results = dataset.asDatasetGraph();
            final Node subject = createURI(serverAddress + id);
//            assertTrue("Didn't find a version triple!", results.contains(ANY, subject, HAS_VERSION.asNode(), ANY));
//            final Iterator<Quad> versionIt = results.find(ANY, subject, HAS_VERSION.asNode(), ANY);
//            while (versionIt.hasNext()) {
//                final String url = versionIt.next().getObject().getURI();
//               assertEquals("Version " + url + " isn't accessible!", OK.getStatusCode(), getStatus(new HttpGet(url)));
//            }
        }
    }

    @Test
    public void testVersionHeaders() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);

        createDatastream(id, "ds", "This DS will be versioned");
        final String dsId = id + "/ds";
        enableVersioning(dsId);

        // Version datastream
        postObjectVersion(dsId, "label");

        final String versionId = dsId + "/fcr:versions/label";

        final Link NON_RDF_SOURCE_LINK = fromUri(NON_RDF_SOURCE.getURI()).rel(type.getLocalName()).build();

        final Link DESCRIBED_BY_LINK = fromUri(serverAddress + versionId + "/" + FCR_METADATA).rel(
                DESCRIBED_BY.getLocalName()).build();

        // Look for expected Link headers
        final HttpHead headObjMethod = headObjMethod(versionId);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {

            final Collection<String> linkHeaders = getLinkHeaders(response);

            final Set<Link> resultSet = linkHeaders.stream().map(Link::valueOf).flatMap(link -> {
                final String linkRel = link.getRel();
                final URI linkUri = link.getUri();

                if (linkRel.equals(NON_RDF_SOURCE_LINK.getRel()) && linkUri.equals(NON_RDF_SOURCE_LINK.getUri())) {
                    // Found nonRdfSource!
                    return of(NON_RDF_SOURCE_LINK);

                } else if (linkRel.equals(DESCRIBED_BY_LINK.getRel()) && linkUri.equals(DESCRIBED_BY_LINK.getUri())) {
                    // Found describedby!
                    return of(DESCRIBED_BY_LINK);
                }
                return empty();
            }).collect(Collectors.toSet());

            assertTrue("No link headers found!", !linkHeaders.isEmpty());
            assertTrue("Didn't find NonRdfSource link header! " + NON_RDF_SOURCE_LINK + " ?= " + linkHeaders,
                    resultSet.contains(NON_RDF_SOURCE_LINK));
            assertTrue("Didn't find describedby link header! " + DESCRIBED_BY_LINK + " ?= " + linkHeaders,
                    resultSet.contains(DESCRIBED_BY_LINK));
        }
    }

    @Test
    public void testVersionListHeaders() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);

        final Link RDF_SOURCE_LINK = fromUri(RDF_SOURCE.getURI()).rel(type.getLocalName()).build();

        final HttpHead head = new HttpHead(serverAddress + id + "/fcr:versions");

        try (final CloseableHttpResponse response = execute(head)) {
            assertEquals(OK.getStatusCode(), getStatus(response));

            final Collection<String> linkHeaders = getLinkHeaders(response);

            final Set<Link> resultSet = linkHeaders.stream().map(Link::valueOf).flatMap(link -> {
                final String linkRel = link.getRel();
                final URI linkUri = link.getUri();
                if (linkRel.equals(RDF_SOURCE_LINK.getRel()) && linkUri.equals(RDF_SOURCE_LINK.getUri())) {
                    // Found RdfSource!
                    return of(RDF_SOURCE_LINK);
                }
                return empty();
            }).collect(Collectors.toSet());
            assertTrue("No link headers found!", !linkHeaders.isEmpty());
            assertTrue("Didn't find RdfSource link header! " + RDF_SOURCE_LINK + " ?= " + linkHeaders,
                    resultSet.contains(RDF_SOURCE_LINK));
        }
    }

    @Test
    public void testCreateLabeledVersion() throws IOException {
        logger.debug("Creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, title.getURI(), "Example Title");
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
        patchLiteralProperty(serverAddress + objId, title.getURI(), "Example Title");

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
        patchLiteralProperty(serverAddress + objId, title.getURI(), "First title");
        logger.debug("Posting a version with label \"" + label1 + "\"");
        postObjectVersion(objId, label1);
        logger.debug("Resetting the title");
        patchLiteralProperty(serverAddress + objId, title.getURI(), "Second title");

        logger.debug("Posting a version with label \"" + label1 + "\"");
        final HttpPost postVersion = postObjMethod(objId + "/fcr:versions");
        postVersion.addHeader("Slug", "label");
        assertEquals("Must not be allowed to create a version with a duplicate label!",
                CONFLICT.getStatusCode(), getStatus(postVersion));

        final HttpGet getVersions = new HttpGet(serverAddress + objId + "/fcr:versions");
        logger.debug("Retrieved versions");
        try (final CloseableDataset dataset = getDataset(getVersions)) {
            final DatasetGraph results = dataset.asDatasetGraph();
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
        patchLiteralProperty(serverAddress + objId, title.getURI(), title1);
        postObjectVersion(objId, firstVersionLabel);

        patchLiteralProperty(serverAddress + objId, title.getURI(), title2);
        postObjectVersion(objId, secondVersionLabel);

        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + objId))) {
            final DatasetGraph preRollback = dataset.asDatasetGraph();
            assertTrue("First title must be present!", preRollback.contains(ANY,
                    subject, title.asNode(), createLiteral(title1)));
            assertTrue("Second title must be present!", preRollback.contains(ANY,
                    subject, title.asNode(), createLiteral(title2)));
        }
        revertToVersion(objId, firstVersionLabel);
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + objId))) {
            final DatasetGraph postRollback = dataset.asDatasetGraph();
            assertTrue("First title must be present!", postRollback.contains(ANY,
                    subject, title.asNode(), createLiteral(title1)));
            assertFalse("Second title must NOT be present!", postRollback.contains(ANY,
                    subject, title.asNode(), createLiteral(title2)));
        }
        /*
         * Make the sure the node is checked out and able to be updated. Because the JCR concept of checked-out is
         * something we don't intend to expose through Fedora in the future, the following line is simply to test that
         * writes can be completed after a reversion.
         */
        patchLiteralProperty(serverAddress + objId, title.getURI(), "additional change");
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
        try (final CloseableDataset dataset = getContent(serverAddress + id)) {
//            assertFalse("Node must not have versionable mixin.", dataset.asDatasetGraph().contains(ANY,
//                    subject, HAS_VERSION_HISTORY.asNode(), ANY));
        }
        postObjectVersion(id, "label");
        try (final CloseableDataset dataset = getContent(serverAddress + id)) {
            final DatasetGraph updatedObjectProperties = dataset.asDatasetGraph();
//            assertTrue("Node is expected to contain hasVersions triple.", updatedObjectProperties.contains(ANY,
//                    subject, HAS_VERSION_HISTORY.asNode(), ANY));
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
        try (final CloseableDataset dataset = getContent(serverAddress + pid + "/" + dsid + "/fcr:metadata")) {
            final DatasetGraph originalObjectProperties = dataset.asDatasetGraph();
            final Node subject = createURI(serverAddress + pid + "/" + dsid);
//            assertFalse("Node must not contain any hasVersions triples.", originalObjectProperties.contains(ANY,
//                    subject, HAS_VERSION_HISTORY.asNode(), ANY));
        }
        // creating a version should succeed
        final HttpPost httpPost = new HttpPost(serverAddress + pid + "/" + dsid + "/fcr:versions");
        httpPost.setHeader("Slug", versionLabel);
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            final String dsVersionURI = getLocation(response);
            assertNotNull("No version location header found", dsVersionURI);
            // version triples should not have fcr:metadata as the subject
            try (final CloseableDataset dataset = getContent(dsVersionURI)) {
                final DatasetGraph dsVersionProperties = dataset.asDatasetGraph();
                assertTrue("Should have triples about the datastream", dsVersionProperties.contains(ANY,
                        createURI(dsVersionURI.replaceAll("/fcr:metadata","")), ANY, ANY));
                assertFalse("Shouldn't have triples about fcr:metadata", dsVersionProperties.contains(ANY,
                        createURI(dsVersionURI), ANY, ANY));
            }
        }
        // datastream should then have versions endpoint
        assertEquals(OK.getStatusCode(), getStatus(new HttpGet(serverAddress + pid + "/" + dsid + "/fcr:versions")));
        // datastream should then be versionable
        try (final CloseableDataset dataset = getContent(serverAddress + pid + "/" + dsid + "/fcr:metadata")) {
//            assertTrue("Node is expected to contain hasVersions triple.", dataset.asDatasetGraph().contains(
//                    ANY, createURI(serverAddress + pid + "/" + dsid), HAS_VERSION_HISTORY.asNode(), ANY));
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
            method.addHeader(ACCEPT, type);
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
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testOmissionOfJCRCVersionRDF() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + id))) {
            final Node subject = createURI(serverAddress + id);
            for (final String prohibitedProperty : jcrVersioningTriples) {
                assertFalse(prohibitedProperty + " must not appear in RDF for version-enabled node!",
                        dataset.asDatasetGraph().contains(ANY, subject, createURI(prohibitedProperty), ANY));
            }
        }
    }

    @Test
    public void testFixityOnVersionedResource() throws IOException {
        final String id = getRandomUniqueId();
        final String childId = id + "/child1";
        createObjectAndClose(id);
        createDatastream(id, "child1", "foo");
        enableVersioning(childId);
        postObjectVersion(childId, "v0");

        final HttpGet checkFixity = getObjMethod(childId + "/fcr:versions/v0/fcr:fixity");
        try (final CloseableHttpResponse response = execute(checkFixity)) {
            assertEquals("Did not get OK response", OK.getStatusCode(), getStatus(response));
        }

    }

    private static void patchLiteralProperty(final String url, final String predicate, final String literal)
            throws IOException {
        final HttpPatch updateObjectGraphMethod = new HttpPatch(url);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity(
                "INSERT DATA { <> <" + predicate + "> \"" + literal + "\" } "));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
    }

    private CloseableDataset getContent(final String url) throws IOException {
        final HttpGet getVersion = new HttpGet(url);
        getVersion.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINS.toString() + "\"");
        return getDataset(getVersion);
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
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertNotNull("No version location header found", getLocation(response));
        }
    }

    private static void revertToVersion(final String objId, final String versionLabel) {
        assertEquals(NO_CONTENT.getStatusCode(),
                getStatus(new HttpPatch(serverAddress + objId + "/fcr:versions/" + versionLabel)));
    }
}