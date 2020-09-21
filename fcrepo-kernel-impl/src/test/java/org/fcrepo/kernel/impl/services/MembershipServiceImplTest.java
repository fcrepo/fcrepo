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
import static org.mockito.ArgumentMatchers.isNull;
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

    private final static Property OTHER_MEMBER_OF = createProperty("http://example.com/otherMemberOf");

    private final static Property OTHER_HAS_MEMBER = createProperty("http://example.com/anotherHasMember");

    @Inject
    private PersistentStorageSessionManager pSessionManager;
    @Mock
    private PersistentStorageSession psSession;
    @Inject
    private MembershipService membershipService;
    @Inject
    private MembershipIndexManager indexManager;
    @Inject
    private ContainmentIndex containmentIndex;

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

        indexManager.clearIndex();
    }

    @Test
    public void getMembers_NoMembership() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        containmentIndex.addContainedBy(txId, rootId, membershipRescId);
        membershipService.resourceCreated(txId, membershipRescId);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
    }

    @Test
    public void getMembers_WithDC_NoMembers() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
    }

    @Test
    public void getMembers_WithDC_AddedMembers_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
    }

    @Test
    public void getMembers_WithDC_AddedMembers_DefaultHasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        // Don't specify a membership relation
        final var dcId = createDirectContainer(membershipRescId, null, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void getMembers_WithDC_AddedMembers_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertIsMemberOf(txId, member1Id, MEMBER_OF, membershipRescId);
        assertIsMemberOf(txId, member2Id, MEMBER_OF, membershipRescId);

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);

        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);
        assertIsMemberOf(null, member2Id, MEMBER_OF, membershipRescId);
    }

    @Test
    public void deleteMember_InDC_AddedInSameTx_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(txId, membershipRescId, 1);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void deleteExistingMember_InDC_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 1);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void deleteExistingMember_InDC_MultipleMembers_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 2);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 1);
        assertCommittedMembershipCount(membershipRescId, 2);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
    }

    @Test
    public void deleteExistingMember_InDC_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(member1Id, 1);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertCommittedMembershipCount(member1Id, 1);
        assertUncommittedMembershipCount(txId, member1Id, 0);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(member1Id, 0);
    }

    @Test
    public void deleteDC_WithMember_CreatedInSameTx() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        createDCMember(dcId, BASIC_CONTAINER);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertUncommittedMembershipCount(txId, membershipRescId, 1);

        mockDeleteHeaders(dcId, rootId, RdfLexicon.DIRECT_CONTAINER);

        // Notify that the DC was deleted
        membershipService.resourceDeleted(txId, dcId);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void deleteExistingDC_WithExistingMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 1);

        mockDeleteHeaders(dcId, rootId, RdfLexicon.DIRECT_CONTAINER);
        // Notify that the DC was deleted
        membershipService.resourceDeleted(txId, dcId);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void deleteExistingMemberAndDC_InSameTx() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        mockDeleteHeaders(dcId, rootId, RdfLexicon.DIRECT_CONTAINER);
        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Delete the member
        membershipService.resourceDeleted(txId, member1Id);
        // Delete the DC itself
        membershipService.resourceDeleted(txId, dcId);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertUncommittedMembershipCount(txId, membershipRescId, 0);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void recreateExistingMember_InDC_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 1);

        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        // Recreate the resource in the same TX
        membershipService.resourceCreated(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 1);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 1);
    }

    @Test
    public void getMembers_MultipleDCsSameMembershipResource_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dc1Id = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dc1Id);

        // Add a child to the outer DC
        final var member1Id = createDCMember(dc1Id, BASIC_CONTAINER);

        final var dc2Id = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dc2Id);

        // Add a child to the outer DC
        final var member2Id = createDCMember(dc2Id, BASIC_CONTAINER);

        assertUncommittedMembershipCount(txId, membershipRescId, 2);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Delete one to ensure only those members are cleaned up
        membershipService.resourceDeleted(txId, member2Id);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void getMembers_MultipleDCsSameMembershipResource_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dc1Id = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dc1Id);

        // Add a child to the outer DC
        final var member1Id = createDCMember(dc1Id, BASIC_CONTAINER);

        final var dc2Id = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dc2Id);

        // Add a child to the outer DC
        final var member2Id = createDCMember(dc2Id, BASIC_CONTAINER);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertUncommittedMembershipCount(txId, member1Id, 1);
        assertUncommittedMembershipCount(txId, member2Id, 1);

        membershipService.commitTransaction(txId);

        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);
        assertIsMemberOf(null, member2Id, MEMBER_OF, membershipRescId);

        // Delete one to ensure only those members are cleaned up
        membershipService.resourceDeleted(txId, member2Id);

        membershipService.commitTransaction(txId);

        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);
        assertCommittedMembershipCount(member2Id, 0);
    }

    @Test
    public void getMembers_DCmemberOfDC() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        // Add a child to the outer DC
        final var outerMemberId = createDCMember(dcId, BASIC_CONTAINER);

        // Add a DC as the child of the first DC
        final var nestedDcId = createDirectContainer(dcId, membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, nestedDcId);

        // Add a child to the nested DC
        final var nestedMemberId = createDCMember(nestedDcId, BASIC_CONTAINER);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertUncommittedMembershipCount(txId, membershipRescId, 3);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, outerMemberId, nestedDcId, nestedMemberId);

        // Delete the nested DC to ensure that it gets cleaned up as both a DC and a member
        membershipService.resourceDeleted(txId, nestedDcId);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, outerMemberId);
    }

    @Test
    public void changeMembershipResource_ForDC() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var membershipResc2Id = mintFedoraId();
        mockGetHeaders(populateHeaders(membershipResc2Id, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipResc2Id);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 2);
        assertCommittedMembershipCount(membershipResc2Id, 0);

        // Change the membership resource for the DC
        mockGetTriplesForDC(dcId, membershipResc2Id, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertHasMembers(txId, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembers(null, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
    }

    @Test
    public void changeMembershipRelation_DC_HasMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(txId, membershipRescId, 2);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Change the membership relation
        mockGetTriplesForDC(dcId, membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertHasMembers(txId, membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);
    }

    @Test
    public void changeResource_DC_HasMemberToIsMemberOf() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(txId, membershipRescId, 1);

        membershipService.commitTransaction(txId);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        // Change the membership direction from a ldp:hasMemberRelation to a ldp:isMemberOfRelation
        mockGetTriplesForDC(dcId, membershipRescId, MEMBER_OF, true);
        membershipService.resourceModified(txId, dcId);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertIsMemberOf(txId, member1Id, MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(txId);

        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);

        // Reverse the membership direction again
        mockGetTriplesForDC(dcId, membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertCommittedMembershipCount(member1Id, 1);
        assertUncommittedMembershipCount(txId, member1Id, 0);
        assertHasMembers(txId, membershipRescId, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(member1Id, 0);
        assertHasMembers(null, membershipRescId, OTHER_HAS_MEMBER, member1Id);
    }

    @Test
    public void changeResource_DC_IsMemberOf() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);

        // Switch DC to a different ldp:isMemberOfRelation
        mockGetTriplesForDC(dcId, membershipRescId, OTHER_MEMBER_OF, true);
        membershipService.resourceModified(txId, dcId);

        assertIsMemberOf(txId, member1Id, OTHER_MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(txId);

        assertIsMemberOf(null, member1Id, OTHER_MEMBER_OF, membershipRescId);

        // Switch back again
        mockGetTriplesForDC(dcId, membershipRescId, MEMBER_OF, true);
        membershipService.resourceModified(txId, dcId);

        assertIsMemberOf(txId, member1Id, MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(txId);

        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);
    }

    @Test
    public void getMementoMembership_AllCreatedAtSameTime_NoChanges() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        final var beforeCreated = Instant.parse("2019-11-10T00:00:00.0Z");
        final var beforeCreatedId = membershipRescId.asMemento(beforeCreated);

        final var afterLastModified = Instant.parse("2019-12-10T00:00:00.0Z");
        final var afterLastModifiedId = membershipRescId.asMemento(afterLastModified);

        // No membership before creation time
        assertUncommittedMembershipCount(txId, beforeCreatedId, 0);
        assertUncommittedMembershipCount(txId, membershipRescId, 1);
        assertUncommittedMembershipCount(txId, afterLastModifiedId, 1);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(beforeCreatedId, 0);
        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertCommittedMembershipCount(afterLastModifiedId, 1);
    }

    @Test
    public void getMementoMembership_OneMembershipAddition_hasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var headMementoId = membershipRescId.asMemento(LAST_MODIFIED_DATE);

        final var beforeAddMementoInstant = Instant.parse("2019-11-12T12:00:00.0Z");
        final var beforeAddMementoId = membershipRescId.asMemento(beforeAddMementoInstant);
        mockGetHeaders(populateHeaders(membershipRescId, rootId, BASIC_CONTAINER, CREATED_DATE,
                beforeAddMementoInstant));

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var memberCreated = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, memberCreated);

        // No membership at first memento timestamp
        assertUncommittedMembershipCount(txId, beforeAddMementoId, 0);
        assertUncommittedMembershipCount(txId, membershipRescId, 1);

        membershipService.commitTransaction(txId);

        // No membership at first memento timestamp
        assertCommittedMembershipCount(beforeAddMementoId, 0);
        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        // Memento request for the head version should return the membership
        assertHasMembers(null, headMementoId, RdfLexicon.LDP_MEMBER, member1Id);
        // Request at the exact time of the membership addition should return member
        final var atAddMementoId = membershipRescId.asMemento(memberCreated);
        assertHasMembers(null, atAddMementoId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void getMementoMembership_AddAndDelete_isMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dcId);

        final var memberCreated = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, memberCreated);
        final var memberCreatedId = member1Id.asMemento(memberCreated);
        final var beforeCreate = Instant.parse("2019-11-10T00:00:00.0Z");
        final var beforeCreateId = member1Id.asMemento(beforeCreate);

        assertUncommittedMembershipCount(txId, beforeCreateId, 0);
        assertUncommittedMembershipCount(txId, member1Id, 1);

        membershipService.commitTransaction(txId);

        // No membership before member created
        assertCommittedMembershipCount(beforeCreateId, 0);
        assertIsMemberOf(null, member1Id, MEMBER_OF, membershipRescId);
        // Explicitly request memento of head
        assertIsMemberOf(null, memberCreatedId, MEMBER_OF, membershipRescId);
        // Request memento after last modified
        final var afterModified = Instant.parse("2019-12-10T00:00:00.0Z");
        final var afterModifiedId = member1Id.asMemento(afterModified);
        assertIsMemberOf(null, afterModifiedId, MEMBER_OF, membershipRescId);

        final var deleteInstant = Instant.parse("2019-11-13T12:00:00.0Z");
        final var deletedMemberId = member1Id.asMemento(deleteInstant);
        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER, memberCreated, deleteInstant);

        final var afterDeleteMemberId = member1Id.asMemento(Instant.parse("2019-11-13T16:00:00.0Z"));

        membershipService.resourceDeleted(txId, member1Id);

        // Make sure delete hasn't leaked
        assertCommittedMembershipCount(member1Id, 1);

        assertIsMemberOf(txId, memberCreatedId, MEMBER_OF, membershipRescId);
        assertUncommittedMembershipCount(txId, deletedMemberId, 0);
        assertUncommittedMembershipCount(txId, afterDeleteMemberId, 0);
        assertUncommittedMembershipCount(txId, member1Id, 0);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(beforeCreateId, 0);
        assertIsMemberOf(null, memberCreatedId, MEMBER_OF, membershipRescId);
        assertCommittedMembershipCount(deletedMemberId, 0);
        assertCommittedMembershipCount(afterDeleteMemberId, 0);
        assertCommittedMembershipCount(member1Id, 0);
    }

    @Test
    public void getMementoMembership_AddAndDelete_hasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var beforeAddMementoInstant = Instant.parse("2019-11-12T12:00:00.0Z");
        final var beforeAddMementoId = membershipRescId.asMemento(beforeAddMementoInstant);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Created = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, member1Created);

        final var member2Created = Instant.parse("2019-11-12T20:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        final var membershipRescAtMember1Create1Id = membershipRescId.asMemento(member1Created);
        final var membershipRescAtMember2Create1Id = membershipRescId.asMemento(member2Created);

        // No membership before members added
        assertUncommittedMembershipCount(txId, beforeAddMementoId, 0);
        // Check membership at the times members were added
        assertUncommittedMembershipCount(txId, membershipRescAtMember1Create1Id, 1);
        assertUncommittedMembershipCount(txId, membershipRescAtMember2Create1Id, 2);
        assertUncommittedMembershipCount(txId, membershipRescId, 2);

        membershipService.commitTransaction(txId);

        // No membership before members added
        assertCommittedMembershipCount(beforeAddMementoId, 0);
        assertHasMembers(null, membershipRescAtMember1Create1Id, RdfLexicon.LDP_MEMBER, member1Id);
        // Inbetween the members being added
        assertHasMembers(null, membershipRescId.asMemento(Instant.parse("2019-11-12T15:00:00.0Z")),
                RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(null, membershipRescAtMember2Create1Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Delete one of the members
        final var deleteInstant = Instant.parse("2019-11-13T12:00:00.0Z");
        mockDeleteHeaders(member2Id, dcId, BASIC_CONTAINER, member2Created, deleteInstant);

        membershipService.resourceDeleted(txId, member2Id);

        final var membershipRescAtDeleteId = membershipRescId.asMemento(deleteInstant);
        final var membershipRescAfterDeleteId = membershipRescId.asMemento(Instant.parse("2019-11-13T15:00:00.0Z"));

        assertUncommittedMembershipCount(txId, membershipRescAtMember2Create1Id, 2);
        assertUncommittedMembershipCount(txId, membershipRescAtDeleteId, 1);
        assertUncommittedMembershipCount(txId, membershipRescAfterDeleteId, 1);

        membershipService.commitTransaction(txId);

        // No membership before members added
        assertCommittedMembershipCount(beforeAddMementoId, 0);
        assertHasMembers(null, membershipRescAtMember1Create1Id, RdfLexicon.LDP_MEMBER, member1Id);
        // Inbetween the members being added
        assertHasMembers(null, membershipRescId.asMemento(Instant.parse("2019-11-12T15:00:00.0Z")),
                RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(null, membershipRescAtMember2Create1Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasMembers(null, membershipRescAtDeleteId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(null, membershipRescAfterDeleteId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void rollbackTransaction() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.rollbackTransaction(txId);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        // Commit the transaction and verify the non-rollback entries persist
        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void resetMembershipIndex() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.reset();

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);
    }

    private void assertHasMembers(final String txId, final FedoraId membershipRescId,
            final Property hasMemberRelation, final FedoraId... memberIds) {
        final var membershipList = getMembershipList(txId, membershipRescId);
        assertEquals(memberIds.length, membershipList.size());
        final var subjectId = membershipRescId.asBaseId();
        for (final FedoraId memberId : memberIds) {
            assertContainsMembership(membershipList, subjectId, hasMemberRelation, memberId);
        }
    }

    private void assertIsMemberOf(final String txId, final FedoraId memberId, final Property isMemberOf,
            final FedoraId membershipRescId) {
        final var membershipList = getMembershipList(txId, memberId);
        assertEquals(1, membershipList.size());
        assertContainsMembership(membershipList, memberId.asBaseId(), isMemberOf, membershipRescId);
    }

    private List<Triple> getMembershipList(final String txId, final FedoraId fedoraId) {
        final var results = membershipService.getMembership(txId, fedoraId);
        return results.collect(Collectors.toList());
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
            final Resource ixModel, final Instant createdDate, final Instant lastModified) {
        final var headers = populateHeaders(fedoraId, parentId, ixModel, createdDate, lastModified);
        mockGetHeaders(txId, headers, parentId);
    }

    private void mockDeleteHeaders(final FedoraId fedoraId, final FedoraId parentId, final Resource ixModel) {
        mockDeleteHeaders(fedoraId, parentId, ixModel, CREATED_DATE, LAST_MODIFIED_DATE);
    }

    private void mockDeleteHeaders(final FedoraId fedoraId, final FedoraId parentId, final Resource ixModel,
            final Instant createdDate, final Instant deleteInstant) {
        final var deletedHeaders = populateHeaders(fedoraId, parentId, ixModel, createdDate, deleteInstant);
        deletedHeaders.setDeleted(true);
        when(psSession.getHeaders(eq(fedoraId), isNull())).thenReturn(deletedHeaders);
    }

    private FedoraId createDCMember(final FedoraId dcId, final Resource ixModel) {
        final var memberId = mintFedoraId();
        mockGetHeaders(txId, memberId, dcId, ixModel, CREATED_DATE, LAST_MODIFIED_DATE);
        membershipService.resourceCreated(txId, memberId);
        return memberId;
    }

    private FedoraId createDCMember(final FedoraId dcId, final Resource ixModel, final Instant lastModified) {
        final var memberId = mintFedoraId();
        mockGetHeaders(txId, memberId, dcId, ixModel, lastModified, lastModified);
        membershipService.resourceCreated(txId, memberId);
        return memberId;
    }

    private ResourceHeaders populateHeaders(final FedoraId fedoraId, final Resource ixModel) {
        return populateHeaders(fedoraId, rootId, ixModel);
    }

    private static ResourceHeaders populateHeaders(final FedoraId fedoraId, final FedoraId parentId,
            final Resource ixModel) {
        return populateHeaders(fedoraId, parentId, ixModel, CREATED_DATE, LAST_MODIFIED_DATE);
    }

    private static ResourceHeadersImpl populateHeaders(final FedoraId fedoraId, final FedoraId parentId,
            final Resource ixModel, final Instant createdDate, final Instant lastModifiedDate) {
        final var headers = new ResourceHeadersImpl();
        headers.setId(fedoraId);
        headers.setParent(parentId);
        headers.setInteractionModel(ixModel.getURI());
        headers.setCreatedBy(CREATED_BY);
        headers.setCreatedDate(createdDate);
        headers.setLastModifiedBy(LAST_MODIFIED_BY);
        headers.setLastModifiedDate(lastModifiedDate);
        headers.setStateToken(STATE_TOKEN);
        return headers;
    }

    private FedoraId createDirectContainer(final FedoraId membershipRescId, final Property relation,
            final boolean useIsMemberOf) {
        return createDirectContainer(rootId, membershipRescId, relation, useIsMemberOf);
    }

    private FedoraId createDirectContainer(final FedoraId parentId, final FedoraId membershipRescId, final Property relation,
            final boolean useIsMemberOf) {
        final var dcId = mintFedoraId();
        mockGetHeaders(populateHeaders(dcId, parentId, RdfLexicon.DIRECT_CONTAINER));
        mockGetTriplesForDC(dcId, membershipRescId, relation, useIsMemberOf);
        return dcId;
    }

    private void mockGetTriplesForDC(final FedoraId dcId, final FedoraId membershipRescId, final Property relation,
            final boolean useIsMemberOf) {
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

    private void assertCommittedMembershipCount(final FedoraId subjectId, final int expected) {
        final var results = membershipService.getMembership(null, subjectId);
        assertEquals("Incorrect number of committed membership properties for " + subjectId,
                expected, results.count());
    }

    private void assertUncommittedMembershipCount(final String txId, final FedoraId subjectId, final int expected) {
        final var results = membershipService.getMembership(txId, subjectId);
        assertEquals("Incorrect number of uncommitted membership properties for " + subjectId,
                expected, results.count());
    }
}
