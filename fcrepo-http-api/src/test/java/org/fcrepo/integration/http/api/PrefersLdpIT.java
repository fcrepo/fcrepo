/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_SERVER_MANAGED;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.fcrepo.http.commons.test.util.CloseableDataset;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.DC_11;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Integration tests for various Prefer headers
 * @author whikloj
 * @since 6.0.0
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class PrefersLdpIT extends AbstractResourceIT {

    private String mainResource;

    private Quad userRdfTriple;

    private Quad systemManagedPropertyTriple;

    private Quad membershipTriple;

    private Quad inboundReferenceTriple;

    private Quad containmentTriple;

    private Quad embededTriple;

    /**
     * Execute a request, test for a CREATED response code and return the location.
     * @param request the request.
     * @return the location URI.
     * @throws Exception on problems with request.
     */
    private String createAndGetLocation(final HttpUriRequest request) throws Exception {
        try (final var response = execute(request)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return getLocation(response);
        }
    }

    /**
     * Generate a quad.
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     * @return the quad
     */
    private Quad generateQuad(final String subject, final String predicate, final Node object) {
        return generateQuad(createURI(subject), predicate, object);
    }

    /**
     * Generate a quad.
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     * @return the quad
     */
    private Quad generateQuad(final Node subject, final String predicate, final String object) {
        final Node objectNode = (object == null ? ANY : createURI(object));
        return generateQuad(subject, predicate, objectNode);
    }

    /**
     * Generate a quad.
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     * @return the quad
     */
    private Quad generateQuad(final Node subject, final String predicate, final Node object) {
        return Quad.create(ANY, subject, createURI(predicate), object);
    }

    /**
     * Generate a common set of triples for the Prefer header tests.
     * @throws Exception problems with a http request.
     */
    private void generateResources() throws Exception {
        // Make the main resource to check with a user triple.
        final var postMain = postObjMethod();
        final String body = "<> <" + DC_11.title + "> \"The object's title\" .";
        postMain.setHeader(CONTENT_TYPE, "text/turtle");
        postMain.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        mainResource = createAndGetLocation(postMain);
        final Node mainNode = createURI(mainResource);
        userRdfTriple = generateQuad(mainNode, DC_11.title.getURI(), createLiteral("The object's title"));
        systemManagedPropertyTriple = generateQuad(mainNode, CREATED_DATE.getURI(), ANY);
        // Make a DirectContainer pointing at the main resource.
        final var postDC = postObjMethod();
        postDC.setHeader("Link", "<" + DIRECT_CONTAINER + ">; rel=\"type\"");
        final String pcdmHasMember = "http://pcdm.org/models#hasMember";
        final String dcBody =
                "@prefix ldp: <http://www.w3.org/ns/ldp#> . <> " +
                        "ldp:membershipResource <" + mainResource + "> ; ldp:hasMemberRelation " +
                        "<" + pcdmHasMember + "> .";
        postDC.setHeader(CONTENT_TYPE, "text/turtle");
        postDC.setEntity(new StringEntity(dcBody, StandardCharsets.UTF_8));
        final String directResource = createAndGetLocation(postDC);
        // Make a member of the DC.
        final var postDCchild = new HttpPost(directResource);
        final String directResourceReference = createAndGetLocation(postDCchild);
        membershipTriple = generateQuad(mainNode, pcdmHasMember, directResourceReference);
        // Make a contained resource.
        final var postChild = new HttpPost(mainResource);
        final String childBody = "<> <" + DC_11.title + "> \"The child's title\" .";
        postChild.setHeader(CONTENT_TYPE, "text/turtle");
        postChild.setEntity(new StringEntity(childBody, StandardCharsets.UTF_8));
        final String containedResource = createAndGetLocation(postChild);
        containmentTriple = generateQuad(mainNode, CONTAINS.getURI(), containedResource);
        embededTriple = generateQuad(containedResource, DC_11.title.getURI(), createLiteral("The child's title"));
        // Make an inbound reference.
        final var otherRef = postObjMethod();
        final String refBody = "<> <http://example.org/related> <" + mainResource + "> .";
        otherRef.setHeader(CONTENT_TYPE, "text/turtle");
        otherRef.setEntity(new StringEntity(refBody, StandardCharsets.UTF_8));
        final String otherReference = createAndGetLocation(otherRef);
        inboundReferenceTriple = generateQuad(otherReference, "http://example.org/related", mainNode);
    }

    @Test
    public void testNormalTriples() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertTrue(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertTrue(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferIncludeMinimal() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(PREFER_MINIMAL_CONTAINER.getURI(), null));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertTrue(graph.contains(systemManagedPropertyTriple));
            assertFalse(graph.contains(containmentTriple));
            assertFalse(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferOmitMinimal() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(null, PREFER_MINIMAL_CONTAINER.getURI()));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertFalse(graph.contains(userRdfTriple));
            assertFalse(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertTrue(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferIncludeMinimalAndSystem() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                PREFER_MINIMAL_CONTAINER.getURI() + " " + PREFER_SERVER_MANAGED.getURI(),
                null));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertTrue(graph.contains(systemManagedPropertyTriple));
            assertFalse(graph.contains(containmentTriple));
            assertFalse(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferOmitSystem() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                null,
                PREFER_SERVER_MANAGED.getURI()));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertFalse(graph.contains(systemManagedPropertyTriple));
            // Containment is consider system managed.
            assertFalse(graph.contains(containmentTriple));
            // Membership is considered system managed.
            assertFalse(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferOmitSystemIncludeContainment() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                PREFER_CONTAINMENT.getURI(),
                PREFER_SERVER_MANAGED.getURI()));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertFalse(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertFalse(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferIncludeMinimalOmitSystem() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                PREFER_MINIMAL_CONTAINER.getURI(),
                PREFER_SERVER_MANAGED.getURI()));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertFalse(graph.contains(systemManagedPropertyTriple));
            assertFalse(graph.contains(containmentTriple));
            assertFalse(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferExcludeMembership() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                null,
                PREFER_MEMBERSHIP.getURI()));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertTrue(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertFalse(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferIncludeInbound() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                INBOUND_REFERENCES.getURI(),
                null));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertTrue(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertTrue(graph.contains(membershipTriple));
            assertTrue(graph.contains(inboundReferenceTriple));
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferIncludeEmbed() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                EMBED_CONTAINED.getURI(),
                null));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(userRdfTriple));
            assertTrue(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertTrue(graph.contains(membershipTriple));
            assertFalse(graph.contains(inboundReferenceTriple));
            assertTrue(graph.contains(embededTriple));
        }
    }

    @Test
    public void testPreferIncludeEmbedInbound_OmitMinimal() throws Exception {
        generateResources();
        final var getReq = new HttpGet(mainResource);
        getReq.setHeader("Prefer", preferLink(
                EMBED_CONTAINED.getURI() + " " + INBOUND_REFERENCES.getURI(),
                PREFER_MINIMAL_CONTAINER.getURI()));
        try (final var dataset = getDataset(getReq)) {
            final var graph = dataset.asDatasetGraph();
            assertFalse(graph.contains(userRdfTriple));
            assertFalse(graph.contains(systemManagedPropertyTriple));
            assertTrue(graph.contains(containmentTriple));
            assertTrue(graph.contains(membershipTriple));
            assertTrue(graph.contains(inboundReferenceTriple));
            // Omitting minimal removes user rdf, this also happens to the embedded resources.
            assertFalse(graph.contains(embededTriple));
        }
    }

    @Test
    public void testGetObjectGraphMinimal() throws IOException {
        final String uri;
        final HttpPost httpPost = postObjMethod();
        httpPost.setHeader("Link", BASIC_CONTAINER_LINK_HEADER);
        httpPost.setHeader(CONTENT_TYPE, "text/turtle");
        httpPost.setEntity(new StringEntity("<> <" + DCTITLE.getURI() + "> \"The title\" ."));
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            uri = getLocation(response);
        }
        final Node resource = createURI(uri);
        // Create a contained child.
        final HttpPut httpPut = new HttpPut(uri + "/a");
        assertEquals(CREATED.getStatusCode(), getStatus(httpPut));

        // Now test with include preference
        final HttpGet httpGet = new HttpGet(uri);
        final String preferHeader2 = preferLink(PREFER_MINIMAL_CONTAINER.toString(), null);
        httpGet.setHeader("Prefer", preferHeader2);
        try (final CloseableHttpResponse response = execute(httpGet);
             final CloseableDataset dataset = getDataset(response)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse(graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext(), "Didn't expect members");
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue(preferenceApplied.contains(preferHeader2),
                    "Preference-Applied header doesn't matched");
            assertTrue(graph.contains(ANY, resource, DCTITLE, ANY), "Missing a user RDF triple");
        }
        // Now try with Omit minimal
        final HttpGet getOmit = new HttpGet(uri);
        final String preferOmitHeader = preferLink(null, PREFER_MINIMAL_CONTAINER.toString());
        getOmit.addHeader("Prefer", preferOmitHeader);
        try (final CloseableHttpResponse response = execute(getOmit);
             final CloseableDataset dataset = getDataset(response)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext(), "Expected members");
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue(preferenceApplied.contains(preferOmitHeader),
                    "Preference-Applied header doesn't matched");
            assertFalse(graph.contains(ANY, resource, DCTITLE, ANY), "Should not return user RDF triples");
        }
    }

    @Test
    public void testGetObjectOmitMembership() throws IOException {
        final String id = getRandomUniqueId();
        final Node resource = createURI(serverAddress + id);
        createObjectAndClose(id, BASIC_CONTAINER_LINK_HEADER);
        final String initialEtag = getEtag(serverAddress + id);
        createObjectAndClose(id + "/a");
        final String withChildEtag = getEtag(serverAddress + id);
        assertNotEquals(initialEtag, withChildEtag);
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", preferLink(null, PREFER_CONTAINMENT + " " + PREFER_MEMBERSHIP));
        final String omitEtag = getEtag(getObjMethod);
        assertEquals(initialEtag, omitEtag);
        assertNotEquals(withChildEtag, omitEtag);
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, BASIC_CONTAINER.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext(),
                    "Expected server managed");
            assertFalse(graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext(), "Expected no members");
        }
    }

    @Test
    public void testGetObjectOmitContainment() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String initialEtag = getEtag(serverAddress + id);
        final String location = serverAddress + id;
        final String updateString = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + location + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put = putObjMethod(id + "/a", "text/turtle", updateString);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final String withMemberEtag = getEtag(serverAddress + id);
        assertNotEquals(initialEtag, withMemberEtag);

        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", preferLink(null, PREFER_CONTAINMENT.toString()));
        final String omitEtag = getEtag(getObjMethod);
        assertNotEquals(initialEtag, omitEtag, "Should not match initial because of membership");
        assertNotEquals(withMemberEtag, omitEtag);
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            assertTrue(graph.find(ANY, resource, LDP_MEMBER.asNode(), ANY).hasNext(), "Didn't find member resources");
            assertTrue(graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, BASIC_CONTAINER.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext(), "Expected server managed");
            assertTrue(graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext(),
                    "Expected server managed");
            assertFalse(graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext(), "Expected nothing contained");
        }
    }

    @Test
    public void testGetObjectOmitServerManaged() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        final String updateString = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + location + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put = putObjMethod(id + "/a", "text/turtle", updateString);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final String withMemberEtag = getEtag(serverAddress + id);

        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", preferLink(null, PREFER_SERVER_MANAGED.toString()));
        assertNotEquals(withMemberEtag, getEtag(getObjMethod), "Etag should not match with SMTs excluded");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            // Membership is now consider system managed and so is removed with the rest.
            assertFalse(graph.find(ANY, resource, LDP_MEMBER.asNode(), ANY).hasNext(), "Didn't find member resources");
            assertFalse(graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, BASIC_CONTAINER.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext(),
                    "Expected nothing server managed");
        }
    }

    @Test
    public void testGetObjectIncludeContainmentAndOmitServerManaged() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String location = serverAddress + id;
        final String updateString = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + location + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put = putObjMethod(id + "/a", "text/turtle", updateString);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final String withMemberEtag = getEtag(serverAddress + id);

        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", preferLink(PREFER_CONTAINMENT.toString(), PREFER_SERVER_MANAGED.toString()));
        assertNotEquals(withMemberEtag, getEtag(getObjMethod), "Etag should not match with SMTs excluded");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            assertTrue(graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext(), "Didn't find ldp containment");
        }
    }

    @Test
    public void testGetLDPRmOmitServerManaged() throws Exception {
        final String versionedResourceURI = createAndGetLocation(postObjMethod());
        final String mementoResourceURI = getLocation(new HttpPost(versionedResourceURI + "/fcr:versions"));
        final String mementoEtag = getEtag(mementoResourceURI);

        final HttpGet mementoGetMethod = new HttpGet(mementoResourceURI);
        mementoGetMethod.addHeader("Prefer", preferLink(null, PREFER_SERVER_MANAGED.toString()));
        assertNotEquals(mementoEtag, getEtag(mementoGetMethod));
        try (final CloseableDataset dataset = getDataset(mementoGetMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(versionedResourceURI);
            assertFalse(graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext(),
                    "Expected nothing server managed");
            assertFalse(graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext(),
                    "Expected nothing server managed");
        }
    }

    @Test
    public void testGetLDPRmWithoutOmitServerManager() throws Exception {
        final String versionedResourceURI = createAndGetLocation(postObjMethod());
        final String mementoResourceURI = getLocation(new HttpPost(versionedResourceURI + "/fcr:versions"));

        final HttpGet mementoGetMethod = new HttpGet(mementoResourceURI);
        try (final CloseableDataset dataset = getDataset(mementoGetMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(versionedResourceURI);
            assertTrue(graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext(),
                    "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext(),
                    "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext(),
                    "Expected server managed");
            assertTrue(graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext(),
                    "Expected server managed");
            assertTrue(graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext(),
                    "Expected server managed");
            assertTrue(graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext(),
                    "Expected server managed");
        }
    }

    @Test
    public void testEmbeddedContainedResources() throws IOException {
        final String id = getRandomUniqueId();
        final String binaryId = "binary0";
        final String preferEmbed = preferLink(EMBED_CONTAINED.toString(), null);

        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(id)));
        assertEquals(CREATED.getStatusCode(), getStatus(putDSMethod(id, binaryId, "some test content")));

        final HttpPatch httpPatch = patchObjMethod(id + "/" + binaryId + "/fcr:metadata");
        httpPatch.addHeader(CONTENT_TYPE, "application/sparql-update");
        httpPatch.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> 'this is a title' } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPatch));

        final String withoutEmbedEtag = getEtag(serverAddress + id);

        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Prefer", preferEmbed);
        assertNotEquals(withoutEmbedEtag, getEtag(httpGet));
        try (final CloseableHttpResponse response = execute(httpGet)) {
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue(preferenceApplied.contains(preferEmbed), "Preference-Applied header doesn't match");

            final DatasetGraph graphStore = getDataset(response).asDatasetGraph();
            assertTrue(graphStore.contains(ANY, createURI(serverAddress + id + "/" + binaryId),
                    title.asNode(), createLiteral("this is a title")),
                    "Property on child binary should be found!" + graphStore);
        }
    }

    @Test
    public void testEmbeddedContainedResourcesNotToDeep() throws IOException {
        final String id = getRandomUniqueId();
        final String level1 = id + "/" + getRandomUniqueId();
        final String level2 = level1 + "/" + getRandomUniqueId();
        final String preferEmbed = preferLink(EMBED_CONTAINED.toString(), null);

        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(id)));

        final HttpPut putLevel1 = putObjMethod(level1);
        putLevel1.addHeader(CONTENT_TYPE, "text/turtle");
        putLevel1.setEntity(new StringEntity("<> <" + title + "> \"First level\"", UTF_8));
        assertEquals(CREATED.getStatusCode(), getStatus(putLevel1));

        final HttpPut putLevel2 = putObjMethod(level2);
        putLevel2.addHeader(CONTENT_TYPE, "text/turtle");
        putLevel2.setEntity(new StringEntity("<> <" + title + "> \"Second level\"", UTF_8));
        assertEquals(CREATED.getStatusCode(), getStatus(putLevel2));

        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Prefer", preferEmbed);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue(preferenceApplied.contains(preferEmbed), "Preference-Applied header doesn't match");

            final DatasetGraph graphStore = getDataset(response).asDatasetGraph();
            assertTrue(graphStore.contains(ANY, createURI(serverAddress + level1),
                    title.asNode(), createLiteral("First level")),
                    "Property on child binary should be found!" + graphStore);
            assertFalse(graphStore.contains(ANY,
                                        createURI(serverAddress + level2),
                                        title.asNode(), createLiteral("Second level")
                                ),
                    "Property from embedded resource's own child should not be found."
                        );
        }
    }
}
