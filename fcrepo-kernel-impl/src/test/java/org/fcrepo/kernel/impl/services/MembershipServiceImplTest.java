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
package org.fcrepo.kernel.impl.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class MembershipServiceImplTest {

    private final static Instant CREATED_DATE = Instant.parse("2019-11-12T10:00:30.0Z");

    private final static String CREATED_BY = "user1";

    private final static Instant LAST_MODIFIED_DATE = Instant.parse("2019-11-12T14:11:05.0Z");

    private final static String LAST_MODIFIED_BY = "user2";

    private final static String STATE_TOKEN = "stately_value";

    private final static Property MEMBER_OF = createProperty("http://example.com/memberOf");

    @Inject
    private PersistentStorageSessionManager pSessionManager;
    @Mock
    private PersistentStorageSession psSession;
    @Inject
    private MembershipService membershipService;
    @Inject
    private ContainmentIndex containmentIndex;
    @Inject
    private ResourceFactory resourceFactory;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    private FedoraId membershipRescId;

    private String txId;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        txId = UUID.randomUUID().toString();

        when(pSessionManager.getSession(txId)).thenReturn(psSession);

        mockGetHeaders(populateHeaders(rootId, BASIC_CONTAINER));
        when(psSession.getTriples(any(FedoraId.class), nullable(Instant.class))).thenAnswer(new Answer<RdfStream>() {
            @Override
            public RdfStream answer(final InvocationOnMock invocation) throws Throwable {
                final var fedoraId = (FedoraId) invocation.getArgument(0);
                final var subject = NodeFactory.createURI(fedoraId.getFullId());
                return new DefaultRdfStream(subject, Stream.empty());
            }
        });

        membershipRescId = mintFedoraId();
    }

    @After
    public void cleanUp() {
        containmentIndex.rollbackTransaction(txId);
        containmentIndex.getContains(null, rootId).forEach(c ->
                containmentIndex.removeContainedBy(txId, rootId, FedoraId.create(c)));
        containmentIndex.commitTransaction(txId);
    }

    // get membership for container with no members
    // get in tx, container no members
    // get in tx, container with newly added members
    // get in tx, container with existing members, no new
    // get in tx, container with existing members and new, no overly
    // get in tx,

    @Test
    public void getMembers_NoMembership() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        containmentIndex.addContainedBy(txId, rootId, membershipRescId);
        membershipService.updateOnCreation(txId, membershipRescId);

        final var results = membershipService.getMembership(txId, membershipRescId);
        assertEquals(0, results.count());
    }

    @Test
    public void getMembers_WithDC_NoMembers() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.updateOnCreation(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.updateOnCreation(txId, dcId);

        final var results = membershipService.getMembership(txId, membershipRescId);
        assertEquals(0, results.count());
    }

    @Test
    public void getMembers_WithDC_AddedMembers_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.updateOnCreation(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.updateOnCreation(txId, dcId);

        final var member1Id = mintFedoraId();
        mockGetHeaders(txId, member1Id, dcId, BASIC_CONTAINER);
        membershipService.updateOnCreation(txId, member1Id);
        final var member2Id = mintFedoraId();
        mockGetHeaders(txId, member2Id, dcId, RdfLexicon.NON_RDF_SOURCE);
        membershipService.updateOnCreation(txId, member2Id);

        final var results = membershipService.getMembership(txId, membershipRescId);
        final var membershipList = results.collect(Collectors.toList());
        assertEquals(2, membershipList.size());

        assertContainsMembership(membershipList, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertContainsMembership(membershipList, membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(txId);

        final var afterCommitResults = membershipService.getMembership(null, membershipRescId);
        final var afterMembershipList = afterCommitResults.collect(Collectors.toList());
        assertEquals(2, afterMembershipList.size());

        assertContainsMembership(afterMembershipList, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertContainsMembership(afterMembershipList, membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
    }

    @Test
    public void getMembers_WithDC_AddedMembers_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.updateOnCreation(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.updateOnCreation(txId, dcId);

        final var member1Id = mintFedoraId();
        mockGetHeaders(txId, member1Id, dcId, BASIC_CONTAINER);
        membershipService.updateOnCreation(txId, member1Id);
        final var member2Id = mintFedoraId();
        mockGetHeaders(txId, member2Id, dcId, RdfLexicon.NON_RDF_SOURCE);
        membershipService.updateOnCreation(txId, member2Id);

        final var containerResults = membershipService.getMembership(txId, membershipRescId);
        assertEquals(0, containerResults.count());

        final var member1Results = membershipService.getMembership(txId, member1Id);
        final var member1Membership = member1Results.collect(Collectors.toList());
        assertEquals(1, member1Membership.size());
        assertContainsMembership(member1Membership, member1Id, MEMBER_OF, membershipRescId);

        final var member2Results = membershipService.getMembership(txId, member2Id);
        final var member2Membership = member2Results.collect(Collectors.toList());
        assertEquals(1, member2Membership.size());
        assertContainsMembership(member2Membership, member2Id, MEMBER_OF, membershipRescId);

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(txId);

        final var committedContainerResults = membershipService.getMembership(null, membershipRescId);
        assertEquals(0, committedContainerResults.count());

        final var committedMember1Results = membershipService.getMembership(null, member1Id);
        final var committedMember1Membership = committedMember1Results.collect(Collectors.toList());
        assertEquals(1, committedMember1Membership.size());
        assertContainsMembership(committedMember1Membership, member1Id, MEMBER_OF, membershipRescId);

        final var committedMember2Results = membershipService.getMembership(null, member2Id);
        final var committedMember2Membership = committedMember2Results.collect(Collectors.toList());
        assertEquals(1, committedMember2Membership.size());
        assertContainsMembership(committedMember2Membership, member2Id, MEMBER_OF, membershipRescId);
    }

    private FedoraId mintFedoraId() {
        return FedoraId.create(UUID.randomUUID().toString());
    }

    private void mockGetHeaders(final ResourceHeaders headers) {
        mockGetHeaders(txId, headers, rootId);
    }

    private void mockGetHeaders(final String txId, final ResourceHeaders headers, final FedoraId parentId) {
        when(psSession.getHeaders(eq(headers.getId()), nullable(Instant.class))).thenReturn(headers);
        containmentIndex.addContainedBy(txId, parentId, headers.getId());
    }

    private void mockGetHeaders(final String txId, final FedoraId fedoraId, final FedoraId parentId,
            final Resource ixModel) {
        final var headers = populateHeaders(fedoraId, parentId, ixModel);
        mockGetHeaders(txId, headers, parentId);

    }

    private ResourceHeaders populateHeaders(final FedoraId fedoraId, final Resource ixModel) {
        return populateHeaders(fedoraId, rootId, ixModel);
    }

    private static ResourceHeaders populateHeaders(final FedoraId fedoraId, final FedoraId parentId,
            final Resource ixModel) {
        final var headers = new ResourceHeadersImpl();
        headers.setId(fedoraId);
        headers.setParent(parentId);
        headers.setInteractionModel(ixModel.getURI());
        headers.setCreatedBy(CREATED_BY);
        headers.setCreatedDate(CREATED_DATE);
        headers.setLastModifiedBy(LAST_MODIFIED_BY);
        headers.setLastModifiedDate(LAST_MODIFIED_DATE);
        headers.setStateToken(STATE_TOKEN);
        return headers;
    }

    private FedoraId createDirectContainer(final FedoraId membershipRescId, final Property relation,
            final boolean useIsMemberOf) {
        final var dcId = mintFedoraId();
        mockGetHeaders(populateHeaders(dcId, RdfLexicon.DIRECT_CONTAINER));

        final var model = ModelFactory.createDefaultModel();
        final var dcRdfResc = model.getResource(dcId.getFullId());
        final var membershipRdfResc = model.getResource(membershipRescId.getFullId());
        dcRdfResc.addProperty(RdfLexicon.MEMBERSHIP_RESOURCE, membershipRdfResc);
        if (relation != null) {
            if (useIsMemberOf) {
                dcRdfResc.addProperty(RdfLexicon.IS_MEMBER_OF_RELATION, relation);
            } else {
                dcRdfResc.addProperty(RdfLexicon.HAS_MEMBER_RELATION, relation);
            }
        }

        when(psSession.getTriples(eq(dcId), nullable(Instant.class))).thenAnswer(new Answer<RdfStream>() {
            @Override
            public RdfStream answer(final InvocationOnMock invocation) throws Throwable {
                return fromModel(model.getResource(dcId.getFullId()).asNode(), model);
            }
        });

        return dcId;
    }

    private void assertContainsMembership(final List<Triple> membershipList, final FedoraId subjectId,
            final Property property, final FedoraId objectId) {

        final var subjectNode = NodeFactory.createURI(subjectId.getFullId());
        final var objectNode = NodeFactory.createURI(objectId.getFullId());

        assertTrue("Membership set did not contain: " + subjectId + " " + property.getURI() + " " + objectId,
                membershipList.stream().anyMatch(t -> t.getSubject().equals(subjectNode)
                        && t.getPredicate().equals(property.asNode())
                        && t.getObject().equals(objectNode)));
    }
}
