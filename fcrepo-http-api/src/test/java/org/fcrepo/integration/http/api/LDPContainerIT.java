/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.awaitility.Awaitility.await;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.Status;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author bbpennel
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class LDPContainerIT extends AbstractResourceIT {

    private final String PCDM_HAS_MEMBER = "http://pcdm.org/models#hasMember";

    private final Property PCDM_HAS_MEMBER_PROP = createProperty(PCDM_HAS_MEMBER);

    private final Property EX_IS_MEMBER_PROP = createProperty("http://example.com/isMember");

    public static final Property PROXY_FOR = createProperty("http://example.com/proxyFor");

    private static final String DIRECT_CONTAINER_LINK_HEADER = "<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\"";

    private static final String INDIRECT_CONTAINER_LINK_HEADER = "<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\"";

    private DefaultOcflObjectSessionFactory objectSessionFactory;
    private OcflPropsConfig ocflPropsConfig;

    @BeforeEach
    public void init() {
        objectSessionFactory = getBean(DefaultOcflObjectSessionFactory.class);
        ocflPropsConfig = getBean(OcflPropsConfig.class);
    }

    @AfterEach
    public void after() {
        objectSessionFactory.setDefaultCommitType(CommitType.NEW_VERSION);
        ocflPropsConfig.setAutoVersioningEnabled(true);
    }

    @Test
    public void testIndirectContainerDefaults() throws Exception {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final Model model = getModel(id);

        final Resource resc = model.getResource(subjectURI);
        assertTrue(resc.hasProperty(RDF.type, INDIRECT_CONTAINER), "Must have container type");

        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must be set");

        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must be set");

        assertTrue(resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT),
                "Default ldp:insertedContentRelation must be set");
    }

    @Test
    public void testIndirectContainerOverrides() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final String indirectId = createIndirectContainer(parentURI);
        final String indirectURI = serverAddress + indirectId;

        final Model model = getModel(indirectId);
        final Resource resc = model.getResource(indirectURI);
        assertTrue(resc.hasProperty(RDF.type, INDIRECT_CONTAINER), "Must have container type");

        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)),
                "Provided ldp:membershipResource must be present");
        assertFalse(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must not be present");

        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, PCDM_HAS_MEMBER_PROP),
                "Provided ldp:hasMemberRelation must be set");
        assertFalse(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must not be present");

        assertTrue(resc.hasProperty(INSERTED_CONTENT_RELATION, PROXY_FOR),
                "Provided ldp:insertedContentRelation must be set");
        assertFalse(resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT),
                "Default ldp:insertedContentRelation must not be present");
    }

    @Test
    public void testIndirectContainerDefaultsAfterPUT() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final String indirectId = createIndirectContainer(parentURI);
        final String indirectURI = serverAddress + indirectId;

        final Model replaceModel = getModel(indirectId);
        replaceModel.removeAll(null, MEMBERSHIP_RESOURCE, null);
        replaceModel.removeAll(null, HAS_MEMBER_RELATION, null);
        replaceModel.removeAll(null, INSERTED_CONTENT_RELATION, null);

        replacePropertiesWithPUT(indirectURI, replaceModel);

        final Model model = getModel(indirectId);
        final Resource resc = model.getResource(indirectURI);
        assertTrue(resc.hasProperty(RDF.type, INDIRECT_CONTAINER), "Must have container type");

        assertFalse(resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)),
                "Provided ldp:membershipResource must be removed");
        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must be set");

        assertFalse(resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)),
                "Provided ldp:hasMemberRelation must be removed");
        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must be set");

        assertFalse(resc.hasProperty(INSERTED_CONTENT_RELATION, PROXY_FOR),
                "Provided ldp:insertedContentRelation must be removed");
        assertTrue(resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT),
                "Default ldp:insertedContentRelation must be present");
    }

    @Test
    public void testIndirectContainerDefaultsAfterPatch() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final String indirectId = createIndirectContainer(parentURI);
        final String indirectURI = serverAddress + indirectId;

        final HttpPatch patch = new HttpPatch(indirectURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:membershipResource <" + parentURI + "> ;\n" +
                "ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> ;\n" +
                "ldp:insertedContentRelation <" + PROXY_FOR.getURI() + "> . } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model = getModel(indirectId);
        final Resource resc = model.getResource(indirectURI);
        assertTrue(resc.hasProperty(RDF.type, INDIRECT_CONTAINER), "Must have container type");

        assertFalse(resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)),
                "Provided ldp:membershipResource must be removed");
        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must be set");

        assertFalse(resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)),
                "Provided ldp:hasMemberRelation must be removed");
        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must be set");

        assertFalse(resc.hasProperty(INSERTED_CONTENT_RELATION, PROXY_FOR),
                "Provided ldp:insertedContentRelation must be removed");
        assertTrue(resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT),
                "Default ldp:insertedContentRelation must be present");
    }

    @Test
    public void testIndirectContainerPatchWithLdpContainsHasMember() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation ?memRel . }\n" +
                "INSERT { <> ldp:hasMemberRelation <" + RdfLexicon.CONTAINS + "> }\n" +
                " WHERE { <> ldp:hasMemberRelation ?memRel}"));
        assertEquals(CONFLICT.getStatusCode(), getStatus(patch),
                "Patch with sparql update allowed ldp:contains in indirect container!");
    }

    @Test
    public void testIndirectContainerPatchWithLdpContainsIsMember() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation ?memRel . }\n" +
                "INSERT { <> ldp:isMemberOfRelation <" + RdfLexicon.CONTAINS + "> }\n" +
                " WHERE { <> ldp:hasMemberRelation ?memRel}"));
        assertEquals(CONFLICT.getStatusCode(), getStatus(patch),
                "Patch with sparql update allowed ldp:contains in indirect container!");
    }

    @Test
    public void testIndirectContainerPatchWithoutLdpContains() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation ?memRel . }\n" +
                "INSERT { <> ldp:hasMemberRelation <info:some/relation> }\n" +
                " WHERE { <> ldp:hasMemberRelation ?memRel}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch),
                "Patch with sparql update did not allow non SMT relation in indirect container!");
    }

    @Test
    public void testETagOnDeletedLdpIndirectContainerChild() throws Exception {
        final String membershipId = createBasicContainer();
        final String membershipUri = serverAddress + membershipId;

        // Create the IndirectContainer
        final String indirectId = createIndirectContainer(membershipUri);

        final String proxyId = indirectId + "/proxy1";
        final String proxyUri = serverAddress + proxyId;

        final String etag0 = getEtag(membershipUri);

        // Create member
        final String memberId = createBasicContainer();
        final String memberUri = serverAddress + memberId;

        // Create proxy to the member
        createProxy(proxyId, memberUri);

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, memberId);

        final String etag1 = getEtag(membershipUri);
        assertNotEquals(etag0, etag1, "ETag didn't change!");

        // Wait a second so that the creation and deletion of the child are not simultaneous
        TimeUnit.SECONDS.sleep(1);

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(proxyUri)),
                "Child resource not deleted!");

        assertHasNoMembership(membershipId, PCDM_HAS_MEMBER_PROP);

        final String etag2 = getEtag(membershipUri);

        assertNotEquals(etag1, etag2, "ETag didn't change!");
    }

    // Ensure that membership is generated/manager when an IndirectContainer is also a proxy
    @Test
    public void indirectContainerWithIndirectContainerChild() throws Exception {
        final String membershipId = createBasicContainer();
        final String membershipUri = serverAddress + membershipId;

        // Create the parent IndirectContainer
        final String parentIndirectId = createIndirectContainer(membershipUri);

        // Create members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        // Create the nested child IndirectContainer,
        final String childIndirectId = parentIndirectId + "/childIndirect";
        createIndirectContainer(childIndirectId, membershipUri, PCDM_HAS_MEMBER_PROP, PROXY_FOR.getURI(), false);

        // Add a proxy into the child container
        final String proxyId = childIndirectId + "/proxy1";
        createProxy(proxyId, member1Uri);

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member1Id);

        // Make the nested child into a proxy itself by adding the insertedContentRelation
        final HttpPatch patch = patchObjMethod(childIndirectId);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>" +
                " INSERT { <> <" + PROXY_FOR + "> <" + member2Uri + "> }" +
                " WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);

        // Change the hasMemberRelation of the child container
        final HttpPatch patch2 = patchObjMethod(childIndirectId);
        patch2.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch2.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> . }" +
                "INSERT { <> ldp:hasMemberRelation ldp:member } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch2)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, member2Id);
        assertHasMembers(membershipId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void testETagLdpIndirectContainerChildIsMemberOf() throws Exception {
        final String membershipId = createBasicContainer();
        final String membershipUri = serverAddress + membershipId;

        // Create the IndirectContainer
        final String indirectId = createIndirectContainerIsMemberOf(membershipUri);

        final String proxyId = indirectId + "/proxy1";
        final String proxyUri = serverAddress + proxyId;

        // Create member
        final String memberId = createBasicContainer();
        final String memberUri = serverAddress + memberId;

        final String etag0 = getEtag(memberUri);

        // Create proxy to the member
        createProxy(proxyId, memberUri);

        assertIsMemberOf(memberId, EX_IS_MEMBER_PROP, membershipId);

        final String etag1 = getEtag(memberUri);
        assertNotEquals(etag0, etag1, "ETag didn't change!");

        // Wait a second so that the creation and deletion of the child are not simultaneous
        TimeUnit.SECONDS.sleep(1);

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(proxyUri)),
                "Child resource not deleted!");

        assertHasNoMembership(memberId, EX_IS_MEMBER_PROP);

        final String etag2 = getEtag(memberUri);

        assertNotEquals(etag1, etag2, "ETag didn't change!");
    }

    @Test
    public void testIndirectContainerInsertedContentRelationMemberSubject() throws Exception {
        final String membershipId = createBasicContainer();
        final String membershipUri = serverAddress + membershipId;

        // Create the IndirectContainer
        final String indirectId = createIndirectContainer(membershipUri, PCDM_HAS_MEMBER_PROP,
                MEMBER_SUBJECT.getURI(), false);
        // Create the member resource, not as a proxy
        final String memberId = createBasicContainer(indirectId, "member1");

        assertHasMembers(membershipId, PCDM_HAS_MEMBER_PROP, memberId);
    }

    @Test
    public void testIndirectContainerChildWithoutInsertedContentRelation() throws Exception {
        final String membershipId = createBasicContainer();
        final String membershipUri = serverAddress + membershipId;

        // Create the IndirectContainer
        final String indirectId = createIndirectContainer(membershipUri);
        // Create child resource in indirect container, without the expected insertedContentRelation
        final String memberId = indirectId + "/member1";
        createObjectAndClose(memberId);

        assertHasNoMembership(membershipId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void testIndirectContainerChildInsertedContentRelationLiteral() throws Exception {
        final String membershipId = createBasicContainer();
        final String membershipUri = serverAddress + membershipId;

        // Create the IndirectContainer
        final String indirectId = createIndirectContainer(membershipUri);
        // Create child with insertedContentRelation specifying a literal as the object
        final String proxyId = indirectId + "/proxy1";
        final HttpPut putProxy = putObjMethod(proxyId);
        putProxy.addHeader(CONTENT_TYPE, "text/turtle");
        final String proxyRDF = "<> <" + PCDM_HAS_MEMBER + "> \"helloworld\" .";
        putProxy.setEntity(new StringEntity(proxyRDF));
        assertEquals(Status.CREATED.getStatusCode(), getStatus(putProxy));

        assertHasNoMembership(membershipId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void indirectContainerPopulatesHasMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        // Create proxies to members
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);
        final String proxy2Id = indirectId + "/proxy2";
        createProxy(proxy2Id, member2Uri);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Deleting a member does not impact membership
        executeAndClose(deleteObjMethod(member1Id));
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);

        // Deleting a proxy impacts membership
        executeAndClose(deleteObjMethod(proxy1Id));
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);

        // Deleting the indirect container should remove all membership from it
        executeAndClose(deleteObjMethod(indirectId));
        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void indirectContainerPopulatesIsMemberOf() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainerIsMemberOf(membershipRescURI);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        // Create proxies to members
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);
        final String proxy2Id = indirectId + "/proxy2";
        createProxy(proxy2Id, member2Uri);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
        assertHasNoMembership(membershipRescId, EX_IS_MEMBER_PROP);

        assertIsMemberOf(member1Id, EX_IS_MEMBER_PROP, membershipRescId);
        assertHasNoMembershipWhenOmitted(member1Id, EX_IS_MEMBER_PROP);

        assertIsMemberOf(member2Id, EX_IS_MEMBER_PROP, membershipRescId);
        assertHasNoMembershipWhenOmitted(member2Id, EX_IS_MEMBER_PROP);

        // Deleting a proxy impacts membership
        executeAndClose(deleteObjMethod(proxy1Id));
        assertHasNoMembership(member1Id, EX_IS_MEMBER_PROP);

        assertIsMemberOf(member2Id, EX_IS_MEMBER_PROP, membershipRescId);

        // Deleting the indirect container should remove all membership from it
        executeAndClose(deleteObjMethod(indirectId));
        assertHasNoMembership(member2Id, EX_IS_MEMBER_PROP);
    }

    // Verify that committing delete transactions works after modifying a proxy
    // without actually changing the produced membership triples.
    @Test
    public void indirectContainerDeleteProxyAfterModifying() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirect1Id = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;

        // Create proxies to members
        final String proxy1Id = indirect1Id + "/proxy1";
        createProxy(proxy1Id, member1Uri);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);

        TimeUnit.MILLISECONDS.sleep(1050l);

        // Edit the proxy in a way that doesn't change membership
        setProperty(proxy1Id, DC.title.getURI(), "Proxying");

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);

        executeAndClose(deleteObjMethod(proxy1Id));

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void indirectContainerDeleteInTx() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;

        // Create proxies to members
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final var txUri = createTransaction();

        final HttpDelete deleteMethod = deleteObjMethod(indirectId);
        deleteMethod.addHeader(ATOMIC_ID_HEADER, txUri);
        executeAndClose(deleteMethod);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        assertHasMembers(txUri, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP);

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void indirectContainerModifyProxyAutoVersioning() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirect1Id = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        TimeUnit.MILLISECONDS.sleep(1000);

        // Create proxy to member1
        final String proxy1Id = indirect1Id + "/proxy1";
        final String proxy1Uri = serverAddress + proxy1Id;
        createProxy(proxy1Id, member1Uri);
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);

        // Memento preceeds addition of proxy, so no membership
        final var mementos = listMementoIds(membershipRescId);
        assertEquals(1, mementos.size());
        assertMementoHasMembers(mementos.get(0), PCDM_HAS_MEMBER_PROP);

        changeProxyMember(proxy1Uri, member1Uri, member2Uri, null);
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);

        // History sohuld be unchanged since the membership resource hasn't been versioned
        final var mementos2 = listMementoIds(membershipRescId);
        assertEquals(1, mementos2.size());
        assertMementoHasMembers(mementos2.get(0), PCDM_HAS_MEMBER_PROP);

        TimeUnit.MILLISECONDS.sleep(1000);

        // Change proxy inside of a transaction
        final var txUri = createTransaction();
        final String member3Id = createBasicContainer(null, getRandomUniqueId(), txUri);
        final String member3Uri = serverAddress + member3Id;
        changeProxyMember(proxy1Uri, member2Uri, member3Uri, txUri);

        // Unchanged outside of tx, changed in tx
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);
        assertHasMembers(txUri, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP, member3Id);

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member3Id);

        final var mementos3 = listMementoIds(membershipRescId);
        assertEquals(1, mementos3.size());
        assertMementoHasMembers(mementos3.get(0), PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void indirectContainerModifyProxyOnDemandVersioning() throws Exception {
        objectSessionFactory.setDefaultCommitType(CommitType.UNVERSIONED);
        ocflPropsConfig.setAutoVersioningEnabled(false);

        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirect1Id = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        // Create proxy to member1
        final String proxy1Id = indirect1Id + "/proxy1";
        final String proxy1Uri = serverAddress + proxy1Id;
        createProxy(proxy1Id, member1Uri);
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);

        changeProxyMember(proxy1Uri, member1Uri, member2Uri, null);
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);

        TimeUnit.MILLISECONDS.sleep(1000);

        createMemento(membershipRescURI);
        final var mementos = listMementoIds(membershipRescId);
        assertEquals(1, mementos.size());
        assertMementoHasMembers(mementos.get(0), PCDM_HAS_MEMBER_PROP, member2Id);

        // Change proxy target again, should rewrite history of membership resc
        changeProxyMember(proxy1Uri, member2Uri, member1Uri, null);
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        final var mementos2 = listMementoIds(membershipRescId);
        assertEquals(1, mementos2.size());
        awaitAssertMementoHasMembers(mementos2.get(0), PCDM_HAS_MEMBER_PROP, member1Id);

        TimeUnit.MILLISECONDS.sleep(1000);

        // Create memento of the proxy, the membership state of the memento should be immutable now
        createMemento(proxy1Uri);
        changeProxyMember(proxy1Uri, member1Uri, member2Uri, null);
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);
        final var mementos3 = listMementoIds(membershipRescId);
        assertEquals(1, mementos3.size());
        awaitAssertMementoHasMembers(mementos3.get(0), PCDM_HAS_MEMBER_PROP, member1Id);

        final var txUri = createTransaction();
        final String member3Id = createBasicContainer(null, getRandomUniqueId(), txUri);
        final String member3Uri = serverAddress + member3Id;
        changeProxyMember(proxy1Uri, member2Uri, member3Uri, txUri);

        // Unchanged outside of tx, changed in tx
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);
        assertHasMembers(txUri, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP, member3Id);

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member3Id);
    }

    private void changeProxyMember(final String proxyUri, final String oldMember, final String newMember,
            final String txUri) throws Exception {
        final HttpPatch patch = new HttpPatch(proxyUri);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        if (txUri != null) {
            patch.addHeader(ATOMIC_ID_HEADER, txUri);
        }
        patch.setEntity(new StringEntity(
                "DELETE { <> <" + PROXY_FOR + "> <" + oldMember + "> . }" +
                "INSERT { <> <" + PROXY_FOR + "> <" + newMember + "> } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void indirectContainerHasMemberMembershipHistory() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String initialEtag = getEtag(membershipRescURI);

        // First version will have no membership
        TimeUnit.MILLISECONDS.sleep(1500);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        // Create proxies to members
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);
        final String proxy2Id = indirectId + "/proxy2";
        createProxy(proxy2Id, member2Uri);

        final String afterPropEtag1 = getEtag(membershipRescURI);
        assertNotEquals(initialEtag, afterPropEtag1, "Etag must change after additions");

        // Trigger an update of the membership resource to create version with two members
        setProperty(membershipRescId, DC.subject.getURI(), "Updated");

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String afterPropEtag2 = getEtag(membershipRescURI);
        assertNotEquals(afterPropEtag1, afterPropEtag2, "Etag must change after modification");

        TimeUnit.MILLISECONDS.sleep(1500);

        // Change the membership relation
        final String indirectURI = serverAddress + indirectId;
        final HttpPatch patch = new HttpPatch(indirectURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> . }" +
                "INSERT { <> ldp:hasMemberRelation ldp:member } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, RdfLexicon.LDP_MEMBER);

        final String afterRelChangeEtag = getEtag(membershipRescURI);
        assertNotEquals(afterPropEtag2, afterRelChangeEtag, "Etag must change after relation changes");

        // Update membership resc to create version where the membership rel has changed
        setProperty(membershipRescId, DC.subject.getURI(), "Updated again");

        final String afterRelChangeEtag2 = getEtag(membershipRescURI);

        TimeUnit.MILLISECONDS.sleep(1500);

        // Delete a proxy, which will disappear from the head version's membership
        executeAndClose(deleteObjMethod(proxy1Id));

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String afterDeleteEtag = getEtag(membershipRescURI);
        assertNotEquals(afterRelChangeEtag2, afterDeleteEtag, "Etag must change after member delete");

        final var mementos = listMementoIds(membershipRescId);
        assertEquals(3, mementos.size());

        // verify membership at each of the mementos of the membership resource
        assertMementoHasNoMembership(mementos.get(0), PCDM_HAS_MEMBER_PROP);
        assertEquals(initialEtag, getEtag(serverAddress + mementos.get(0)));

        assertMementoHasMembers(mementos.get(1), PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertEquals(afterPropEtag2, getEtag(serverAddress + mementos.get(1)));

        assertMementoHasMembers(mementos.get(2), RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertEquals(afterRelChangeEtag2, getEtag(serverAddress + mementos.get(2)));

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
    }

    @Test
    public void indirectContainerIsMemberOfMembershipHistory() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainerIsMemberOf(membershipRescURI);

        // Get IDs the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        // Second member will be updated to create an extra version of it
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;
        // Create one member after creating proxy for it
        final String member3Id = getRandomUniqueId();
        final String member3Uri = serverAddress + member3Id;

        TimeUnit.SECONDS.sleep(1);

        // Create proxies to members
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);
        final String proxy2Id = indirectId + "/proxy2";
        createProxy(proxy2Id, member2Uri);
        final String proxy3Id = indirectId + "/proxy3";
        createProxy(proxy3Id, member3Uri);

        TimeUnit.SECONDS.sleep(1);

        // Create member3 after its proxy
        createBasicContainer(null, member3Id);

        TimeUnit.MILLISECONDS.sleep(1500);

        // Change the membership relation
        final String directURI = serverAddress + indirectId;
        final HttpPatch patch = new HttpPatch(directURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:isMemberOfRelation <" + EX_IS_MEMBER_PROP + "> . }" +
                "INSERT { <> ldp:isMemberOfRelation ldp:member } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        setProperty(member2Id, DC.subject.getURI(), "Updated");

        final var member1Mementos = listMementoIds(member1Id);
        assertEquals(1, member1Mementos.size());
        assertHasNoMembership(member1Mementos.get(0), EX_IS_MEMBER_PROP);
        assertHasNoMembership(member1Id, EX_IS_MEMBER_PROP);
        assertIsMemberOf(member1Id, RdfLexicon.LDP_MEMBER, membershipRescId);

        final var member2Mementos = listMementoIds(member2Id);
        assertEquals(2, member2Mementos.size());
        await().atMost(2, TimeUnit.SECONDS).until(() -> {
            assertHasNoMembership(member2Mementos.get(0), EX_IS_MEMBER_PROP);
            assertMementoIsMemberOf(member2Mementos.get(1), RdfLexicon.LDP_MEMBER, membershipRescId);
            return true;
        });

        assertIsMemberOf(member2Id, RdfLexicon.LDP_MEMBER, membershipRescId);

        final var member3Mementos = listMementoIds(member3Id);
        assertEquals(1, member3Mementos.size());
        assertMementoIsMemberOf(member3Mementos.get(0), EX_IS_MEMBER_PROP, membershipRescId);
        assertHasNoMembership(member3Id, EX_IS_MEMBER_PROP);
        assertIsMemberOf(member1Id, RdfLexicon.LDP_MEMBER, membershipRescId);
    }

    @Test
    public void indirectContainerHasMemberWithTransaction() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final var member1Id = createBasicContainer(null, "member1");
        final String member1Uri = serverAddress + member1Id;
        final var member2Id = createBasicContainer(null, "member2");
        final String member2Uri = serverAddress + member2Id;
        final var member3Id = createBasicContainer(null, "member3");
        final String member3Uri = serverAddress + member3Id;

        // Create one proxy outside of the TX
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);

        // Create second proxy in TX
        final var txUri = createTransaction();
        final String proxy2Id = indirectId + "/proxy2";
        createProxy(proxy2Id, member2Uri, PROXY_FOR.getURI(), txUri);

        // Outside the tx, only 1 member should be visible
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        // In the tx, both should be
        assertHasMembers(txUri, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP,
                member1Id, member2Id);

        // commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));
        // After committing, both members should be present
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);

        // Create another tx in order to add a member then roll it back
        final var txUri2 = createTransaction();

        final String proxy3Id = indirectId + "/proxy3";
        createProxy(proxy3Id, member3Uri, PROXY_FOR.getURI(), txUri2);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasMembers(txUri2, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP,
                member1Id, member2Id, member3Id);

        // rollback
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(txUri2)));

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertEquals(GONE.getStatusCode(), getStatus(new HttpGet(txUri2)),
                "Rolled back transaction should be gone");
    }

    @Test
    public void testEtagForIndirectContainerHasMemberPatch() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final String indirectId = createIndirectContainer(membershipRescURI);

        // Capture baseline etag before any membership is added
        final String initialEtag = getEtag(membershipRescURI);

        final var member1Id = createBasicContainer(null, "member1");
        final String member1Uri = serverAddress + member1Id;
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);

        final String committedMembershipEtag = getEtag(membershipRescURI);
        assertNotEquals(initialEtag, committedMembershipEtag, "Committed etag must not match original etag");

        // Update the membership resource using the etags
        addTitleWithEtag(membershipRescURI, "title1", deweakify(initialEtag), Status.PRECONDITION_FAILED);
        addTitleWithEtag(membershipRescURI, "title2", deweakify(committedMembershipEtag), Status.NO_CONTENT);

        assertNotEquals(committedMembershipEtag, getEtag(membershipRescURI), "Etag must update after modification");
    }

    @Test
    public void indirectContainerModifyProxyReferencedMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;
        final String member2Id = createBasicContainer();
        final String member2Uri = serverAddress + member2Id;

        // Create proxies to member1
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Change the proxy to reference member2 instead
        final HttpPatch patch = patchObjMethod(proxy1Id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "DELETE { <> <" + PROXY_FOR + "> ?existing . }" +
                " INSERT { <> <" + PROXY_FOR + "> <" + member2Uri + "> }" +
                " WHERE { <> <" + PROXY_FOR + "> ?existing}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);
    }

    @Test
    public void indirectContainerRemoveProxyReferencedMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;

        // Create proxies to member1
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Change the proxy to remove the member reference
        final HttpPatch patch = patchObjMethod(proxy1Id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "DELETE { <> <" + PROXY_FOR + "> ?existing . }" +
                " WHERE { <> <" + PROXY_FOR + "> ?existing}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void indirectContainerMultipleProxyForSameMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var indirectId = createIndirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Create the members
        final String member1Id = createBasicContainer();
        final String member1Uri = serverAddress + member1Id;

        // Create proxies to member1
        final String proxy1Id = indirectId + "/proxy1";
        createProxy(proxy1Id, member1Uri);
        final String proxy2Id = indirectId + "/proxy2";
        createProxy(proxy2Id, member1Uri);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Delete 1 proxy, membership should remain
        executeAndClose(deleteObjMethod(proxy1Id));
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);

        // Delete both proxies, membership should be gone
        executeAndClose(deleteObjMethod(proxy2Id));
        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
    }

    private String createIndirectContainer(final String membershipURI) throws Exception {
        return createIndirectContainer(membershipURI, PCDM_HAS_MEMBER_PROP, PROXY_FOR.getURI(), false);
    }

    private String createIndirectContainerIsMemberOf(final String membershipURI) throws Exception {
        return createIndirectContainer(membershipURI, EX_IS_MEMBER_PROP, PROXY_FOR.getURI(), true);
    }

    private String createIndirectContainer(final String membershipURI, final Property hasMemberRelation,
            final String insertedContentRelation, final boolean isMemberOf) throws Exception {
        final String indirectId = getRandomUniqueId();
        return createIndirectContainer(indirectId, membershipURI, hasMemberRelation,
                insertedContentRelation, isMemberOf);
    }

    private String createIndirectContainer(final String indirectId, final String membershipURI,
            final Property hasMemberRelation, final String insertedContentRelation, final boolean isMemberOf)
                    throws Exception {
        final HttpPut putIndirect = putObjMethod(indirectId);
        putIndirect.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        putIndirect.addHeader(CONTENT_TYPE, "text/turtle");
        final String memberRelation = isMemberOf ? "ldp:isMemberOfRelation" : "ldp:hasMemberRelation";
        final String membersRDF = "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "<> " + memberRelation + " <" + hasMemberRelation + "> ; " +
                "ldp:insertedContentRelation <" + insertedContentRelation + ">; " +
                "ldp:membershipResource <" + membershipURI + "> . ";
        putIndirect.setEntity(new StringEntity(membersRDF));
        assertEquals(Status.CREATED.getStatusCode(), getStatus(putIndirect));

        return indirectId;
    }

    private void createProxy(final String proxyId, final String memberURI) throws Exception {
        createProxy(proxyId, memberURI, PROXY_FOR.getURI(), null);
    }

    private void createProxy(final String proxyId, final String memberURI, final String insertedContentRelation,
            final String txUri) throws Exception {
        final HttpPut putProxy = putObjMethod(proxyId);
        putProxy.addHeader(CONTENT_TYPE, "text/turtle");
        final String proxyRDF = "<> <" + insertedContentRelation + "> <" + memberURI + "> .";
        putProxy.setEntity(new StringEntity(proxyRDF));
        if (txUri != null) {
            putProxy.addHeader(ATOMIC_ID_HEADER, txUri);
        }
        assertEquals(Status.CREATED.getStatusCode(), getStatus(putProxy));
    }

    @Test
    public void testDirectContainerDefaults() throws Exception {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final Model model = getModel(id);

        final Resource resc = model.getResource(subjectURI);
        assertTrue(resc.hasProperty(RDF.type, DIRECT_CONTAINER), "Must have container type");

        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must be set");
        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must be set");
    }

    @Test
    public void testDirectContainerOverrides() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final var directId = createDirectContainer(parentURI);
        final String directURI = serverAddress + directId;

        final Model model = getModel(directId);
        final Resource resc = model.getResource(directURI);
        assertTrue(resc.hasProperty(RDF.type, DIRECT_CONTAINER), "Must have container type");

        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)),
                "Provided ldp:membershipResource must be present");
        assertFalse(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must not be present");

        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, PCDM_HAS_MEMBER_PROP),
                "Provided ldp:hasMemberRelation must be set");
        assertFalse(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must not be present");
    }

    @Test
    public void testDirectContainerDefaultsAfterPUT() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final var directId = createDirectContainer(parentURI);
        final String directURI = serverAddress + directId;

        final Model replaceModel = getModel(directId);
        replaceModel.removeAll(null, MEMBERSHIP_RESOURCE, null);
        replaceModel.removeAll(null, HAS_MEMBER_RELATION, null);

        replacePropertiesWithPUT(directURI, replaceModel);

        final Model model = getModel(directId);
        final Resource resc = model.getResource(directURI);
        assertTrue(resc.hasProperty(RDF.type, DIRECT_CONTAINER), "Must have container type");

        assertFalse(resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)),
                "Provided ldp:membershipResource must be removed");
        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must be set");

        assertFalse(resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)),
                "Provided ldp:hasMemberRelation must be removed");
        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must be set");
    }

    @Test
    public void testDirectContainerDefaultsAfterPatch() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final var directId = createDirectContainer(parentURI);
        final String directURI = serverAddress + directId;

        final HttpPatch patch = new HttpPatch(directURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:membershipResource <" + parentURI + "> ;\n" +
                "ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> . } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model = getModel(directId);
        final Resource resc = model.getResource(directURI);
        assertTrue(resc.hasProperty(RDF.type, DIRECT_CONTAINER), "Must have container type");

        assertFalse(resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)),
                "Provided ldp:membershipResource must be removed");
        assertTrue(resc.hasProperty(MEMBERSHIP_RESOURCE, resc),
                "Default ldp:membershipResource must be set");

        assertFalse(resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)),
                "Provided ldp:hasMemberRelation must be removed");
        assertTrue(resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER),
                "Default ldp:hasMemberRelation must be set");
    }

    @Test
    public void testDirectContainerPatchWithLdpContainsHasMember() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation ?memRel . }\n" +
                "INSERT { <> ldp:hasMemberRelation <" + RdfLexicon.CONTAINS + "> }\n" +
                " WHERE { <> ldp:hasMemberRelation ?memRel}"));
        assertEquals(CONFLICT.getStatusCode(), getStatus(patch),
                "Patch with sparql update allowed ldp:contains in direct container!");
    }

    @Test
    public void testDirectContainerPatchWithLdpContainsIsMember() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation ?memRel . }\n" +
                "INSERT { <> ldp:isMemberOfRelation <" + RdfLexicon.CONTAINS + "> }\n" +
                " WHERE { <> ldp:hasMemberRelation ?memRel}"));
        assertEquals(CONFLICT.getStatusCode(), getStatus(patch),
                "Patch with sparql update allowed ldp:contains in direct container!");
    }

    @Test
    public void testDirectContainerPatchWithoutLdpContains() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation ?memRel . }\n" +
                "INSERT { <> ldp:hasMemberRelation <info:some/relation> }\n" +
                " WHERE { <> ldp:hasMemberRelation ?memRel}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testDirectContainerInvalidMembershipRelationProperties() throws Exception {
        final String membershipResc1Id = createBasicContainer();
        final String membershipResc1URI = serverAddress + membershipResc1Id;

        final var directId = createDirectContainer(membershipResc1URI);
        final String directURI = serverAddress + directId;

        // Change the membership relation via PATCH
        final HttpPatch patch = new HttpPatch(directURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "INSERT { <> ldp:hasMemberRelation ldp:member } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }

        // Try setting two via PUT
        final Model replaceModel = ModelFactory.createDefaultModel();
        replaceModel.add(createResource(directURI), RdfLexicon.HAS_MEMBER_RELATION, RdfLexicon.LDP_MEMBER);
        replaceModel.add(createResource(directURI), RdfLexicon.HAS_MEMBER_RELATION, PCDM_HAS_MEMBER_PROP);
        replacePropertiesWithPUT(directURI, replaceModel, BAD_REQUEST);

        // Try setting both ldp:hasMemberOf and ldp:isMemberOf
        final Model replaceModel2 = ModelFactory.createDefaultModel();
        replaceModel2.add(createResource(directURI), RdfLexicon.HAS_MEMBER_RELATION, RdfLexicon.LDP_MEMBER);
        replaceModel2.add(createResource(directURI), RdfLexicon.IS_MEMBER_OF_RELATION, PCDM_HAS_MEMBER_PROP);
        replacePropertiesWithPUT(directURI, replaceModel2, BAD_REQUEST);

        // Set an invalid object value
        final Model replaceModel3 = ModelFactory.createDefaultModel();
        replaceModel3.add(createResource(directURI), RdfLexicon.HAS_MEMBER_RELATION, "what");
        replacePropertiesWithPUT(directURI, replaceModel3, BAD_REQUEST);
    }

    @Test
    public void testDirectContainerInvalidMembershipResourceProperties() throws Exception {
        final String membershipResc1Id = createBasicContainer();
        final String membershipResc1URI = serverAddress + membershipResc1Id;

        final String membershipResc2Id = createBasicContainer();
        final String membershipResc2URI = serverAddress + membershipResc2Id;

        final var directId = createDirectContainer(membershipResc1URI);
        final String directURI = serverAddress + directId;

        // Change the membership relation
        final HttpPatch patch = new HttpPatch(directURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "INSERT { <> ldp:membershipResource <" + membershipResc2URI + "> } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }

        // Try setting two via PUT
        final Model replaceModel = ModelFactory.createDefaultModel();
        replaceModel.add(createResource(directURI), RdfLexicon.MEMBERSHIP_RESOURCE,
                createResource(membershipResc1URI));
        replaceModel.add(createResource(directURI), RdfLexicon.MEMBERSHIP_RESOURCE,
                createResource(directURI));
        replacePropertiesWithPUT(directURI, replaceModel, BAD_REQUEST);

        // Set object to non-resource
        final Model replaceModel2 = ModelFactory.createDefaultModel();
        replaceModel2.add(createResource(directURI), RdfLexicon.MEMBERSHIP_RESOURCE, "ohno");
        replacePropertiesWithPUT(directURI, replaceModel2, BAD_REQUEST);
    }

    @Test
    public void directContainerPopulatesHasMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Add some members
        final var member1Id = createBasicContainer(directId, "member1");
        final var member2Id = createBasicContainer(directId, "member2");

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        executeAndClose(deleteObjMethod(member1Id));

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member2Id);

        executeAndClose(deleteObjMethod(directId));

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);
    }

    @Test
    public void directContainerPopulatesIsMemberOf() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI, EX_IS_MEMBER_PROP, true);

        assertHasNoMembership(membershipRescId, EX_IS_MEMBER_PROP);
        assertHasNoMembership(directId, EX_IS_MEMBER_PROP);

        // Add some members
        final var member1Id = createBasicContainer(directId, "member1");
        final var member2Id = createBasicContainer(directId, "member2");

        assertHasNoMembership(membershipRescId, EX_IS_MEMBER_PROP);
        assertIsMemberOf(member1Id, EX_IS_MEMBER_PROP, membershipRescId);
        assertIsMemberOf(member2Id, EX_IS_MEMBER_PROP, membershipRescId);
        assertHasNoMembershipWhenOmitted(member1Id, EX_IS_MEMBER_PROP);
        assertHasNoMembershipWhenOmitted(member2Id, EX_IS_MEMBER_PROP);
    }

    @Test
    public void directContainerHasMemberMembershipHistory() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String initialEtag = getEtag(membershipRescURI);

        // First version will have no membership
        TimeUnit.MILLISECONDS.sleep(1500);

        final var member1Id = createBasicContainer(directId, "member1");
        final var member2Id = createBasicContainer(directId, "member2");

        final String afterPropEtag1 = getEtag(membershipRescURI);
        assertNotEquals(initialEtag, afterPropEtag1, "Etag must change after additions");

        // Trigger an update of the membership resource to create version with two members
        setProperty(membershipRescId, DC.subject.getURI(), "Updated");

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String afterPropEtag2 = getEtag(membershipRescURI);
        assertNotEquals(afterPropEtag1, afterPropEtag2, "Etag must change after modification");

        TimeUnit.MILLISECONDS.sleep(1500);

        // Change the membership relation
        final String directURI = serverAddress + directId;
        final HttpPatch patch = new HttpPatch(directURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> . }" +
                "INSERT { <> ldp:hasMemberRelation ldp:member } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, RdfLexicon.LDP_MEMBER);

        final String afterRelChangeEtag = getEtag(membershipRescURI);
        assertNotEquals(afterPropEtag2, afterRelChangeEtag, "Etag must change after relation changes");

        // Update membership resc to create version where the membership rel has changed
        setProperty(membershipRescId, DC.subject.getURI(), "Updated again");

        final String afterRelChangeEtag2 = getEtag(membershipRescURI);

        TimeUnit.MILLISECONDS.sleep(1500);

        // Delete a member, which will disappear from the head version's membership
        executeAndClose(deleteObjMethod(member1Id));

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String afterDeleteEtag = getEtag(membershipRescURI);
        assertNotEquals(afterRelChangeEtag2, afterDeleteEtag, "Etag must change after member delete");

        final var mementos = listMementoIds(membershipRescId);
        assertEquals(3, mementos.size());

        // verify membership at each of the mementos of the membership resource
        assertMementoHasNoMembership(mementos.get(0), PCDM_HAS_MEMBER_PROP);
        assertEquals(initialEtag, getEtag(serverAddress + mementos.get(0)));

        assertMementoHasMembers(mementos.get(1), PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertEquals(afterPropEtag2, getEtag(serverAddress + mementos.get(1)));

        assertMementoHasMembers(mementos.get(2), RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertEquals(afterRelChangeEtag2, getEtag(serverAddress + mementos.get(2)));

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
    }

    @Test
    public void directContaineIsMemberOfMembershipHistory() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI, EX_IS_MEMBER_PROP, true);

        // Add some members
        final var member1Id = createBasicContainer(directId, "member1");
        final var member2Id = createBasicContainer(directId, "member2");

        TimeUnit.MILLISECONDS.sleep(1500);

        // Change the membership relation
        final String directURI = serverAddress + directId;
        final HttpPatch patch = new HttpPatch(directURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:isMemberOfRelation <" + EX_IS_MEMBER_PROP + "> . }" +
                "INSERT { <> ldp:isMemberOfRelation ldp:member } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        setProperty(member2Id, DC.subject.getURI(), "Updated");

        final var member1Mementos = listMementoIds(member1Id);
        assertEquals(1, member1Mementos.size());
        assertMementoIsMemberOf(member1Mementos.get(0), EX_IS_MEMBER_PROP, membershipRescId);
        assertIsMemberOf(member1Id, RdfLexicon.LDP_MEMBER, membershipRescId);
        assertHasNoMembership(member1Id, EX_IS_MEMBER_PROP);

        final var member2Mementos = listMementoIds(member2Id);
        assertEquals(2, member2Mementos.size());
        assertMementoIsMemberOf(member2Mementos.get(0), EX_IS_MEMBER_PROP, membershipRescId);
        assertMementoIsMemberOf(member2Mementos.get(1), RdfLexicon.LDP_MEMBER, membershipRescId);
        assertIsMemberOf(member2Id, RdfLexicon.LDP_MEMBER, membershipRescId);
        assertHasNoMembership(member2Id, EX_IS_MEMBER_PROP);
    }

    @Test
    public void directContainerHasMemberWithTransaction() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Add some members
        final var member1Id = createBasicContainer(directId, "member1");

        final var txUri = createTransaction();

        final var member2Id = createBasicContainer(directId, "member2", txUri);

        // Outside the tx, only 1 member should be visible
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id);
        // In the tx, both should be
        assertHasMembers(txUri, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP,
                member1Id, member2Id);

        // commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));
        // After committing, both members should be present
        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);

        // Create another tx in order to add a member then roll it back
        final var txUri2 = createTransaction();

        final var member3Id = createBasicContainer(directId, "member3", txUri2);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasMembers(txUri2, membershipRescId, membershipRescId, PCDM_HAS_MEMBER_PROP,
                member1Id, member2Id, member3Id);

        // rollback
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(txUri2)));

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertEquals(GONE.getStatusCode(), getStatus(new HttpGet(txUri2)),
                "Rolled back transaction should be gone");
    }

    @Test
    public void directContainerChangeDCOnDemandVersioning() throws Exception {
        objectSessionFactory.setDefaultCommitType(CommitType.UNVERSIONED);
        ocflPropsConfig.setAutoVersioningEnabled(false);

        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        assertHasNoMembership(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String initialEtag = getEtag(membershipRescURI);

        final var member1Id = createBasicContainer(directId, "member1");
        final var member2Id = createBasicContainer(directId, "member2");

        final String afterPropEtag1 = getEtag(membershipRescURI);
        assertNotEquals("Etag must change after additions", initialEtag, afterPropEtag1);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        // Change the membership relation
        final String directURI = serverAddress + directId;
        changeMembershipRelation(directURI, PCDM_HAS_MEMBER_PROP, RdfLexicon.LDP_MEMBER, null);

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, RdfLexicon.LDP_MEMBER);

        // Version the membership resc
        createMemento(membershipRescURI);

        TimeUnit.MILLISECONDS.sleep(1000);

        // Verify membership in the created memento
        final var mementos = listMementoIds(membershipRescId);
        assertEquals(1, mementos.size());
        assertMementoHasMembers(mementos.get(0), RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Change membership relation, should rewrite history of membership resc
        changeMembershipRelation(directURI, RdfLexicon.LDP_MEMBER, PCDM_HAS_MEMBER_PROP, null);

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        final var mementos2 = listMementoIds(membershipRescId);
        assertEquals(1, mementos2.size());
        assertMementoHasMembers(mementos2.get(0), PCDM_HAS_MEMBER_PROP, member1Id, member2Id);

        // Version the DC and change property again, should preserve membership in memento
        createMemento(directURI);
        changeMembershipRelation(directURI, PCDM_HAS_MEMBER_PROP, RdfLexicon.LDP_MEMBER, null);

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        final var mementos3 = listMementoIds(membershipRescId);
        assertEquals(1, mementos3.size());
        assertMementoHasMembers(mementos3.get(0), PCDM_HAS_MEMBER_PROP, member1Id, member2Id);

        // Modify within a transaction
        final var txUri = createTransaction();
        final Property otherMembership = createProperty("http://example.com/member");
        changeMembershipRelation(directURI, RdfLexicon.LDP_MEMBER, otherMembership, txUri);

        // Unchanged outside of tx, changed in tx
        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasMembers(txUri, membershipRescId, membershipRescId, otherMembership, member1Id, member2Id);

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));
        assertHasMembers(membershipRescId, otherMembership, member1Id, member2Id);
    }

    private void changeMembershipRelation(final String containerUri, final Property oldRel,
            final Property newRel, final String txUri) throws Exception {
        final HttpPatch patch = new HttpPatch(containerUri);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        if (txUri != null) {
            patch.addHeader(ATOMIC_ID_HEADER, txUri);
        }
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:hasMemberRelation <" + oldRel + "> . }" +
                "INSERT { <> ldp:hasMemberRelation <" + newRel + "> } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }
    }

    private String createMemento(final String subjectUri) throws Exception {
        final HttpPost createVersionMethod = new HttpPost(subjectUri + "/" + FCR_VERSIONS);

        // Create new memento of resource with updated body
        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response), "Didn't get a CREATED response!");
            return response.getFirstHeader(LOCATION).getValue();
        }
    }

    @Test
    public void testEtagForDirectContainerHasMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        // Capture baseline etag before any membership is added
        final String initialEtag = getEtag(membershipRescURI);
        final String initialGetEtag = getEtag(new HttpGet(membershipRescURI));
        assertEquals(initialEtag, initialGetEtag, "HEAD and basic GET should produce same etag");

        // Add member in transaction
        final var txUri = createTransaction();

        createBasicContainer(directId, "member1", txUri);

        final String txMembershipEtag = getEtag(addTxTo(new HttpHead(membershipRescURI), txUri));

        assertEquals(initialEtag, getEtag(membershipRescURI), "Etag outside of tx must be unchanged");
        assertNotEquals(initialEtag, txMembershipEtag, "Etag within the tx must have changed");

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));

        final String committedMembershipEtag = getEtag(membershipRescURI);
        assertEquals(txMembershipEtag, committedMembershipEtag, "Committed etag must match pre-commit etag");
        assertNotEquals(initialEtag, committedMembershipEtag, "Committed etag must not match original etag");

        assertEquals(committedMembershipEtag, getEtag(new HttpGet(membershipRescURI)),
                "Committed GET and HEAD etags must match");

        // Verify that etag of membership resource is the same when excluding membership as before there was any
        final String excludeMembershipEtag = getEtag(getOmitMembership(membershipRescURI));
        assertEquals(initialEtag, excludeMembershipEtag,
                "Etag without membership should match initial etag");
    }

    @Test
    public void testEtagForDirectContainerHasMemberPatch() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        // Capture baseline etag before any membership is added
        final String initialEtag = getEtag(membershipRescURI);

        createBasicContainer(directId, "member1");

        final String committedMembershipEtag = getEtag(membershipRescURI);
        assertNotEquals(initialEtag, committedMembershipEtag,
                "Committed etag must not match original etag");

        // Update the membership resource using the etags
        addTitleWithEtag(membershipRescURI, "title1", deweakify(initialEtag), Status.PRECONDITION_FAILED);
        addTitleWithEtag(membershipRescURI, "title2", deweakify(committedMembershipEtag), Status.NO_CONTENT);

        assertNotEquals(committedMembershipEtag, getEtag(membershipRescURI),
                "Etag must update after modification");
    }

    @Test
    public void testEtagForDirectContainerHasMemberPut() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        // Capture baseline etag before any membership is added
        final String initialEtag = getEtag(membershipRescURI);

        createBasicContainer(directId, "member1");

        final String committedMembershipEtag = getEtag(membershipRescURI);
        assertNotEquals(initialEtag, committedMembershipEtag, "Committed etag must not match original etag");

        // Update the membership resource using the etags
        putPropertiesWithEtag(membershipRescURI, deweakify(initialEtag), Status.PRECONDITION_FAILED);
        putPropertiesWithEtag(membershipRescURI, deweakify(committedMembershipEtag), Status.NO_CONTENT);

        assertNotEquals(committedMembershipEtag, getEtag(membershipRescURI),
                "Etag must update after modification");
    }

    @Test
    public void testEtagForDirectContainerIsMemberOf() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI, EX_IS_MEMBER_PROP, true);

        // Capture starting etag for membership resource
        final String membershipEtag = getEtag(membershipRescURI);
        assertNotNull(membershipEtag);

        // Add member in transaction
        final var txUri = createTransaction();

        final var memberId = createBasicContainer(directId, "member1", txUri);
        final var memberUri = serverAddress + memberId;

        final String txMemberEtag = getEtag(addTxTo(new HttpHead(memberUri), txUri));
        assertNotNull(txMemberEtag, "Member etag must not be null in tx");

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));

        final String committedMemberEtag = getEtag(memberUri);
        assertEquals(txMemberEtag, committedMemberEtag, "Committed etag must match pre-commit etag");

        assertEquals(membershipEtag, getEtag(membershipRescURI), "Membership resc etag must not change");

        // Verify etag varies appropriately when excluding membership
        final String excludeMemberEtag = getEtag(getOmitMembership(memberUri));
        assertNotEquals(committedMemberEtag, excludeMemberEtag,
                "Etag for member must change when excluding membership");

        final String excludeMembershipEtag = getEtag(getOmitMembership(membershipRescURI));
        assertEquals(membershipEtag, excludeMembershipEtag,
                "Etag for membership resc should not change when excluding membership");

        // Update the member resource using the etags
        addTitleWithEtag(memberUri, "title1", deweakify(excludeMemberEtag), Status.PRECONDITION_FAILED);
        addTitleWithEtag(memberUri, "title2", deweakify(committedMemberEtag), Status.NO_CONTENT);
    }

    @Test
    public void testEtagForDirectContainerIsMemberOfPatch() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI, EX_IS_MEMBER_PROP, true);

        final var memberId = createBasicContainer(directId, "member1");
        final var memberUri = serverAddress + memberId;

        final String committedMemberEtag = getEtag(memberUri);
        assertNotNull(committedMemberEtag, "Member resource must have etag");

        // Update the member resource using the etags
        addTitleWithEtag(memberUri, "title1", deweakify("W/\"fake\""), Status.PRECONDITION_FAILED);
        addTitleWithEtag(memberUri, "title2", deweakify(committedMemberEtag), Status.NO_CONTENT);

        assertNotEquals(committedMemberEtag, getEtag(membershipRescURI), "Etag must update after modification");
    }

    @Test
    public void testEtagForDirectContainerIsMemberOfPut() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI, EX_IS_MEMBER_PROP, true);

        final var memberId = createBasicContainer(directId, "member1");
        final var memberUri = serverAddress + memberId;

        final String committedMemberEtag = getEtag(memberUri);
        assertNotNull(committedMemberEtag, "Member resource must have etag");

        putPropertiesWithEtag(memberUri, deweakify("W/\"fake\""), Status.PRECONDITION_FAILED);
        putPropertiesWithEtag(memberUri, deweakify(committedMemberEtag), Status.NO_CONTENT);

        assertNotEquals(committedMemberEtag, getEtag(membershipRescURI), "Etag must update after modification");
    }

    @Test
    public void testETagOnDeletedLdpDirectContainerChild() throws Exception {
        final String membershipRescId = getRandomUniqueId();
        final String members = membershipRescId + "/members";
        final String child = members + "/child";

        createObjectAndClose(membershipRescId);

        final String etag0 = getEtag(serverAddress + membershipRescId);

        // Create the DirectContainer
        final HttpPut createContainer = new HttpPut(serverAddress + members);
        createContainer.addHeader(CONTENT_TYPE, "text/turtle");
        createContainer.addHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        final String membersRDF = "<> <http://www.w3.org/ns/ldp#hasMemberRelation> <http://pcdm.org/models#hasMember>;"
            + " <http://www.w3.org/ns/ldp#membershipResource> <" + serverAddress + membershipRescId + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        assertEquals(CREATED.getStatusCode(), getStatus(createContainer), "Membership container not created!");

        // Create the child resource
        createObjectAndClose(child);

        final String etag1 = getEtag(serverAddress + membershipRescId);
        assertNotEquals(etag0, etag1, "Adding child must change etag of membership resc");

        // Wait a second so that the creation and deletion of the child are not simultaneous
        TimeUnit.SECONDS.sleep(1);

        // Delete the child resource
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + child)),
                "Child resource not deleted!");

        final String etag2 = getEtag(serverAddress + membershipRescId);

        assertNotEquals(etag1, etag2, "ETag didn't change!");
    }

    private void putPropertiesWithEtag(final String rescUri, final String etag, final Status status)
            throws IOException {
        final HttpPut replaceMethod = new HttpPut(rescUri);
        replaceMethod.setHeader(HttpHeaders.IF_MATCH, etag);
        replaceMethod.addHeader(CONTENT_TYPE, "text/n3");
        replaceMethod.setEntity(new StringEntity("<" + rescUri + "> <info:test#label> \"foo\""));
        try (final CloseableHttpResponse response = execute(replaceMethod)) {
            assertEquals(status.getStatusCode(), getStatus(response));
        }
    }

    private void addTitleWithEtag(final String rescUri, final String title, final String etag, final Status status)
            throws IOException {
        final HttpPatch postProp = new HttpPatch(rescUri);
        postProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        postProp.setHeader(HttpHeaders.IF_MATCH, etag);
        final String updateString =
                "INSERT { <" + rescUri + "> <" + DC.title.getURI() + "> \"" + title + "\" } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        try (final CloseableHttpResponse dcResp = execute(postProp)) {
            assertEquals(status.getStatusCode(), getStatus(dcResp), dcResp.getStatusLine().toString());
        }
    }

    private String deweakify(final String etag) {
        return etag.replaceFirst("W/", "");
    }

    private String toId(final String rescUri) {
        return rescUri.replaceFirst(serverAddress, "");
    }

    private List<String> listMementoIds(final String rescId) throws IOException {
        final HttpGet httpGet = getObjMethod(rescId + "/" + FCR_VERSIONS);
        httpGet.setHeader("Accept", APPLICATION_LINK_FORMAT);

        final String responseBody;
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response), "Didn't get a OK response!");
            responseBody = EntityUtils.toString(response.getEntity());
        }

        final List<String> bodyList = Arrays.asList(responseBody.split("," + System.lineSeparator()));
        return bodyList.stream()
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(Link::valueOf)
                .filter(l -> "memento".equals(l.getRel()))
                .map(l -> l.getUri().toString())
                .map(this::toId)
                .sorted()
                .collect(Collectors.toList());
    }

    private void assertMementoHasMembers(final String membershipMementoRescId,
            final Property hasMemberRelation, final String... memberIds) throws Exception {
        final var subjId = StringUtils.substringBefore(membershipMementoRescId, "/fcr:versions");
        assertHasMembers(null, membershipMementoRescId, subjId, hasMemberRelation, memberIds);
    }

    private void awaitAssertMementoHasMembers(final String membershipMementoRescId,
                                                           final Property hasMemberRelation,
                                                           final String... memberIds) {
        await().untilAsserted(() -> {
            assertMementoHasMembers(membershipMementoRescId, hasMemberRelation, memberIds);
        });
    }

    private void assertHasMembers(final String membershipRescId,
            final Property hasMemberRelation, final String... memberIds) throws Exception {
        assertHasMembers(null, membershipRescId, membershipRescId, hasMemberRelation, memberIds);
    }

    private void assertHasMembers(final String txUri, final String membershipRescId, final String subjectId,
            final Property hasMemberRelation, final String... memberIds) throws Exception {
        final var model = getModel(txUri, membershipRescId);
        final var membershipResc = model.getResource(serverAddress + subjectId);

        assertEquals(memberIds.length, membershipResc.listProperties(hasMemberRelation).toList().size());
        for (final String memberId : memberIds) {
            final var memberUri = serverAddress + memberId;
            assertTrue(membershipResc.hasProperty(hasMemberRelation, createResource(memberUri)),
                    "Did not contain expected member " + memberId);
        }
    }

    private void assertMementoIsMemberOf(final String memberId, final Property isMemberOfRelation,
            final String membershipRescId) throws Exception {
        final var subjId = StringUtils.substringBefore(memberId, "/fcr:versions");
        assertIsMemberOf(memberId, subjId, isMemberOfRelation, membershipRescId);
    }

    private void assertIsMemberOf(final String memberId, final Property isMemberOfRelation,
            final String membershipRescId) throws Exception {
        assertIsMemberOf(memberId, memberId, isMemberOfRelation, membershipRescId);
    }

    private void assertIsMemberOf(final String memberId, final String subjectId,
            final Property isMemberOfRelation, final String membershipRescId) throws Exception {
        final var model = getModel(memberId);
        final var memberResc = model.getResource(serverAddress + subjectId);

        final var membershipUri = serverAddress + membershipRescId;
        assertTrue(memberResc.hasProperty(isMemberOfRelation, createResource(membershipUri)),
                "Did not contain expected membership " + isMemberOfRelation + " " + membershipRescId);
    }

    private void assertMementoHasNoMembership(final String mementoId,
            final Property memberRelation) throws Exception {
        final var subjId = StringUtils.substringBefore(mementoId, "/fcr:versions");
        assertHasNoMembership(getModel(mementoId), subjId, memberRelation);
    }

    private void assertHasNoMembership(final String subjectId, final Property memberRelation) throws Exception {
        assertHasNoMembership(getModel(subjectId), subjectId, memberRelation);
    }

    private void assertHasNoMembership(final Model model, final String subjectId, final Property memberRelation) {
        final var membershipResc = model.getResource(serverAddress + subjectId);
        assertFalse(membershipResc.hasProperty(memberRelation),
                "Expect " + subjectId + " to have no membership");
    }

    private void assertHasNoMembershipWhenOmitted(final String subjectId, final Property memberRelation)
            throws Exception {
        assertHasNoMembership(getModelOmitMembership(subjectId), subjectId, memberRelation);
    }

    private Model getModelOmitMembership(final String pid) throws Exception {
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse response = execute(getOmitMembership(serverAddress + pid))) {
            model.read(response.getEntity().getContent(), serverAddress + pid, "TURTLE");
        }
        return model;
    }

    private HttpGet getOmitMembership(final String uri) {
        final var httpGet = new HttpGet(uri);
        httpGet.addHeader("Prefer", "return=representation; omit=\"" + PREFER_MEMBERSHIP + "\"");
        return httpGet;
    }

    private String createBasicContainer() {
        return createBasicContainer(null, getRandomUniqueId());
    }

    private String createBasicContainer(final String parentId, final String id) {
        return createBasicContainer(parentId, id, null);
    }

    private String createBasicContainer(final String parentId, final String id,
            final String txUri) {
        final String containerId;
        if (parentId != null) {
            containerId = parentId + "/" + id;
        } else {
            containerId = id;
        }
        final HttpPut putContainer = putObjMethod(containerId);
        if (txUri != null) {
            putContainer.addHeader(ATOMIC_ID_HEADER, txUri);
        }
        executeAndClose(putContainer);

        return containerId;
    }

    private String createDirectContainer(final String membershipURI)
            throws Exception {
        return createDirectContainer(membershipURI, PCDM_HAS_MEMBER_PROP, false);
    }

    private String createDirectContainer(final String membershipURI, final Property memberRelation,
            final boolean isMemberOf) throws Exception {
        final String containerId = getRandomUniqueId();

        final HttpPost postIndirect = postObjMethod();
        postIndirect.setHeader("Slug", containerId);
        postIndirect.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        postIndirect.addHeader(CONTENT_TYPE, "text/turtle");

        final var relationProperty = isMemberOf ? "ldp:isMemberOfRelation" : "ldp:hasMemberRelation";
        final String membersRDF = "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "<> " + relationProperty + " <" + memberRelation.getURI() + "> ; " +
                "ldp:membershipResource <" + membershipURI + "> . ";
        postIndirect.setEntity(new StringEntity(membersRDF));
        executeAndClose(postIndirect);

        return containerId;
    }

    private void replacePropertiesWithPUT(final String resourceURI, final Model replaceModel)
            throws Exception {
        replacePropertiesWithPUT(resourceURI, replaceModel, NO_CONTENT);
    }

    private void replacePropertiesWithPUT(final String resourceURI, final Model replaceModel,
            final Status expectedStatus) throws Exception {
        final HttpPut replaceMethod = new HttpPut(resourceURI);
        replaceMethod.addHeader(CONTENT_TYPE, "text/turtle");
        replaceMethod.setHeader("Prefer", "handling=lenient; received=\"minimal\"");
        try (final StringWriter w = new StringWriter()) {
            replaceModel.write(w, "TURTLE");
            replaceMethod.setEntity(new StringEntity(w.toString()));
        }
        assertEquals(expectedStatus.getStatusCode(), getStatus(replaceMethod));
    }
}
