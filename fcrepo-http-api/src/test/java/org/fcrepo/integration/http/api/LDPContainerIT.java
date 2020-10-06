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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PROXY_FOR;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author bbpennel
 */
@TestExecutionListeners(
        listeners = { LinuxTestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class LDPContainerIT extends AbstractResourceIT {

    private final String PCDM_HAS_MEMBER = "http://pcdm.org/models#hasMember";

    private final Property PCDM_HAS_MEMBER_PROP = createProperty(PCDM_HAS_MEMBER);

    private final Property EX_IS_MEMBER_PROP = createProperty("http://example.com/isMember");

    private final Property EX_HAS_MEMBER_PROP = createProperty("http://example.com/hasMember");

    private static final String DIRECT_CONTAINER_LINK_HEADER = "<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\"";

    private static final String INDIRECT_CONTAINER_LINK_HEADER = "<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\"";

    @Ignore //TODO Fix this test
    @Test
    public void testIndirectContainerDefaults() throws Exception {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final Model model = getModel(id);

        final Resource resc = model.getResource(subjectURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, INDIRECT_CONTAINER));

        assertTrue("Default ldp:membershipResource must be set",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertTrue("Default ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));

        assertTrue("Default ldp:insertedContentRelation must be set",
                resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT));
    }

    @Ignore //TODO Fix this test
    @Test
    public void testIndirectContainerOverrides() throws Exception {
        final String parentId = getRandomUniqueId();
        final String parentURI = serverAddress + parentId;
        final HttpPut putParent = putObjMethod(parentId);
        executeAndClose(putParent);

        final String indirectId = parentId + "/indirect";
        final String indirectURI = serverAddress + indirectId;
        createIndirectContainer(indirectId, parentURI);

        final Model model = getModel(indirectId);
        final Resource resc = model.getResource(indirectURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, INDIRECT_CONTAINER));

        assertTrue("Provided ldp:membershipResource must be present",
                resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)));
        assertFalse("Default ldp:membershipResource must not be present",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertTrue("Provided ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, PCDM_HAS_MEMBER_PROP));
        assertFalse("Default ldp:hasMemberRelation must not be present",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));

        assertTrue("Provided ldp:insertedContentRelation must be set",
                resc.hasProperty(INSERTED_CONTENT_RELATION, PROXY_FOR));
        assertFalse("Default ldp:insertedContentRelation must not be present",
                resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT));
    }

    @Ignore //TODO Fix this test
    @Test
    public void testIndirectContainerDefaultsAfterPUT() throws Exception {
        final String parentId = getRandomUniqueId();
        final String parentURI = serverAddress + parentId;
        createObjectAndClose(parentId);

        final String indirectId = parentId + "/direct";
        final String indirectURI = serverAddress + indirectId;
        createIndirectContainer(indirectId, parentURI);

        final Model replaceModel = getModel(indirectId);
        replaceModel.removeAll(null, MEMBERSHIP_RESOURCE, null);
        replaceModel.removeAll(null, HAS_MEMBER_RELATION, null);
        replaceModel.removeAll(null, INSERTED_CONTENT_RELATION, null);

        replacePropertiesWithPUT(indirectURI, replaceModel);

        final Model model = getModel(indirectId);
        final Resource resc = model.getResource(indirectURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, INDIRECT_CONTAINER));

        assertFalse("Provided ldp:membershipResource must be removed",
                resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)));
        assertTrue("Default ldp:membershipResource must be set",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertFalse("Provided ldp:hasMemberRelation must be removed",
                resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)));
        assertTrue("Default ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));

        assertFalse("Provided ldp:insertedContentRelation must be removed",
                resc.hasProperty(INSERTED_CONTENT_RELATION, PROXY_FOR));
        assertTrue("Default ldp:insertedContentRelation must be present",
                resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT));
    }

    @Ignore //TODO Fix this test
    @Test
    public void testIndirectContainerDefaultsAfterPatch() throws Exception {
        final String parentId = getRandomUniqueId();
        final String parentURI = serverAddress + parentId;
        final HttpPut putParent = putObjMethod(parentId);
        executeAndClose(putParent);

        final String indirectId = parentId + "/indirect";
        final String indirectURI = serverAddress + indirectId;
        createIndirectContainer(indirectId, parentURI);

        final HttpPatch patch = new HttpPatch(indirectURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "DELETE { <> ldp:membershipResource <" + parentURI + "> ;\n" +
                "ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> ;\n" +
                "ldp:insertedContentRelation <" + PROXY_FOR.getURI() + "> . } WHERE {}"));
        try (CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model = getModel(indirectId);
        final Resource resc = model.getResource(indirectURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, INDIRECT_CONTAINER));

        assertFalse("Provided ldp:membershipResource must be removed",
                resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)));
        assertTrue("Default ldp:membershipResource must be set",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertFalse("Provided ldp:hasMemberRelation must be removed",
                resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)));
        assertTrue("Default ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));

        assertFalse("Provided ldp:insertedContentRelation must be removed",
                resc.hasProperty(INSERTED_CONTENT_RELATION, PROXY_FOR));
        assertTrue("Default ldp:insertedContentRelation must be present",
                resc.hasProperty(INSERTED_CONTENT_RELATION, MEMBER_SUBJECT));
    }

    private void createIndirectContainer(final String indirectId, final String membershipURI) throws Exception {
        final HttpPut putIndirect = putObjMethod(indirectId);
        putIndirect.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        putIndirect.addHeader(CONTENT_TYPE, "text/turtle");
        final String membersRDF = "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "<> ldp:hasMemberRelation <" + PCDM_HAS_MEMBER + "> ; " +
                "ldp:insertedContentRelation <" + PROXY_FOR.getURI() + ">; " +
                "ldp:membershipResource <" + membershipURI + "> . ";
        putIndirect.setEntity(new StringEntity(membersRDF));
        executeAndClose(putIndirect);
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
        assertTrue("Must have container type", resc.hasProperty(RDF.type, DIRECT_CONTAINER));

        assertTrue("Default ldp:membershipResource must be set",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));
        assertTrue("Default ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));
    }

    @Test
    public void testDirectContainerOverrides() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final var directId = createDirectContainer(parentURI);
        final String directURI = serverAddress + directId;

        final Model model = getModel(directId);
        final Resource resc = model.getResource(directURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, DIRECT_CONTAINER));

        assertTrue("Provided ldp:membershipResource must be present",
                resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)));
        assertFalse("Default ldp:membershipResource must not be present",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertTrue("Provided ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, PCDM_HAS_MEMBER_PROP));
        assertFalse("Default ldp:hasMemberRelation must not be present",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));
    }

    @Test
    public void testDirectContainerDefaultsAfterPUT() throws Exception {
        final String parentId = createBasicContainer();
        final String parentURI = serverAddress + parentId;

        final var directId = createDirectContainer(parentURI);
        final String directURI = serverAddress + directId;

        final Model replaceModel = ModelFactory.createDefaultModel();
        // TODO switch back removing individual properties once lenient handling of SMTs is in place
//        final Model replaceModel = getModel(directId);
//        replaceModel.removeAll(null, MEMBERSHIP_RESOURCE, null);
//        replaceModel.removeAll(null, HAS_MEMBER_RELATION, null);

        replacePropertiesWithPUT(directURI, replaceModel);

        final Model model = getModel(directId);
        final Resource resc = model.getResource(directURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, DIRECT_CONTAINER));

        assertFalse("Provided ldp:membershipResource must be removed",
                resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)));
        assertTrue("Default ldp:membershipResource must be set",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertFalse("Provided ldp:hasMemberRelation must be removed",
                resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)));
        assertTrue("Default ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));
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
        try (CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model = getModel(directId);
        final Resource resc = model.getResource(directURI);
        assertTrue("Must have container type", resc.hasProperty(RDF.type, DIRECT_CONTAINER));

        assertFalse("Provided ldp:membershipResource must be removed",
                resc.hasProperty(MEMBERSHIP_RESOURCE, createResource(parentURI)));
        assertTrue("Default ldp:membershipResource must be set",
                resc.hasProperty(MEMBERSHIP_RESOURCE, resc));

        assertFalse("Provided ldp:hasMemberRelation must be removed",
                resc.hasProperty(HAS_MEMBER_RELATION, createResource(parentURI)));
        assertTrue("Default ldp:hasMemberRelation must be set",
                resc.hasProperty(HAS_MEMBER_RELATION, LDP_MEMBER));
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

    private void assertHasMembers(final String membershipRescId,
            final Property hasMemberRelation, final String... memberIds) throws Exception {
        final var model = getModel(membershipRescId);
        final var membershipResc = model.getResource(serverAddress + membershipRescId);

        assertEquals(memberIds.length, membershipResc.listProperties(hasMemberRelation).toList().size());
        for (final String memberId : memberIds) {
            final var memberUri = serverAddress + memberId;
            assertTrue("Did not contain expected member " + memberId,
                    membershipResc.hasProperty(hasMemberRelation, createResource(memberUri)));
        }
    }

    private void assertIsMemberOf(final String memberId, final Property isMemberOfRelation,
            final String membershipRescId) throws Exception {
        final var model = getModel(memberId);
        final var memberResc = model.getResource(serverAddress + memberId);

        final var membershipUri = serverAddress + membershipRescId;
        assertTrue("Did not contain expected membership " + isMemberOfRelation + " " + membershipRescId,
                memberResc.hasProperty(isMemberOfRelation, createResource(membershipUri)));
    }

    private void assertHasNoMembership(final String subjectId, final Property memberRelation) throws Exception {
        assertHasNoMembership(getModel(subjectId), subjectId, memberRelation);
    }

    private void assertHasNoMembership(final Model model, final String subjectId, final Property memberRelation) throws Exception {
        final var membershipResc = model.getResource(serverAddress + subjectId);
        assertFalse("Expect " + subjectId + " to have no membership",
                membershipResc.hasProperty(memberRelation));
    }

    private void assertHasNoMembershipWhenOmitted(final String subjectId, final Property memberRelation)
            throws Exception {
        assertHasNoMembership(getModelOmitMembership(subjectId), subjectId, memberRelation);
    }

    private Model getModelOmitMembership(final String pid) throws Exception {
        final Model model = createDefaultModel();
        final var httpGet = new HttpGet(serverAddress + pid);
        httpGet.addHeader("Prefer", "return=representation; omit=\"" + PREFER_MEMBERSHIP + "\"");
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), serverAddress + pid, "TURTLE");
        }
        return model;
    }

    private String createBasicContainer() {
        return createBasicContainer(null, getRandomUniqueId());
    }

    private String createBasicContainer(final String parentId, final String id) {
        final String containerId;
        if (parentId != null) {
            containerId = parentId + "/" + id;
        } else {
            containerId = id;
        }
        final HttpPut putContainer = putObjMethod(containerId);
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
        final HttpPut replaceMethod = new HttpPut(resourceURI);
        replaceMethod.addHeader(CONTENT_TYPE, "text/turtle");
        replaceMethod.setHeader("Prefer", "handling=lenient; received=\"minimal\"");
        try (final StringWriter w = new StringWriter()) {
            replaceModel.write(w, "TURTLE");
            replaceMethod.setEntity(new StringEntity(w.toString()));
        }
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));
    }
}
