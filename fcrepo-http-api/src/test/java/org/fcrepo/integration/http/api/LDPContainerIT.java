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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
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
import static org.fcrepo.kernel.api.RdfLexicon.PROXY_FOR;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.Status;

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
import org.fcrepo.kernel.api.RdfLexicon;
import org.junit.Ignore;
import org.junit.Test;
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
        try (final CloseableHttpResponse response = execute(patch)) {
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
        assertEquals("Patch with sparql update allowed ldp:contains in indirect container!",
                CONFLICT.getStatusCode(), getStatus(patch));
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
        assertEquals("Patch with sparql update allowed ldp:contains in indirect container!",
                CONFLICT.getStatusCode(), getStatus(patch));
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
        assertEquals("Patch with sparql update did not allow non SMT relation in indirect container!",
                NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    @Test
    @Ignore("Needs updated eTags - FCREPO-3410")
    public void testETagOnDeletedLdpIndirectContainerChild() throws Exception {
        final String id = getRandomUniqueId();
        final String members = id + "/members";
        final String child = members + "/child";

        createObjectAndClose(id);

        // Create the IndirectContainer
        final HttpPut createContainer = new HttpPut(serverAddress + members);
        createContainer.addHeader(CONTENT_TYPE, "text/turtle");
        createContainer.addHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        final String membersRDF = "<> <http://www.w3.org/ns/ldp#hasMemberRelation> <info:fedora/test/hasTitle> ; "
            + "<http://www.w3.org/ns/ldp#insertedContentRelation> <http://www.w3.org/2004/02/skos/core#prefLabel>; "
            + "<http://www.w3.org/ns/ldp#membershipResource> <" + serverAddress + id + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        assertEquals("Membership container not created!", CREATED.getStatusCode(), getStatus(createContainer));

        // Create a child with the appropriate property
        final HttpPut createChild = new HttpPut(serverAddress + child);
        createChild.addHeader(CONTENT_TYPE, "text/turtle");
        final String childRDF = "<> <http://www.w3.org/2004/02/skos/core#prefLabel> \"A title\".";
        createChild.setEntity(new StringEntity(childRDF));
        assertEquals("Child container not created!", CREATED.getStatusCode(), getStatus(createChild));

        final String etag1 = getEtag(serverAddress + id);

        assertEquals("Child resource not deleted!", NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + child)));

        final String etag2 = getEtag(serverAddress + id);

        assertNotEquals("ETag didn't change!", etag1, etag2);
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

        final Model replaceModel = getModel(directId);
        replaceModel.removeAll(null, MEMBERSHIP_RESOURCE, null);
        replaceModel.removeAll(null, HAS_MEMBER_RELATION, null);

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
        try (final CloseableHttpResponse response = execute(patch)) {
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
        assertEquals("Patch with sparql update allowed ldp:contains in direct container!",
                CONFLICT.getStatusCode(), getStatus(patch));
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
        assertEquals("Patch with sparql update allowed ldp:contains in direct container!",
                CONFLICT.getStatusCode(), getStatus(patch));
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
        assertEquals("Patch with sparql update did not allow non SMT relation in direct container!",
                NO_CONTENT.getStatusCode(), getStatus(patch));
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
        assertNotEquals("Etag must change after additions", initialEtag, afterPropEtag1);

        // Trigger an update of the membership resource to create version with two members
        setProperty(membershipRescId, DC.subject.getURI(), "Updated");

        assertHasMembers(membershipRescId, PCDM_HAS_MEMBER_PROP, member1Id, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String afterPropEtag2 = getEtag(membershipRescURI);
        assertNotEquals("Etag must change after modification", afterPropEtag1, afterPropEtag2);

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
        assertNotEquals("Etag must change after relation changes", afterPropEtag2, afterRelChangeEtag);

        // Update membership resc to create version where the membership rel has changed
        setProperty(membershipRescId, DC.subject.getURI(), "Updated again");

        final String afterRelChangeEtag2 = getEtag(membershipRescURI);

        TimeUnit.MILLISECONDS.sleep(1500);

        // Delete a member, which will disappear from the head version's membership
        executeAndClose(deleteObjMethod(member1Id));

        assertHasMembers(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
        assertHasNoMembershipWhenOmitted(membershipRescId, PCDM_HAS_MEMBER_PROP);

        final String afterDeleteEtag = getEtag(membershipRescURI);
        assertNotEquals("Etag must change after member delete", afterRelChangeEtag2, afterDeleteEtag);

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

        final var member2Mementos = listMementoIds(member2Id);
        assertEquals(2, member2Mementos.size());
        assertMementoIsMemberOf(member2Mementos.get(0), EX_IS_MEMBER_PROP, membershipRescId);
        assertMementoIsMemberOf(member2Mementos.get(1), RdfLexicon.LDP_MEMBER, membershipRescId);
        assertIsMemberOf(member2Id, RdfLexicon.LDP_MEMBER, membershipRescId);
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
        assertEquals("Rolled back transaction should be gone",
                GONE.getStatusCode(), getStatus(new HttpGet(txUri2)));
    }

    @Test
    public void testEtagForDirectContainerHasMember() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI);

        // Capture baseline etag before any membership is added
        final String initialEtag = getEtag(membershipRescURI);
        final String initialGetEtag = getEtag(new HttpGet(membershipRescURI));
        assertEquals("HEAD and basic GET should produce same etag", initialEtag, initialGetEtag);

        // Add member in transaction
        final var txUri = createTransaction();

        createBasicContainer(directId, "member1", txUri);

        final String txMembershipEtag = getEtag(addTxTo(new HttpHead(membershipRescURI), txUri));

        assertEquals("Etag outside of tx must be unchanged", initialEtag, getEtag(membershipRescURI));
        assertNotEquals("Etag within the tx must have changed", initialEtag, txMembershipEtag);

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));

        final String committedMembershipEtag = getEtag(membershipRescURI);
        assertEquals("Committed etag must match pre-commit etag", txMembershipEtag, committedMembershipEtag);
        assertNotEquals("Committed etag must not match original etag", initialEtag, committedMembershipEtag);

        assertEquals("Committed GET and HEAD etags must match",
                committedMembershipEtag, getEtag(new HttpGet(membershipRescURI)));

        // Verify that etag of membership resource is the same when excluding membership as before there was any
        final String excludeMembershipEtag = getEtag(getOmitMembership(membershipRescURI));
        assertEquals("Etag without membership should match initial etag", initialEtag, excludeMembershipEtag);
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
        assertNotEquals("Committed etag must not match original etag", initialEtag, committedMembershipEtag);

        // Update the membership resource using the etags
        addTitleWithEtag(membershipRescURI, "title1", deweakify(initialEtag), Status.PRECONDITION_FAILED);
        addTitleWithEtag(membershipRescURI, "title2", deweakify(committedMembershipEtag), Status.NO_CONTENT);

        assertNotEquals("Etag must update after modification", committedMembershipEtag, getEtag(membershipRescURI));
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
        assertNotEquals("Committed etag must not match original etag", initialEtag, committedMembershipEtag);

        // Update the membership resource using the etags
        putPropertiesWithEtag(membershipRescURI, deweakify(initialEtag), Status.PRECONDITION_FAILED);
        putPropertiesWithEtag(membershipRescURI, deweakify(committedMembershipEtag), Status.NO_CONTENT);

        assertNotEquals("Etag must update after modification", committedMembershipEtag, getEtag(membershipRescURI));
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
        assertNotNull("Member etag must not be null in tx", txMemberEtag);

        // Commit tx
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpPut(txUri)));

        final String committedMemberEtag = getEtag(memberUri);
        assertEquals("Committed etag must match pre-commit etag", txMemberEtag, committedMemberEtag);

        assertEquals("Membership resc etag must not change", membershipEtag, getEtag(membershipRescURI));

        // Verify etag varies appropriately when excluding membership
        final String excludeMemberEtag = getEtag(getOmitMembership(memberUri));
        assertNotEquals("Etag for member must change when excluding membership",
                committedMemberEtag, excludeMemberEtag);

        final String excludeMembershipEtag = getEtag(getOmitMembership(membershipRescURI));
        assertEquals("Etag for membership resc should not change when excluding membership",
                membershipEtag, excludeMembershipEtag);

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
        assertNotNull("Member resource must have etag", committedMemberEtag);

        // Update the member resource using the etags
        addTitleWithEtag(memberUri, "title1", deweakify("W/\"fake\""), Status.PRECONDITION_FAILED);
        addTitleWithEtag(memberUri, "title2", deweakify(committedMemberEtag), Status.NO_CONTENT);

        assertNotEquals("Etag must update after modification", committedMemberEtag, getEtag(membershipRescURI));
    }

    @Test
    public void testEtagForDirectContainerIsMemberOfPut() throws Exception {
        final var membershipRescId = createBasicContainer();
        final var membershipRescURI = serverAddress + membershipRescId;

        final var directId = createDirectContainer(membershipRescURI, EX_IS_MEMBER_PROP, true);

        final var memberId = createBasicContainer(directId, "member1");
        final var memberUri = serverAddress + memberId;

        final String committedMemberEtag = getEtag(memberUri);
        assertNotNull("Member resource must have etag", committedMemberEtag);

        putPropertiesWithEtag(memberUri, deweakify("W/\"fake\""), Status.PRECONDITION_FAILED);
        putPropertiesWithEtag(memberUri, deweakify(committedMemberEtag), Status.NO_CONTENT);

        assertNotEquals("Etag must update after modification", committedMemberEtag, getEtag(membershipRescURI));
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
        assertEquals("Membership container not created!", CREATED.getStatusCode(), getStatus(createContainer));

        // Create the child resource
        createObjectAndClose(child);

        final String etag1 = getEtag(serverAddress + membershipRescId);
        assertNotEquals("Adding child must change etag of membership resc", etag0, etag1);

        // Wait a second so that the creation and deletion of the child are not simultaneous
        TimeUnit.SECONDS.sleep(1);

        // Delete the child resource
        assertEquals("Child resource not deleted!", NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + child)));

        final String etag2 = getEtag(serverAddress + membershipRescId);

        assertNotEquals("ETag didn't change!", etag1, etag2);
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
            assertEquals(dcResp.getStatusLine().toString(), status.getStatusCode(), getStatus(dcResp));
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
            assertEquals("Didn't get a OK response!", OK.getStatusCode(), getStatus(response));
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
            assertTrue("Did not contain expected member " + memberId,
                    membershipResc.hasProperty(hasMemberRelation, createResource(memberUri)));
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
        assertTrue("Did not contain expected membership " + isMemberOfRelation + " " + membershipRescId,
                memberResc.hasProperty(isMemberOfRelation, createResource(membershipUri)));
    }

    private void assertMementoHasNoMembership(final String mementoId,
            final Property memberRelation) throws Exception {
        final var subjId = StringUtils.substringBefore(mementoId, "/fcr:versions");
        assertHasNoMembership(getModel(mementoId), subjId, memberRelation);
    }

    private void assertHasNoMembership(final String subjectId, final Property memberRelation) throws Exception {
        assertHasNoMembership(getModel(subjectId), subjectId, memberRelation);
    }

    private void assertHasNoMembership(final Model model, final String subjectId, final Property memberRelation)
            throws Exception {
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
