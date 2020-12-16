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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/membershipServiceTest.xml")
public class MembershipServiceImplTest {

    private final static Instant CREATED_DATE = Instant.parse("2019-11-12T10:00:30.0Z");

    private final static String CREATED_BY = "user1";

    private final static Instant LAST_MODIFIED_DATE = Instant.parse("2019-11-12T14:11:05.0Z");

    private final static Instant LAST_MODIFIED_DATE2 = Instant.parse("2019-11-12T16:10:00.0Z");

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
    @Inject
    private OcflPropsConfig propsConfig;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    private FedoraId membershipRescId;

    private String txId;
    private String shortLivedTx;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        txId = UUID.randomUUID().toString();
        shortLivedTx = UUID.randomUUID().toString();

        when(pSessionManager.getSession(txId)).thenReturn(psSession);
        when(pSessionManager.getSession(shortLivedTx)).thenReturn(psSession);

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

        setField(propsConfig, "autoVersioningEnabled", Boolean.TRUE);
    }

    @After
    public void cleanUp() {
        containmentIndex.reset();
        indexManager.clearIndex();
    }

    @Test
    public void getMembers_NoMembership() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        containmentIndex.addContainedBy(txId, rootId, membershipRescId);
        membershipService.resourceCreated(txId, membershipRescId);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(txId, membershipRescId));
    }

    @Test
    public void getMembers_WithDC_NoMembers() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(txId, membershipRescId));
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

        final var lastUpdated = membershipService.getLastUpdatedTimestamp(txId, membershipRescId);
        assertNotNull(lastUpdated);

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertEquals("Last updated timestamp should not change during commit",
                lastUpdated, membershipService.getLastUpdatedTimestamp(null, membershipRescId));
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

        final var member1Updated = membershipService.getLastUpdatedTimestamp(txId, member1Id);
        assertNotNull(member1Updated);
        final var member2Updated = membershipService.getLastUpdatedTimestamp(txId, member2Id);
        assertNotNull(member2Updated);
        assertNull("No membership expected for the membership resource",
                membershipService.getLastUpdatedTimestamp(txId, membershipRescId));

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);

        assertHasMembersNoTx(member1Id, MEMBER_OF, membershipRescId);
        assertHasMembersNoTx(member2Id, MEMBER_OF, membershipRescId);

        assertEquals(member1Updated, membershipService.getLastUpdatedTimestamp(null, member1Id));
        assertEquals(member2Updated, membershipService.getLastUpdatedTimestamp(null, member2Id));
    }

    @Test
    public void getMembers_WithDC_BinaryAsMembershipResc() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, RdfLexicon.NON_RDF_SOURCE));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        final var descId = membershipRescId.asDescription();

        assertHasMembers(txId, descId, RdfLexicon.LDP_MEMBER, member1Id);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(descId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void deleteMember_InDC_AddedInSameTx_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(txId, membershipRescId, 1);

        assertNotNull(membershipService.getLastUpdatedTimestamp(txId, membershipRescId));

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(txId, membershipRescId));

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(null, membershipRescId));
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

        final var lastUpdated = membershipService.getLastUpdatedTimestamp(null, membershipRescId);
        assertNotNull(lastUpdated);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(txId, member1Id);

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);

        final var afterDeleteUpdated = membershipService.getLastUpdatedTimestamp(null, membershipRescId);
        assertNotNull(afterDeleteUpdated);
        assertNotEquals(lastUpdated, afterDeleteUpdated);
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

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
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
    public void purgeDC() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 1);

        when(psSession.getHeaders(eq(dcId), nullable(Instant.class))).thenThrow(
                new PersistentItemNotFoundException(""));

        membershipService.resourceDeleted(txId, dcId);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertUncommittedMembershipCount(txId, membershipRescId, 0);
    }

    @Test
    public void purgeMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 2);

        final var lastUpdated = membershipService.getLastUpdatedTimestamp(null, membershipRescId);
        assertNotNull(lastUpdated);

        when(psSession.getHeaders(eq(member1Id), nullable(Instant.class))).thenThrow(
                new PersistentItemNotFoundException(""));

        membershipService.resourceDeleted(txId, member1Id);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
        assertUncommittedMembershipCount(txId, membershipRescId, 1);

        assertEquals(lastUpdated, membershipService.getLastUpdatedTimestamp(null, membershipRescId));
    }

    @Test
    public void purgeMembershipResource_isMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(member1Id, 1);

        when(psSession.getHeaders(eq(membershipRescId), nullable(Instant.class))).thenThrow(
                new PersistentItemNotFoundException(""));

        membershipService.resourceDeleted(txId, membershipRescId);

        assertCommittedMembershipCount(member1Id, 0);
        assertUncommittedMembershipCount(txId, member1Id, 0);
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

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Delete one to ensure only those members are cleaned up
        membershipService.resourceDeleted(txId, member2Id);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
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

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
        assertIsMemberOfNoTx(member2Id, MEMBER_OF, membershipRescId);

        // Delete one to ensure only those members are cleaned up
        membershipService.resourceDeleted(txId, member2Id);

        membershipService.commitTransaction(txId);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
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

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, outerMemberId, nestedDcId, nestedMemberId);

        // Delete the nested DC to ensure that it gets cleaned up as both a DC and a member
        membershipService.resourceDeleted(txId, nestedDcId);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, outerMemberId);
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

        final var msRescUpdated = membershipService.getLastUpdatedTimestamp(null, membershipRescId);
        assertNotNull(msRescUpdated);
        assertNull(membershipService.getLastUpdatedTimestamp(null, membershipResc2Id));

        // Change the membership resource for the DC
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipResc2Id, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertHasMembers(txId, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        final var msRescUpdatedAfter = membershipService.getLastUpdatedTimestamp(null, membershipRescId);
        assertNotNull(msRescUpdatedAfter);
        assertNotEquals("First membership resc should have changed last_updated timestamp",
                msRescUpdated, msRescUpdatedAfter);
        assertNotNull(membershipService.getLastUpdatedTimestamp(null, membershipResc2Id));
    }

    @Test
    public void changeMembershipResource_ForDC_ManualVersioning() throws Exception {
        setField(propsConfig, "autoVersioningEnabled", Boolean.FALSE);

        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var membershipResc2Id = mintFedoraId();
        mockGetHeaders(populateHeaders(membershipResc2Id, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipResc2Id);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertCommittedMembershipCount(membershipResc2Id, 0);

        // Change the membership resource for the DC without creating a version
        mockListVersion(dcId);
        mockGetTriplesForDCHead(dcId, CREATED_DATE, membershipResc2Id, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(txId, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);

        // Change membership property without versioning
        mockGetTriplesForDCHead(dcId, CREATED_DATE, membershipResc2Id, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(txId, membershipResc2Id, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, OTHER_HAS_MEMBER, member1Id);

        // Create version from former head version
        final var versionChangeTime = Instant.parse("2019-11-13T12:00:00.0Z");
        mockListVersion(dcId, versionChangeTime);
        // New head state matches previous head state for the moment
        mockGetTriplesForDC(dcId, versionChangeTime, membershipResc2Id, OTHER_HAS_MEMBER, false);
        mockGetHeaders(txId, dcId.asMemento(versionChangeTime), populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, versionChangeTime), rootId);

        // Change memebership resource after having created version
        final var afterVersionChangeTime = Instant.parse("2019-11-13T14:00:00.0Z");
        mockGetHeaders(txId, dcId, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, afterVersionChangeTime), rootId);
        mockGetTriplesForDCHead(dcId, afterVersionChangeTime, membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        // Membership resc 2 should still have a member prior to the version creation/last property update
        assertHasMembers(txId, membershipResc2Id.asMemento(CREATED_DATE), OTHER_HAS_MEMBER,
                member1Id);
        assertUncommittedMembershipCount(txId, membershipResc2Id, 0);
        assertHasMembersNoTx(membershipResc2Id, OTHER_HAS_MEMBER, member1Id);
        assertHasMembers(txId, membershipRescId, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(membershipResc2Id, 0);
        assertHasMembersNoTx(membershipResc2Id.asMemento(CREATED_DATE), OTHER_HAS_MEMBER,
                member1Id);
        assertCommittedMembershipCount(membershipRescId.asMemento(CREATED_DATE), 0);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id);
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

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Change the membership relation
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(txId, dcId);

        assertHasMembers(txId, membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);
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

        assertHasMembers(null, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var msRescUpdated1 = indexManager.getLastUpdated(null, membershipRescId);
        assertNotNull(msRescUpdated1);
        assertNull(indexManager.getLastUpdated(null, member1Id));

        indexManager.logMembership();

        // Change the membership direction from a ldp:hasMemberRelation to a ldp:isMemberOfRelation
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, MEMBER_OF, true);
        membershipService.resourceModified(txId, dcId);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertIsMemberOf(txId, member1Id, MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(txId);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);

        final var memRescUpdated1 = indexManager.getLastUpdated(null, member1Id);
        assertNotNull(memRescUpdated1);
        final var msRescUpdated2 = indexManager.getLastUpdated(null, membershipRescId);
        assertNotNull(msRescUpdated2);
        assertNotEquals(msRescUpdated1, msRescUpdated2);

        indexManager.logMembership();

        // Reverse the membership direction again
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE2, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(txId, dcId, rootId, RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, LAST_MODIFIED_DATE2);
        membershipService.resourceModified(txId, dcId);

        assertCommittedMembershipCount(member1Id, 1);
        assertUncommittedMembershipCount(txId, member1Id, 0);
        assertHasMembers(txId, membershipRescId, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(txId);

        assertCommittedMembershipCount(member1Id, 0);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id);

        indexManager.logMembership();

        final var memRescUpdated2 = indexManager.getLastUpdated(null, member1Id);
        assertNotNull(memRescUpdated2);
        assertNotEquals(memRescUpdated1, memRescUpdated2);
        final var msRescUpdated3 = indexManager.getLastUpdated(null, membershipRescId);
        assertNotNull(msRescUpdated3);
        assertNotEquals(msRescUpdated2, msRescUpdated3);
    }

    @Test
    public void changeResource_DC_IsMemberOf() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);

        // Switch DC to a different ldp:isMemberOfRelation
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, OTHER_MEMBER_OF, true);
        membershipService.resourceModified(txId, dcId);

        assertIsMemberOf(txId, member1Id, OTHER_MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(txId);

        assertIsMemberOfNoTx(member1Id, OTHER_MEMBER_OF, membershipRescId);

        // Switch back again
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, MEMBER_OF, true);
        membershipService.resourceModified(txId, dcId);

        assertIsMemberOf(txId, member1Id, MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(txId);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
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
        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
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
        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        // Memento request for the head version should return the membership
        assertHasMembersNoTx(headMementoId, RdfLexicon.LDP_MEMBER, member1Id);
        // Request at the exact time of the membership addition should return member
        final var atAddMementoId = membershipRescId.asMemento(memberCreated);
        assertHasMembersNoTx(atAddMementoId, RdfLexicon.LDP_MEMBER, member1Id);
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
        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
        // Explicitly request memento of head
        assertIsMemberOfNoTx(memberCreatedId, MEMBER_OF, membershipRescId);
        // Request memento after last modified
        final var afterModified = Instant.parse("2019-12-10T00:00:00.0Z");
        final var afterModifiedId = member1Id.asMemento(afterModified);
        assertIsMemberOfNoTx(afterModifiedId, MEMBER_OF, membershipRescId);

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
        assertIsMemberOfNoTx(memberCreatedId, MEMBER_OF, membershipRescId);
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
        assertHasMembersNoTx(membershipRescAtMember1Create1Id, RdfLexicon.LDP_MEMBER, member1Id);
        // Inbetween the members being added
        assertHasMembersNoTx(membershipRescId.asMemento(Instant.parse("2019-11-12T15:00:00.0Z")),
                RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescAtMember2Create1Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

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
        assertHasMembersNoTx(membershipRescAtMember1Create1Id, RdfLexicon.LDP_MEMBER, member1Id);
        // Inbetween the members being added
        assertHasMembersNoTx(membershipRescId.asMemento(Instant.parse("2019-11-12T15:00:00.0Z")),
                RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescAtMember2Create1Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasMembersNoTx(membershipRescAtDeleteId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescAfterDeleteId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void rollbackTransaction() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.rollbackTransaction(txId);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        // Commit the transaction and verify the non-rollback entries persist
        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void resetMembershipIndex() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(txId, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.reset();

        assertUncommittedMembershipCount(txId, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void populateMembershipHistory_DC_DeletedMember() throws Exception {
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

        membershipService.commitTransaction(txId);

        // Delete one of the members
        final var deleteInstant = Instant.parse("2019-11-13T12:00:00.0Z");
        mockDeleteHeaders(member2Id, dcId, BASIC_CONTAINER, member2Created, deleteInstant);

        membershipService.resourceDeleted(txId, member2Id);

        final var membershipRescAtDeleteId = membershipRescId.asMemento(deleteInstant);
        final var membershipRescAfterDeleteId = membershipRescId.asMemento(Instant.parse("2019-11-13T15:00:00.0Z"));

        membershipService.commitTransaction(txId);

        // Clear the index
        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE);

        // Repopulate index
        membershipService.populateMembershipHistory(txId, dcId);

        membershipService.commitTransaction(txId);

        // No membership before members added
        assertCommittedMembershipCount(beforeAddMementoId, 0);
        assertHasMembersNoTx(membershipRescAtMember1Create1Id, RdfLexicon.LDP_MEMBER, member1Id);
        // Inbetween the members being added
        assertHasMembersNoTx(membershipRescId.asMemento(Instant.parse("2019-11-12T15:00:00.0Z")),
                RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescAtMember2Create1Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertHasMembersNoTx(membershipRescAtDeleteId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescAfterDeleteId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void populateMembershipHistory_DC_ChangeRelation_AddedMemberAfter() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        membershipService.commitTransaction(txId);

        // Change the membership relation
        final var changeRelationInstant = Instant.parse("2019-11-14T12:00:00.0Z");
        final var dcAtChangeRelation = dcId.asMemento(changeRelationInstant);

        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, changeRelationInstant);

        mockGetTriplesForDC(dcId, changeRelationInstant, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(txId, dcAtChangeRelation, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, changeRelationInstant), rootId);
        membershipService.resourceModified(txId, dcId);

        membershipService.commitTransaction(txId);

        assertHasMembersNoTx(membershipRescId.asMemento(changeRelationInstant), OTHER_HAS_MEMBER, member2Id);

        final var member1Created = Instant.parse("2019-11-15T12:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, member1Created);

        membershipService.commitTransaction(txId);

        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE, changeRelationInstant);

        membershipService.populateMembershipHistory(txId, dcId);

        membershipService.commitTransaction(txId);

        // No membership before member added
        assertCommittedMembershipCount(membershipRescId.asMemento(Instant.parse("2019-11-13T12:00:00.0Z")), 0);
        assertHasMembersNoTx(membershipRescId.asMemento(changeRelationInstant), OTHER_HAS_MEMBER, member2Id);
        assertHasMembersNoTx(membershipRescId.asMemento(member1Created), OTHER_HAS_MEMBER,
                member1Id, member2Id);
        assertHasMembersNoTx(membershipRescId.asMemento(member1Created), OTHER_HAS_MEMBER,
                member1Id, member2Id);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);
    }

    @Test
    public void populateMembershipHistory_DC_ChangeRelation_AddMemberBefore() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        final var member2Created = Instant.parse("2019-11-13T12:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        membershipService.commitTransaction(txId);

        // Change the membership relation
        final var changeRelationInstant = Instant.parse("2019-11-14T12:00:00.0Z");
        final var dcAtChangeRelation = dcId.asMemento(changeRelationInstant);

        // Mock triples change for changed DC
        mockGetTriplesForDC(dcId, changeRelationInstant, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(txId, dcAtChangeRelation, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, changeRelationInstant), rootId);
        membershipService.resourceModified(txId, dcId);

        membershipService.commitTransaction(txId);

        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE, changeRelationInstant);

        membershipService.populateMembershipHistory(txId, dcId);

        membershipService.commitTransaction(txId);

        // No membership before creation
        assertCommittedMembershipCount(membershipRescId.asMemento(Instant.parse("2019-01-01T12:00:00.0Z")), 0);
        assertHasMembersNoTx(membershipRescId.asMemento(CREATED_DATE), RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembersNoTx(membershipRescId.asMemento(changeRelationInstant), OTHER_HAS_MEMBER,
                member1Id, member2Id);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);
    }

    @Test
    public void populateMembershipHistory_DC_ChangedRelation_DeleteMemberBefore() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(txId, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(txId, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(txId);

        final var member2Created = Instant.parse("2019-11-13T12:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        // Delete one of the members
        final var deleteInstant = Instant.parse("2019-11-13T20:00:00.0Z");
        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER, CREATED_DATE, deleteInstant);
        membershipService.resourceDeleted(txId, member1Id);

        membershipService.commitTransaction(txId);

        // Change the membership relation
        final var changeRelationInstant = Instant.parse("2019-11-14T12:00:00.0Z");
        final var dcAtChangeRelation = dcId.asMemento(changeRelationInstant);

        // Mock triples change for changed DC
        mockGetTriplesForDC(dcId, changeRelationInstant, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(txId, dcAtChangeRelation, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, changeRelationInstant), rootId);
        membershipService.resourceModified(txId, dcId);

        membershipService.commitTransaction(txId);

        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE, changeRelationInstant);

        membershipService.populateMembershipHistory(txId, dcId);

        membershipService.commitTransaction(txId);

        // No membership before creation
        assertCommittedMembershipCount(membershipRescId.asMemento(Instant.parse("2019-01-01T12:00:00.0Z")), 0);
        // Member 1 is deleted before relationship change, but after member 2 is created
        assertHasMembersNoTx(membershipRescId.asMemento(CREATED_DATE), RdfLexicon.LDP_MEMBER,
                member1Id);
        assertHasMembersNoTx(membershipRescId.asMemento(member2Created), RdfLexicon.LDP_MEMBER,
                member1Id, member2Id);
        // Member 2 exists before and after the relation change, so its history contains both
        assertHasMembersNoTx(membershipRescId.asMemento(deleteInstant), RdfLexicon.LDP_MEMBER,
                member2Id);
        assertHasMembersNoTx(membershipRescId.asMemento(changeRelationInstant), OTHER_HAS_MEMBER,
                member2Id);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member2Id);
    }

    private void mockListVersion(final FedoraId fedoraId, final Instant... versions) {
        when(psSession.listVersions(fedoraId.asResourceId())).thenReturn(Arrays.asList(versions));
    }

    private void assertHasMembersNoTx(final FedoraId membershipRescId,
            final Property hasMemberRelation, final FedoraId... memberIds) {
        assertHasMembers(shortLivedTx, membershipRescId, hasMemberRelation, memberIds);
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

    private void assertIsMemberOfNoTx(final FedoraId memberId, final Property isMemberOf,
            final FedoraId membershipRescId) {
        assertIsMemberOf(shortLivedTx, memberId, isMemberOf, membershipRescId);
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
        mockGetHeaders(txId, headers.getId(), headers, rootId);
    }

    private void mockGetHeaders(final String txId, final FedoraId fedoraId, final ResourceHeaders headers,
            final FedoraId parentId) {
        when(psSession.getHeaders(eq(fedoraId), nullable(Instant.class))).thenReturn(headers);
        if (!fedoraId.isMemento()) {
            when(psSession.getHeaders(eq(headers.getId().asMemento(headers.getCreatedDate())),
                    nullable(Instant.class))).thenReturn(headers);
        }
        containmentIndex.addContainedBy(txId, parentId, fedoraId);
    }

    private void mockGetHeaders(final String txId, final FedoraId fedoraId, final FedoraId parentId,
            final Resource ixModel, final Instant createdDate, final Instant lastModified) {
        final var headers = populateHeaders(fedoraId, parentId, ixModel, createdDate, lastModified);
        mockGetHeaders(txId, fedoraId, headers, parentId);
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

    private FedoraId createDirectContainer(final FedoraId parentId, final FedoraId membershipRescId,
            final Property relation, final boolean useIsMemberOf) {
        final var dcId = mintFedoraId();
        mockGetHeaders(populateHeaders(dcId, parentId, RdfLexicon.DIRECT_CONTAINER));
        mockGetTriplesForDC(dcId, CREATED_DATE, membershipRescId, relation, useIsMemberOf);
        return dcId;
    }

    private void mockGetTriplesForDCHead(final FedoraId dcId, final Instant startTime, final FedoraId membershipRescId,
            final Property relation, final boolean useIsMemberOf) {
        mockGetTriplesForDC(dcId, startTime, membershipRescId, relation, useIsMemberOf, true);
    }

    private void mockGetTriplesForDC(final FedoraId dcId, final Instant startTime, final FedoraId membershipRescId,
            final Property relation, final boolean useIsMemberOf) {
        mockGetTriplesForDC(dcId, startTime, membershipRescId, relation, useIsMemberOf, false);
    }

    private void mockGetTriplesForDC(final FedoraId dcId, final Instant startTime, final FedoraId membershipRescId,
            final Property relation, final boolean useIsMemberOf, final boolean isHead) {
        final var model = ModelFactory.createDefaultModel();
        final var dcRdfResc = model.getResource(dcId.getBaseId());
        final var membershipRdfResc = model.getResource(membershipRescId.getFullId());
        dcRdfResc.addProperty(RdfLexicon.MEMBERSHIP_RESOURCE, membershipRdfResc);
        if (relation != null) {
            if (useIsMemberOf) {
                dcRdfResc.addProperty(RdfLexicon.IS_MEMBER_OF_RELATION, relation);
            } else {
                dcRdfResc.addProperty(RdfLexicon.HAS_MEMBER_RELATION, relation);
            }
        }

        mockGetTriplesForDC(dcId, null, model);
        if (!isHead) {
            mockGetTriplesForDC(dcId, startTime, model);
        }
    }

    private void mockGetTriplesForDC(final FedoraId dcId, final Instant startTime, final Model model) {
        when(psSession.getTriples(eq(dcId), eq(startTime))).thenAnswer(new Answer<RdfStream>() {
            @Override
            public RdfStream answer(final InvocationOnMock invocation) throws Throwable {
                return fromModel(model.getResource(dcId.getBaseId()).asNode(), model);
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
        final var results = membershipService.getMembership(shortLivedTx, subjectId);
        assertEquals("Incorrect number of committed membership properties for " + subjectId,
                expected, results.count());
    }

    private void assertUncommittedMembershipCount(final String txId, final FedoraId subjectId, final int expected) {
        final var results = membershipService.getMembership(txId, subjectId);
        assertEquals("Incorrect number of uncommitted membership properties for " + subjectId,
                expected, results.count());
    }
}
