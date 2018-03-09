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

import static com.google.common.collect.Iterators.size;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_FIXITY;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetGraph;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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

    private static String VERSION_LINK_URI = "<" + VERSIONED_RESOURCE.getURI() + ">; rel=\"type\"";

    private final List<String> rdfTypes = Arrays.asList(POSSIBLE_RDF_RESPONSE_VARIANTS_STRING);

    private String[] timeMapResponseTypes;

    @Before
    public void setUp() {
        rdfTypes.add(APPLICATION_LINK_FORMAT);
        timeMapResponseTypes = rdfTypes.toArray(new String[rdfTypes.size()]);
    }

    // TimeMap returns variable results, could test for application/link-format result.
    @Test
    public void testGetObjectVersionProfile() throws IOException {
        final String id = getRandomUniqueId();

        createObjectAndClose(id);
        enableVersioning(id);
        postObjectVersion(id);
        logger.debug("Retrieved version profile:");
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + id + "/" + FCR_VERSIONS))) {
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

    private static void enableVersioning(final String pid) throws IOException {
        final HttpGet request = new HttpGet(serverAddress + pid);
        request.setHeader("Prefer",
            "return=representation; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");
        final HttpPut putReq = new HttpPut(serverAddress + pid);
        try (final CloseableHttpResponse response = execute(request)) {
            if (getStatus(response) == OK.getStatusCode()) {
                // Resource exists so get the body to put back with header
                putReq.setEntity(response.getEntity());
                putReq.setHeader("Prefer", "handling=lenient; received=\"minimal\"");
            }
        }
        putReq.setHeader("Link", VERSION_LINK_URI);
        try {
            execute(putReq);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Should work with Memento
    @Test
    public void testGetUnversionedObjectVersionProfile() {
        final String pid = getRandomUniqueId();

        createObject(pid);

        final HttpGet getVersion = new HttpGet(serverAddress + pid + "/" + FCR_VERSIONS);
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
        final String versionURI = postObjectVersion(id);

        logger.debug("Replacing the title");
        patchLiteralProperty(serverAddress + id, title.getURI(), "Second Title");

        try (final CloseableDataset dataset = getContent(versionURI)) {
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

    // Versioning is now individual
    @Test
    @Ignore
    public void testVersioningANodeWithAVersionableChild() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);
        logger.debug("Adding a child");
        createDatastream(id, "ds", "This DS will not be versioned");
        logger.debug("Posting version");
        final String parentVersionURI = postObjectVersion(id);

        logger.debug("Retrieved version profile:");
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + id + "/" + FCR_VERSIONS))) {
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
        final String versionId = postObjectVersion(dsId);

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

        final HttpHead head = new HttpHead(serverAddress + id + "/" + FCR_VERSIONS);

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

    // Mementos are not labelled.
    @Test
    @Ignore
    public void testCreateLabeledVersion() throws IOException {
        logger.debug("Creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, title.getURI(), "Example Title");
        logger.debug("Posting a labeled version");
        postObjectVersion(objId);
    }

    @Test
    public void testCreateTwoVersionsWithSameLabel() throws IOException {
        final Instant date1 = LocalDateTime.of(2000, 10, 15, 11, 35, 23).atZone(ZoneId.of("UTC")).toInstant();
        final String date1format = DateTimeFormatter.RFC_1123_DATE_TIME.format(date1);
        final Instant date2 = LocalDateTime.of(2011, 4, 1, 18, 35, 23).atZone(ZoneId.of("UTC")).toInstant();
        final String date2format = DateTimeFormatter.RFC_1123_DATE_TIME.format(date2);

        logger.debug("creating an object");
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);

        logger.debug("Setting a title");
        patchLiteralProperty(serverAddress + objId, title.getURI(), "First title");
        logger.debug("Posting a version with a date \"" + date1format + "\"");
        final String version1id = postVersion(objId, date1);

        logger.debug("Resetting the title");
        patchLiteralProperty(serverAddress + objId, title.getURI(), "Second title");

        logger.debug("Posting a version with a date \"" + date1format + "\"");
        final HttpPost postVersion = postObjMethod(objId + "/" + FCR_VERSIONS);
        postVersion.addHeader(MEMENTO_DATETIME_HEADER, date1format);
        assertEquals("Must not be allowed to create a version with a duplicate label!",
                CONFLICT.getStatusCode(), getStatus(postVersion));

        final HttpGet getVersions = new HttpGet(serverAddress + objId + "/" + FCR_VERSIONS);
        logger.debug("Retrieved versions");
        try (final CloseableDataset dataset = getDataset(getVersions)) {
            final DatasetGraph results = dataset.asDatasetGraph();
            assertEquals("Expected exactly 3 triples!", 3, countTriples(results));
            logger.debug("Posting a version with date \"" + date2format + "\"");
            postVersion(objId, date2);

            logger.debug("Deleting first dated version \"" + date1format + "\"");
            final HttpDelete remove = new HttpDelete(version1id);
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(remove));

            logger.debug("Reusing a date new version with date \"" + date1format + "\"");
            postVersion(objId, date1);
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
        final Instant date1 = LocalDateTime.of(2000, 10, 15, 11, 35, 23).atZone(ZoneId.of("UTC")).toInstant();
        final String date1format = DateTimeFormatter.RFC_1123_DATE_TIME.format(date1);

        logger.debug("creating an object");
        final String objId1 = getRandomUniqueId();
        createObjectAndClose(objId1);
        enableVersioning(objId1);

        logger.debug("Posting a version with date \"" + date1format + "\"");
        postObjectVersion(objId1, date1);

        logger.debug("creating another object");
        final String objId2 = getRandomUniqueId();
        createObjectAndClose(objId2);
        enableVersioning(objId2);

        logger.debug("Posting a version with date \"" + date1format + "\"");
        postObjectVersion(objId2, date1);
    }

    // A datastream is just another resource, so this is irrelevant.
    @Test
    @Ignore
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
    public void testInvalidVersionDatetime() throws IOException {
        final String objId = getRandomUniqueId();
        final String invalidDate = "blah";
        createObjectAndClose(objId);
        enableVersioning(objId);
        final HttpPost postReq = new HttpPost(serverAddress + objId + "/" + FCR_VERSIONS);
        postReq.addHeader(MEMENTO_DATETIME_HEADER, invalidDate);
        assertEquals(CONFLICT.getStatusCode(), getStatus(postReq));
    }

    // This is around reverting which we no longer do
    @Test
    @Ignore
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
        // postObjectVersion(objId, firstVersionLabel);

        patchLiteralProperty(serverAddress + objId, title.getURI(), title2);
        // postObjectVersion(objId, secondVersionLabel);

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
        final Instant date1 = LocalDateTime.of(2000, 10, 15, 11, 35, 23).atZone(ZoneId.of("UTC")).toInstant();

        createResource(serverAddress + objId);
        createObjectAndClose(objId);
        enableVersioning(objId);

        final String version1Uri = postObjectVersion(objId, date1);

        // make sure the version exists
        assertEquals(OK.getStatusCode(),
            getStatus(new HttpGet(version1Uri)));

        // remove the version we created
        assertEquals(NO_CONTENT.getStatusCode(),
            getStatus(new HttpDelete(version1Uri)));

        // make sure the version is gone
        assertEquals(NOT_FOUND.getStatusCode(),
            getStatus(new HttpGet(version1Uri)));
    }

    // label tests are useless
    @Test
    @Ignore
    public void testRemoveInvalidVersion() {
        // create an object
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        // enableVersioning(objId);

        // removing a non-existent version should 404
        assertEquals(NOT_FOUND.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + objId + "/fcr:versions/invalid-version-label")));
    }

    // JCR version issues are irrelevant
    @Test
    @Ignore
    public void testRemoveCurrentVersion() throws IOException {
        // create an object
        final String versionLabel = "testVersionNumberUno";
        final String objId = getRandomUniqueId();
        createObjectAndClose(objId);
        enableVersioning(objId);
        // postObjectVersion(objId, versionLabel);

        // removing the current version should 400
        assertEquals(BAD_REQUEST.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + objId + "/fcr:versions/" + versionLabel)));
    }

    // no longer exists, headers are tested in FedoraVersioningIT
    @Test
    @Ignore
    public void testVersionOperationAddsVersionableMixin() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final Node subject = createURI(serverAddress + id);
        try (final CloseableDataset dataset = getContent(serverAddress + id)) {
//            assertFalse("Node must not have versionable mixin.", dataset.asDatasetGraph().contains(ANY,
//                    subject, HAS_VERSION_HISTORY.asNode(), ANY));
        }
        // postObjectVersion(id, "label");
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
        createDatastream(pid, dsid, originalContent);

        // datastream should not have fcr:versions endpoint
        assertEquals(NOT_FOUND.getStatusCode(),
            getStatus(new HttpGet(serverAddress + pid + "/" + dsid + "/" + FCR_VERSIONS)));

        // datastream should not be versionable
        final HttpGet getReq = new HttpGet(serverAddress + pid + "/" + dsid + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(getReq)) {
            checkForNLinkHeaders(response, VERSIONED_RESOURCE.getURI(), "type", 0);
        }
        // creating a version should fail
        final HttpPost httpPost = new HttpPost(serverAddress + pid + "/" + dsid + "/fcr:versions");

        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(NOT_FOUND.getStatusCode(), getStatus(response));
        }

        // Make versionable
        enableVersioning(serverAddress + pid + "/" + dsid);

        // Now it should succeed
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

        // update the content
        final String updatedContent = "This is the updated content";
        executeAndClose(putDSMethod(pid, dsid, updatedContent));
        try (final CloseableHttpResponse dsResponse = execute(getDSMethod(pid, dsid))) {
            assertEquals(updatedContent, EntityUtils.toString(dsResponse.getEntity()));
        }
    }

    @Test
    public void testTimeMapResponseContentTypes() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);

        for (final String type : timeMapResponseTypes) {
            final HttpGet method = new HttpGet(serverAddress + id + "/fcr:versions");
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testGetVersionResponseContentTypes() throws IOException {
        final String id = getRandomUniqueId();

        createObjectAndClose(id);
        enableVersioning(id);
        final String versionUri = postObjectVersion(id);

        for (final String type : timeMapResponseTypes) {
            final HttpGet method = new HttpGet(versionUri);
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    // no more JCR
    @Test
    @Ignore
    public void testOmissionOfJCRCVersionRDF() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        enableVersioning(id);
        try (final CloseableDataset dataset = getDataset(new HttpGet(serverAddress + id))) {
            final Node subject = createURI(serverAddress + id);
            // for (final String prohibitedProperty : jcrVersioningTriples) {
            // assertFalse(prohibitedProperty + " must not appear in RDF for version-enabled node!",
            // dataset.asDatasetGraph().contains(ANY, subject, createURI(prohibitedProperty), ANY));
            // }
        }
    }

    @Test
    public void testFixityOnVersionedResource() throws IOException {
        final String id = getRandomUniqueId();
        final String childId = id + "/child1";
        createObjectAndClose(id);
        createDatastream(id, "child1", "foo");
        enableVersioning(childId);
        final String childVersion = postObjectVersion(childId);

        final HttpGet checkFixity = getObjMethod(childVersion + "/" + FCR_FIXITY);
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
        getVersion.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINED.toString() + "\"");
        return getDataset(getVersion);
    }

    public String postObjectVersion(final String pid) throws IOException {
        return postVersion(pid, Instant.now());
    }

    public String postObjectVersion(final String pid, final Instant datetime) throws IOException {
        return postVersion(pid, datetime);
    }

    public String postDsVersion(final String pid, final String dsId) throws IOException {
        return postObjectVersion(pid + "/" + dsId);
    }

    public String postVersion(final String path, final Instant mementoDateTime) throws IOException {
        logger.debug("Posting version");
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions");
        postVersion.addHeader(MEMENTO_DATETIME_HEADER, DateTimeFormatter.RFC_1123_DATE_TIME.format(mementoDateTime));
        try (final CloseableHttpResponse response = execute(postVersion)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertNotNull("No version location header found", getLocation(response));
            return getLocation(response);
        }
    }

    private static void revertToVersion(final String objId, final String versionLabel) {
        assertEquals(NO_CONTENT.getStatusCode(),
                getStatus(new HttpPatch(serverAddress + objId + "/fcr:versions/" + versionLabel)));
    }
}