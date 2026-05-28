/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.vocabulary.VOID.property;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.impl.TestTransactionHelper;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("/membershipServiceTest.xml")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
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

    public static final Property PROXY_FOR = createProperty("http://example.com/proxyFor");

    private static final String RELATIVE_RESOURCE_PATH = "some/ocfl/path/v1/content/.fcrepo/fcr-container.json";

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

    private Transaction transaction;

    private Transaction shortLivedTx;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    private FedoraId membershipRescId;

    private String txId;
    private String shortLivedTxId;

    private Transaction readOnlyTx;

    @BeforeEach
    @FlywayTest
    public void setup() {
        MockitoAnnotations.openMocks(this);

        txId = UUID.randomUUID().toString();
        transaction = TestTransactionHelper.mockTransaction(txId, false);
        shortLivedTxId = UUID.randomUUID().toString();
        shortLivedTx = TestTransactionHelper.mockTransaction(shortLivedTxId, true);

        when(pSessionManager.getSession(transaction)).thenReturn(psSession);
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
        readOnlyTx = ReadOnlyTransaction.INSTANCE;
    }

    @Test
    public void getMembers_NoMembership() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        containmentIndex.addContainedBy(transaction, rootId, membershipRescId);
        membershipService.resourceCreated(transaction, membershipRescId);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(transaction, membershipRescId));
    }

    @Test
    public void getMembers_WithDC_NoMembers() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(transaction, membershipRescId));
    }

    @Test
    public void getMembers_WithDC_AddedMembers_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        final var lastUpdated = membershipService.getLastUpdatedTimestamp(transaction, membershipRescId);
        assertNotNull(lastUpdated);

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertEquals(lastUpdated, membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId),
                "Last updated timestamp should not change during commit");
    }

    @Test
    public void getMembers_WithDC_AddedMembers_DefaultHasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        // Don't specify a membership relation
        final var dcId = createDirectContainer(membershipRescId, null, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void getMembers_WithDC_AddedMembers_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertIsMemberOf(transaction, member1Id, MEMBER_OF, membershipRescId);
        assertIsMemberOf(transaction, member2Id, MEMBER_OF, membershipRescId);

        final var member1Updated = membershipService.getLastUpdatedTimestamp(transaction, member1Id);
        assertNotNull(member1Updated);
        final var member2Updated = membershipService.getLastUpdatedTimestamp(transaction, member2Id);
        assertNotNull(member2Updated);
        assertNull(membershipService.getLastUpdatedTimestamp(transaction, membershipRescId),
                "No membership expected for the membership resource");

        // Commit the transaction and verify we can still get the added members
        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);

        assertHasMembersNoTx(member1Id, MEMBER_OF, membershipRescId);
        assertHasMembersNoTx(member2Id, MEMBER_OF, membershipRescId);

        assertEquals(member1Updated, membershipService.getLastUpdatedTimestamp(readOnlyTx, member1Id));
        assertEquals(member2Updated, membershipService.getLastUpdatedTimestamp(readOnlyTx, member2Id));
    }

    @Test
    public void getMembers_WithDC_BinaryAsMembershipResc() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, RdfLexicon.NON_RDF_SOURCE));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        final var descId = membershipRescId.asDescription();

        assertHasMembers(transaction, descId, RdfLexicon.LDP_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(descId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void deleteMember_InDC_AddedInSameTx_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(transaction, membershipRescId, 1);

        assertNotNull(membershipService.getLastUpdatedTimestamp(transaction, membershipRescId));

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(transaction, member1Id);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(transaction, membershipRescId));

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);

        assertNull(membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId));
    }

    @Test
    public void deleteExistingMember_InDC_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);

        final var lastUpdated = membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId);
        assertNotNull(lastUpdated);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(transaction, member1Id);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);

        final var afterDeleteUpdated = membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId);
        assertNotNull(afterDeleteUpdated);
        assertNotEquals(lastUpdated, afterDeleteUpdated);
    }

    @Test
    public void deleteExistingMember_InDC_MultipleMembers_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 2);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(transaction, member1Id);

        assertUncommittedMembershipCount(transaction, membershipRescId, 1);
        assertCommittedMembershipCount(membershipRescId, 2);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
    }

    @Test
    public void deleteExistingMember_InDC_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(member1Id, 1);

        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Notify that the member was deleted
        membershipService.resourceDeleted(transaction, member1Id);

        assertCommittedMembershipCount(member1Id, 1);
        assertUncommittedMembershipCount(transaction, member1Id, 0);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(member1Id, 0);
    }

    @Test
    public void deleteDC_WithMember_CreatedInSameTx() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        createDCMember(dcId, BASIC_CONTAINER);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertUncommittedMembershipCount(transaction, membershipRescId, 1);

        mockDeleteHeaders(dcId, rootId, RdfLexicon.DIRECT_CONTAINER);

        // Notify that the DC was deleted
        membershipService.resourceDeleted(transaction, dcId);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void deleteExistingDC_WithExistingMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);

        mockDeleteHeaders(dcId, rootId, RdfLexicon.DIRECT_CONTAINER);
        // Notify that the DC was deleted
        membershipService.resourceDeleted(transaction, dcId);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void deleteExistingMemberAndDC_InSameTx() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        mockDeleteHeaders(dcId, rootId, RdfLexicon.DIRECT_CONTAINER);
        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER);
        // Delete the member
        membershipService.resourceDeleted(transaction, member1Id);
        // Delete the DC itself
        membershipService.resourceDeleted(transaction, dcId);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertUncommittedMembershipCount(transaction, membershipRescId, 0);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void purgeDC() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);

        when(psSession.getHeaders(eq(dcId), nullable(Instant.class))).thenThrow(
                new PersistentItemNotFoundException(""));

        membershipService.resourceDeleted(transaction, dcId);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
    }

    @Test
    public void purgeMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 2);

        final var lastUpdated = membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId);
        assertNotNull(lastUpdated);

        when(psSession.getHeaders(eq(member1Id), nullable(Instant.class))).thenThrow(
                new PersistentItemNotFoundException(""));

        membershipService.resourceDeleted(transaction, member1Id);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member2Id);
        assertUncommittedMembershipCount(transaction, membershipRescId, 1);

        assertEquals(lastUpdated, membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId));
    }

    @Test
    public void purgeMembershipResource_isMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(member1Id, 1);

        when(psSession.getHeaders(eq(membershipRescId), nullable(Instant.class))).thenThrow(
                new PersistentItemNotFoundException(""));

        membershipService.resourceDeleted(transaction, membershipRescId);

        assertCommittedMembershipCount(member1Id, 0);
        assertUncommittedMembershipCount(transaction, member1Id, 0);
    }

    @Test
    public void recreateExistingMember_InDC_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);

        // Notify that the member was deleted
        membershipService.resourceDeleted(transaction, member1Id);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 1);

        // Recreate the resource in the same TX
        membershipService.resourceCreated(transaction, member1Id);

        assertUncommittedMembershipCount(transaction, membershipRescId, 1);
        assertCommittedMembershipCount(membershipRescId, 1);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);
    }

    @Test
    public void getMembers_MultipleDCsSameMembershipResource_HasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dc1Id = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dc1Id);

        // Add a child to the outer DC
        final var member1Id = createDCMember(dc1Id, BASIC_CONTAINER);

        final var dc2Id = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dc2Id);

        // Add a child to the outer DC
        final var member2Id = createDCMember(dc2Id, BASIC_CONTAINER);

        assertUncommittedMembershipCount(transaction, membershipRescId, 2);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Delete one to ensure only those members are cleaned up
        membershipService.resourceDeleted(transaction, member2Id);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void getMembers_MultipleDCsSameMembershipResource_IsMemberOfRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dc1Id = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dc1Id);

        // Add a child to the outer DC
        final var member1Id = createDCMember(dc1Id, BASIC_CONTAINER);

        final var dc2Id = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dc2Id);

        // Add a child to the outer DC
        final var member2Id = createDCMember(dc2Id, BASIC_CONTAINER);

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertUncommittedMembershipCount(transaction, member1Id, 1);
        assertUncommittedMembershipCount(transaction, member2Id, 1);

        membershipService.commitTransaction(transaction);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
        assertIsMemberOfNoTx(member2Id, MEMBER_OF, membershipRescId);

        // Delete one to ensure only those members are cleaned up
        membershipService.resourceDeleted(transaction, member2Id);

        membershipService.commitTransaction(transaction);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
        assertCommittedMembershipCount(member2Id, 0);
    }

    @Test
    public void getMembers_DCmemberOfDC() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        // Add a child to the outer DC
        final var outerMemberId = createDCMember(dcId, BASIC_CONTAINER);

        // Add a DC as the child of the first DC
        final var nestedDcId = createDirectContainer(dcId, membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, nestedDcId);

        // Add a child to the nested DC
        final var nestedMemberId = createDCMember(nestedDcId, BASIC_CONTAINER);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertUncommittedMembershipCount(transaction, membershipRescId, 3);

        // Asserting membership can be found from the perspective of each member
        assertCommittedMembershipByObjectCount(outerMemberId, 0);
        assertUncommittedMembershipByObjectCount(transaction, outerMemberId, 1);
        assertCommittedMembershipByObjectCount(nestedDcId, 0);
        assertUncommittedMembershipByObjectCount(transaction, nestedDcId, 1);
        assertCommittedMembershipByObjectCount(nestedMemberId, 0);
        assertUncommittedMembershipByObjectCount(transaction, nestedMemberId, 1);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, outerMemberId, nestedDcId, nestedMemberId);

        // Delete the nested DC to ensure that it gets cleaned up as both a DC and a member
        membershipService.resourceDeleted(transaction, nestedDcId);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, outerMemberId);
    }

    @Test
    public void changeMembershipResource_ForDC() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var membershipResc2Id = mintFedoraId();
        mockGetHeaders(populateHeaders(membershipResc2Id, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipResc2Id);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 2);
        assertCommittedMembershipCount(membershipResc2Id, 0);

        final var msRescUpdated = membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId);
        assertNotNull(msRescUpdated);
        assertNull(membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipResc2Id));

        // Change the membership resource for the DC
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipResc2Id, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceModified(transaction, dcId);

        assertHasMembers(transaction, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        final var msRescUpdatedAfter = membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipRescId);
        assertNotNull(msRescUpdatedAfter);
        assertNotEquals(msRescUpdated, msRescUpdatedAfter,
                "First membership resc should have changed last_updated timestamp");
        assertNotNull(membershipService.getLastUpdatedTimestamp(readOnlyTx, membershipResc2Id));
    }

    @Test
    public void changeMembershipResource_ForDC_ManualVersioning() throws Exception {
        setField(propsConfig, "autoVersioningEnabled", Boolean.FALSE);

        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var membershipResc2Id = mintFedoraId();
        mockGetHeaders(populateHeaders(membershipResc2Id, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipResc2Id);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertCommittedMembershipCount(membershipResc2Id, 0);

        // Change the membership resource for the DC without creating a version
        mockListVersion(dcId);
        mockGetTriplesForDCHead(dcId, CREATED_DATE, membershipResc2Id, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceModified(transaction, dcId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(transaction, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);

        // Change membership property without versioning
        mockGetTriplesForDCHead(dcId, CREATED_DATE, membershipResc2Id, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(transaction, dcId);

        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(transaction, membershipResc2Id, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, OTHER_HAS_MEMBER, member1Id);

        // Create version from former head version
        final var versionChangeTime = Instant.parse("2019-11-13T12:00:00.0Z");
        mockListVersion(dcId, versionChangeTime);
        // New head state matches previous head state for the moment
        mockGetTriplesForDC(dcId, versionChangeTime, membershipResc2Id, OTHER_HAS_MEMBER, false);
        mockGetHeaders(transaction, dcId.asMemento(versionChangeTime), populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, versionChangeTime), rootId);

        // Change membership resource after having created version
        final var afterVersionChangeTime = Instant.parse("2019-11-13T14:00:00.0Z");
        mockGetHeaders(transaction, dcId, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, afterVersionChangeTime), rootId);
        mockGetTriplesForDCHead(dcId, afterVersionChangeTime, membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(transaction, dcId);

        // Membership resc 2 should still have a member prior to the version creation/last property update
        assertHasMembers(transaction, membershipResc2Id.asMemento(CREATED_DATE), OTHER_HAS_MEMBER,
                member1Id);
        assertUncommittedMembershipCount(transaction, membershipResc2Id, 0);
        assertHasMembersNoTx(membershipResc2Id, OTHER_HAS_MEMBER, member1Id);
        assertHasMembers(transaction, membershipRescId, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipResc2Id, 0);
        assertHasMembersNoTx(membershipResc2Id.asMemento(CREATED_DATE), OTHER_HAS_MEMBER,
                member1Id);
        assertCommittedMembershipCount(membershipRescId.asMemento(CREATED_DATE), 0);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id);
    }

    @Test
    public void changeMembershipRelation_DC_HasMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(transaction, membershipRescId, 2);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        // Change the membership relation
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceModified(transaction, dcId);

        assertHasMembers(transaction, membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id, member2Id);
    }

    @Test
    public void changeResource_DC_HasMemberToIsMemberOf() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        assertUncommittedMembershipCount(transaction, membershipRescId, 1);

        membershipService.commitTransaction(transaction);

        assertHasMembers(readOnlyTx, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var msRescUpdated1 = indexManager.getLastUpdated(readOnlyTx, membershipRescId);
        assertNotNull(msRescUpdated1);
        assertNull(indexManager.getLastUpdated(readOnlyTx, member1Id));

        // Change the membership direction from a ldp:hasMemberRelation to a ldp:isMemberOfRelation
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, MEMBER_OF, true);
        membershipService.resourceModified(transaction, dcId);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertIsMemberOf(transaction, member1Id, MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(transaction);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);

        final var memRescUpdated1 = indexManager.getLastUpdated(readOnlyTx, member1Id);
        assertNotNull(memRescUpdated1);
        final var msRescUpdated2 = indexManager.getLastUpdated(readOnlyTx, membershipRescId);
        assertNotNull(msRescUpdated2);
        assertNotEquals(msRescUpdated1, msRescUpdated2);

        // Reverse the membership direction again
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE2, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(transaction, dcId, rootId, RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, LAST_MODIFIED_DATE2);
        membershipService.resourceModified(transaction, dcId);

        assertCommittedMembershipCount(member1Id, 1);
        assertUncommittedMembershipCount(transaction, member1Id, 0);
        assertHasMembers(transaction, membershipRescId, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(member1Id, 0);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id);

        final var memRescUpdated2 = indexManager.getLastUpdated(readOnlyTx, member1Id);
        assertNotNull(memRescUpdated2);
        assertNotEquals(memRescUpdated1, memRescUpdated2);
        final var msRescUpdated3 = indexManager.getLastUpdated(readOnlyTx, membershipRescId);
        assertNotNull(msRescUpdated3);
        assertNotEquals(msRescUpdated2, msRescUpdated3);
    }

    @Test
    public void changeResource_DC_IsMemberOf() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);

        // Switch DC to a different ldp:isMemberOfRelation
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, OTHER_MEMBER_OF, true);
        membershipService.resourceModified(transaction, dcId);

        assertIsMemberOf(transaction, member1Id, OTHER_MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(transaction);

        assertIsMemberOfNoTx(member1Id, OTHER_MEMBER_OF, membershipRescId);

        // Switch back again
        mockGetTriplesForDC(dcId, LAST_MODIFIED_DATE, membershipRescId, MEMBER_OF, true);
        membershipService.resourceModified(transaction, dcId);

        assertIsMemberOf(transaction, member1Id, MEMBER_OF, membershipRescId);

        membershipService.commitTransaction(transaction);

        assertIsMemberOfNoTx(member1Id, MEMBER_OF, membershipRescId);
    }

    @Test
    public void getMementoMembership_AllCreatedAtSameTime_NoChanges() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        final var beforeCreated = Instant.parse("2019-11-10T00:00:00.0Z");
        final var beforeCreatedId = membershipRescId.asMemento(beforeCreated);

        final var afterLastModified = Instant.parse("2019-12-10T00:00:00.0Z");
        final var afterLastModifiedId = membershipRescId.asMemento(afterLastModified);

        // No membership before creation time
        assertUncommittedMembershipCount(transaction, beforeCreatedId, 0);
        assertUncommittedMembershipCount(transaction, membershipRescId, 1);
        assertUncommittedMembershipCount(transaction, afterLastModifiedId, 1);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(beforeCreatedId, 0);
        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertCommittedMembershipCount(afterLastModifiedId, 1);
    }

    @Test
    public void getMementoMembership_OneMembershipAddition_hasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var headMementoId = membershipRescId.asMemento(LAST_MODIFIED_DATE);

        final var beforeAddMementoInstant = Instant.parse("2019-11-12T12:00:00.0Z");
        final var beforeAddMementoId = membershipRescId.asMemento(beforeAddMementoInstant);
        mockGetHeaders(populateHeaders(membershipRescId, rootId, BASIC_CONTAINER, CREATED_DATE,
                beforeAddMementoInstant));

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var memberCreated = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, memberCreated);

        // No membership at first memento timestamp
        assertUncommittedMembershipCount(transaction, beforeAddMementoId, 0);
        assertUncommittedMembershipCount(transaction, membershipRescId, 1);

        membershipService.commitTransaction(transaction);

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
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, MEMBER_OF, true);
        membershipService.resourceCreated(transaction, dcId);

        final var memberCreated = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, memberCreated);
        final var memberCreatedId = member1Id.asMemento(memberCreated);
        final var beforeCreate = Instant.parse("2019-11-10T00:00:00.0Z");
        final var beforeCreateId = member1Id.asMemento(beforeCreate);

        assertUncommittedMembershipCount(transaction, beforeCreateId, 0);
        assertUncommittedMembershipCount(transaction, member1Id, 1);

        membershipService.commitTransaction(transaction);

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

        membershipService.resourceDeleted(transaction, member1Id);

        // Make sure delete hasn't leaked
        assertCommittedMembershipCount(member1Id, 1);

        assertIsMemberOf(transaction, memberCreatedId, MEMBER_OF, membershipRescId);
        assertUncommittedMembershipCount(transaction, deletedMemberId, 0);
        assertUncommittedMembershipCount(transaction, afterDeleteMemberId, 0);
        assertUncommittedMembershipCount(transaction, member1Id, 0);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(beforeCreateId, 0);
        assertIsMemberOfNoTx(memberCreatedId, MEMBER_OF, membershipRescId);
        assertCommittedMembershipCount(deletedMemberId, 0);
        assertCommittedMembershipCount(afterDeleteMemberId, 0);
        assertCommittedMembershipCount(member1Id, 0);
    }

    @Test
    public void getMementoMembership_AddAndDelete_hasMemberRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var beforeAddMementoInstant = Instant.parse("2019-11-12T12:00:00.0Z");
        final var beforeAddMementoId = membershipRescId.asMemento(beforeAddMementoInstant);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Created = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, member1Created);

        final var member2Created = Instant.parse("2019-11-12T20:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        final var membershipRescAtMember1Create1Id = membershipRescId.asMemento(member1Created);
        final var membershipRescAtMember2Create1Id = membershipRescId.asMemento(member2Created);

        // No membership before members added
        assertUncommittedMembershipCount(transaction, beforeAddMementoId, 0);
        // Check membership at the times members were added
        assertUncommittedMembershipCount(transaction, membershipRescAtMember1Create1Id, 1);
        assertUncommittedMembershipCount(transaction, membershipRescAtMember2Create1Id, 2);
        assertUncommittedMembershipCount(transaction, membershipRescId, 2);

        membershipService.commitTransaction(transaction);

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

        membershipService.resourceDeleted(transaction, member2Id);

        final var membershipRescAtDeleteId = membershipRescId.asMemento(deleteInstant);
        final var membershipRescAfterDeleteId = membershipRescId.asMemento(Instant.parse("2019-11-13T15:00:00.0Z"));

        assertUncommittedMembershipCount(transaction, membershipRescAtMember2Create1Id, 2);
        assertUncommittedMembershipCount(transaction, membershipRescAtDeleteId, 1);
        assertUncommittedMembershipCount(transaction, membershipRescAfterDeleteId, 1);

        membershipService.commitTransaction(transaction);

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
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.rollbackTransaction(transaction);

        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        // Commit the transaction and verify the non-rollback entries persist
        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void resetMembershipIndex() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);

        membershipService.reset();

        assertUncommittedMembershipCount(transaction, membershipRescId, 0);
        assertCommittedMembershipCount(membershipRescId, 0);
    }

    @Test
    public void clearAllTransactionsMembershipIndex() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertCommittedMembershipCount(membershipRescId, 1);

        final var member2Id = createDCMember(dcId, RdfLexicon.NON_RDF_SOURCE);

        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id, member2Id);
        assertUncommittedMembershipCount(transaction, membershipRescId, 2);

        membershipService.clearAllTransactions();

        assertUncommittedMembershipCount(transaction, membershipRescId, 1);
        assertCommittedMembershipCount(membershipRescId, 1);
        assertHasMembers(transaction, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
    }

    @Test
    public void populateMembershipHistory_DC_DeletedMember() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var beforeAddMementoInstant = Instant.parse("2019-11-12T12:00:00.0Z");
        final var beforeAddMementoId = membershipRescId.asMemento(beforeAddMementoInstant);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Created = Instant.parse("2019-11-12T13:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, member1Created);

        final var member2Created = Instant.parse("2019-11-12T20:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        final var membershipRescAtMember1Create1Id = membershipRescId.asMemento(member1Created);
        final var membershipRescAtMember2Create1Id = membershipRescId.asMemento(member2Created);

        membershipService.commitTransaction(transaction);

        // Delete one of the members
        final var deleteInstant = Instant.parse("2019-11-13T12:00:00.0Z");
        mockDeleteHeaders(member2Id, dcId, BASIC_CONTAINER, member2Created, deleteInstant);

        membershipService.resourceDeleted(transaction, member2Id);

        final var membershipRescAtDeleteId = membershipRescId.asMemento(deleteInstant);
        final var membershipRescAfterDeleteId = membershipRescId.asMemento(Instant.parse("2019-11-13T15:00:00.0Z"));

        membershipService.commitTransaction(transaction);

        // Clear the index
        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE);

        // Repopulate index
        membershipService.populateMembershipHistory(transaction, dcId);

        membershipService.commitTransaction(transaction);

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
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        membershipService.commitTransaction(transaction);

        // Change the membership relation
        final var changeRelationInstant = Instant.parse("2019-11-14T12:00:00.0Z");
        final var dcAtChangeRelation = dcId.asMemento(changeRelationInstant);

        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, changeRelationInstant);

        mockGetTriplesForDC(dcId, changeRelationInstant, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(transaction, dcAtChangeRelation, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, changeRelationInstant), rootId);
        membershipService.resourceModified(transaction, dcId);

        membershipService.commitTransaction(transaction);

        assertHasMembersNoTx(membershipRescId.asMemento(changeRelationInstant), OTHER_HAS_MEMBER, member2Id);

        final var member1Created = Instant.parse("2019-11-15T12:00:00.0Z");
        final var member1Id = createDCMember(dcId, BASIC_CONTAINER, member1Created);

        membershipService.commitTransaction(transaction);

        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE, changeRelationInstant);

        membershipService.populateMembershipHistory(transaction, dcId);

        membershipService.commitTransaction(transaction);

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
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        final var member2Created = Instant.parse("2019-11-13T12:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        membershipService.commitTransaction(transaction);

        // Change the membership relation
        final var changeRelationInstant = Instant.parse("2019-11-14T12:00:00.0Z");
        final var dcAtChangeRelation = dcId.asMemento(changeRelationInstant);

        // Mock triples change for changed DC
        mockGetTriplesForDC(dcId, changeRelationInstant, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(transaction, dcAtChangeRelation, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, changeRelationInstant), rootId);
        membershipService.resourceModified(transaction, dcId);

        membershipService.commitTransaction(transaction);

        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE, changeRelationInstant);

        membershipService.populateMembershipHistory(transaction, dcId);

        membershipService.commitTransaction(transaction);

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
        membershipService.resourceCreated(transaction, membershipRescId);

        final var dcId = createDirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, dcId);

        final var member1Id = createDCMember(dcId, BASIC_CONTAINER);

        membershipService.commitTransaction(transaction);

        final var member2Created = Instant.parse("2019-11-13T12:00:00.0Z");
        final var member2Id = createDCMember(dcId, BASIC_CONTAINER, member2Created);

        // Delete one of the members
        final var deleteInstant = Instant.parse("2019-11-13T20:00:00.0Z");
        mockDeleteHeaders(member1Id, dcId, BASIC_CONTAINER, CREATED_DATE, deleteInstant);
        membershipService.resourceDeleted(transaction, member1Id);

        membershipService.commitTransaction(transaction);

        // Change the membership relation
        final var changeRelationInstant = Instant.parse("2019-11-14T12:00:00.0Z");
        final var dcAtChangeRelation = dcId.asMemento(changeRelationInstant);

        // Mock triples change for changed DC
        mockGetTriplesForDC(dcId, changeRelationInstant, membershipRescId, OTHER_HAS_MEMBER, false);
        mockGetHeaders(transaction, dcAtChangeRelation, populateHeaders(dcId, rootId,
                RdfLexicon.DIRECT_CONTAINER, CREATED_DATE, changeRelationInstant), rootId);
        membershipService.resourceModified(transaction, dcId);

        membershipService.commitTransaction(transaction);

        membershipService.reset();

        mockListVersion(dcId, CREATED_DATE, changeRelationInstant);

        membershipService.populateMembershipHistory(transaction, dcId);

        membershipService.commitTransaction(transaction);

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

    @Test
    public void changeMembershipResource_ForIDC_ManualVersioning() throws Exception {
        setField(propsConfig, "autoVersioningEnabled", Boolean.FALSE);

        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var membershipResc2Id = mintFedoraId();
        mockGetHeaders(populateHeaders(membershipResc2Id, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipResc2Id);

        final var idcId = createIndirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, idcId);

        final var member1Id = createDCMember(rootId, BASIC_CONTAINER);

        createProxy(idcId, member1Id, CREATED_DATE, true);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 1);
        assertCommittedMembershipCount(membershipResc2Id, 0);

        // Change the membership resource for the IDC without creating a version
        mockListVersion(idcId);
        mockGetTriplesForDC(idcId, CREATED_DATE, membershipResc2Id, RdfLexicon.LDP_MEMBER, false, PROXY_FOR, true);
        membershipService.resourceModified(transaction, idcId);

        assertHasMembersNoTx(membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(transaction, membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);

        // Change membership property without versioning
        mockGetTriplesForDC(idcId, CREATED_DATE, membershipResc2Id, OTHER_HAS_MEMBER, false, PROXY_FOR, true);
        membershipService.resourceModified(transaction, idcId);

        assertHasMembersNoTx(membershipResc2Id, RdfLexicon.LDP_MEMBER, member1Id);
        assertHasMembers(transaction, membershipResc2Id, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipRescId, 0);
        assertHasMembersNoTx(membershipResc2Id, OTHER_HAS_MEMBER, member1Id);

        // Create version from former head version
        final var versionChangeTime = Instant.parse("2019-11-13T12:00:00.0Z");
        mockListVersion(idcId, versionChangeTime);
        // New head state matches previous head state for the moment
        mockGetTriplesForDC(idcId, versionChangeTime, membershipResc2Id, OTHER_HAS_MEMBER, false, PROXY_FOR, false);
        mockGetHeaders(transaction, idcId.asMemento(versionChangeTime), populateHeaders(idcId, rootId,
                RdfLexicon.INDIRECT_CONTAINER, CREATED_DATE, versionChangeTime), rootId);

        // Change membership resource after having created version
        final var afterVersionChangeTime = Instant.parse("2019-11-13T14:00:00.0Z");
        mockGetHeaders(transaction, idcId, populateHeaders(idcId, rootId,
                RdfLexicon.INDIRECT_CONTAINER, CREATED_DATE, afterVersionChangeTime), rootId);
        mockGetTriplesForDC(idcId, afterVersionChangeTime, membershipRescId, OTHER_HAS_MEMBER, false, PROXY_FOR, true);
        membershipService.resourceModified(transaction, idcId);

        // Membership resc 2 should still have a member prior to the version creation/last property update
        assertHasMembers(transaction, membershipResc2Id.asMemento(CREATED_DATE), OTHER_HAS_MEMBER,
                member1Id);
        assertUncommittedMembershipCount(transaction, membershipResc2Id, 0);
        assertHasMembersNoTx(membershipResc2Id, OTHER_HAS_MEMBER, member1Id);
        assertHasMembers(transaction, membershipRescId, OTHER_HAS_MEMBER, member1Id);

        membershipService.commitTransaction(transaction);

        assertCommittedMembershipCount(membershipResc2Id, 0);
        assertHasMembersNoTx(membershipResc2Id.asMemento(CREATED_DATE), OTHER_HAS_MEMBER,
                member1Id);
        assertCommittedMembershipCount(membershipRescId.asMemento(CREATED_DATE), 0);
        assertHasMembersNoTx(membershipRescId, OTHER_HAS_MEMBER, member1Id);
    }


    @Test
    public void getMembership_MemberProxiedInMultipleIDCs() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        // Create the target which will be member of two IDCs
        final var member1Id = createDCMember(rootId, BASIC_CONTAINER);

        // Create first IDC
        final var idcId = createIndirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, idcId);

        createProxy(idcId, member1Id, CREATED_DATE, true);

        assertUncommittedMembershipCount(transaction, membershipRescId, 1);
        assertCommittedMembershipCount(membershipRescId, 0);
        membershipService.commitTransaction(transaction);
        assertCommittedMembershipCount(membershipRescId, 1);

        final var idcId2 = createIndirectContainer(membershipRescId, OTHER_HAS_MEMBER, false);
        membershipService.resourceCreated(transaction, idcId2);

        createProxy(idcId2, member1Id, CREATED_DATE, true);

        assertUncommittedMembershipCount(transaction, membershipRescId, 2);
        assertCommittedMembershipCount(membershipRescId, 1);
        membershipService.commitTransaction(transaction);
        assertCommittedMembershipCount(membershipRescId, 2);

        // Two relations to the same member. They have different properties, but we aren't checking that here
        assertHasMembersNoTx(membershipRescId, null, member1Id, member1Id);

        // Verify that membership from the objects perspective returns relations from both IDCs
        final var membershipByObjectList = getMembershipListByObject(transaction, member1Id);
        assertEquals(2, membershipByObjectList.size());
        assertContainsMembership(membershipByObjectList, membershipRescId, RdfLexicon.LDP_MEMBER, member1Id);
        assertContainsMembership(membershipByObjectList, membershipRescId, OTHER_HAS_MEMBER, member1Id);
    }

    @Test
    public void populateMembershipHistory_IDC_MissingInsertedContentRelation() throws Exception {
        mockGetHeaders(populateHeaders(membershipRescId, BASIC_CONTAINER));
        membershipService.resourceCreated(transaction, membershipRescId);

        final var idcId = createIndirectContainer(membershipRescId, RdfLexicon.LDP_MEMBER, false);
        membershipService.resourceCreated(transaction, idcId);

        // First member has a proxy missing the insertedContentRelation property
        final var proxyId = createDCMember(idcId, BASIC_CONTAINER);
        mockGetHeaders(transaction, proxyId, idcId, BASIC_CONTAINER, CREATED_DATE, LAST_MODIFIED_DATE);
        final var model = ModelFactory.createDefaultModel();
        mockGetTriplesForDC(proxyId, null, model);
        membershipService.resourceCreated(transaction, proxyId);

        // Second member is valid
        final var member2Id = createDCMember(rootId, BASIC_CONTAINER);
        createProxy(idcId, member2Id, CREATED_DATE, false);

        membershipService.reset();

        // Trigger population of membership history
        mockListVersion(idcId, CREATED_DATE);
        // First member proxy missing insertedContentRelation, so should be skipped but not throw an error
        membershipService.populateMembershipHistory(transaction, idcId);
        membershipService.commitTransaction(transaction);

        // Only the valid member should show up in the membership
        assertCommittedMembershipCount(membershipRescId, 1);
    }

    private void mockListVersion(final FedoraId fedoraId, final Instant... versions) {
        when(psSession.listVersions(fedoraId.asResourceId())).thenReturn(Arrays.asList(versions));
    }

    private void assertHasMembersNoTx(final FedoraId membershipRescId,
            final Property hasMemberRelation, final FedoraId... memberIds) {
        assertHasMembers(shortLivedTx, membershipRescId, hasMemberRelation, memberIds);
    }

    private void assertHasMembers(final Transaction transaction, final FedoraId membershipRescId,
            final Property hasMemberRelation, final FedoraId... memberIds) {
        final var membershipList = getMembershipList(transaction, membershipRescId);
        assertEquals(memberIds.length, membershipList.size());
        final var subjectId = membershipRescId.asBaseId();
        for (final FedoraId memberId : memberIds) {
            assertContainsMembership(membershipList, subjectId, hasMemberRelation, memberId);
            final FedoraId memberIdMemento;
            if (membershipRescId.isMemento()) {
                memberIdMemento = memberId.asMemento(membershipRescId.getMementoInstant());
            } else {
                memberIdMemento = memberId;
            }
            final var membershipByObjectList = getMembershipListByObject(transaction, memberIdMemento);
            assertContainsMembership(membershipByObjectList, subjectId, hasMemberRelation, memberId);
        }
    }

    private void assertIsMemberOfNoTx(final FedoraId memberId, final Property isMemberOf,
            final FedoraId membershipRescId) {
        assertIsMemberOf(shortLivedTx, memberId, isMemberOf, membershipRescId);
    }

    private void assertIsMemberOf(final Transaction transaction, final FedoraId memberId, final Property isMemberOf,
            final FedoraId membershipRescId) {
        final var membershipList = getMembershipList(transaction, memberId);
        assertEquals(1, membershipList.size());
        assertContainsMembership(membershipList, memberId.asBaseId(), isMemberOf, membershipRescId);
    }

    private List<Triple> getMembershipList(final Transaction transaction, final FedoraId fedoraId) {
        final var results = membershipService.getMembership(transaction, fedoraId);
        return results.collect(Collectors.toList());
    }

    private List<Triple> getMembershipListByObject(final Transaction transaction, final FedoraId objectId) {
        final var results = membershipService.getMembershipByObject(transaction, objectId);
        return results.collect(Collectors.toList());
    }

    private FedoraId mintFedoraId() {
        return FedoraId.create(UUID.randomUUID().toString());
    }

    private void mockGetHeaders(final ResourceHeaders headers) {
        mockGetHeaders(transaction, headers.getId(), headers, rootId);
    }

    private void mockGetHeaders(final Transaction transaction, final FedoraId fedoraId, final ResourceHeaders headers,
            final FedoraId parentId) {
        when(psSession.getHeaders(eq(fedoraId), nullable(Instant.class))).thenReturn(headers);
        if (!fedoraId.isMemento()) {
            when(psSession.getHeaders(eq(headers.getId().asMemento(headers.getCreatedDate())),
                    nullable(Instant.class))).thenReturn(headers);
        }
        containmentIndex.addContainedBy(transaction, parentId, fedoraId);
    }

    private void mockGetHeaders(final Transaction transaction, final FedoraId fedoraId, final FedoraId parentId,
            final Resource ixModel, final Instant createdDate, final Instant lastModified) {
        final var headers = populateHeaders(fedoraId, parentId, ixModel, createdDate, lastModified);
        mockGetHeaders(transaction, fedoraId, headers, parentId);
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
        mockGetHeaders(transaction, memberId, dcId, ixModel, CREATED_DATE, LAST_MODIFIED_DATE);
        membershipService.resourceCreated(transaction, memberId);
        return memberId;
    }

    private FedoraId createDCMember(final FedoraId dcId, final Resource ixModel, final Instant lastModified) {
        final var memberId = mintFedoraId();
        mockGetHeaders(transaction, memberId, dcId, ixModel, lastModified, lastModified);
        membershipService.resourceCreated(transaction, memberId);
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
        headers.setStorageRelativePath(RELATIVE_RESOURCE_PATH);
        return headers;
    }

    private FedoraId createIndirectContainer(final FedoraId membershipRescId, final Property relation,
            final boolean useIsMemberOf) {
        return createIndirectContainer(rootId, membershipRescId, relation, useIsMemberOf);
    }

    private FedoraId createIndirectContainer(final FedoraId parentId, final FedoraId membershipRescId,
            final Property relation, final boolean useIsMemberOf) {
        final var dcId = mintFedoraId();
        mockGetHeaders(populateHeaders(dcId, parentId, RdfLexicon.INDIRECT_CONTAINER));
        mockGetTriplesForDC(dcId, CREATED_DATE, membershipRescId, relation, useIsMemberOf, PROXY_FOR, false);
        return dcId;
    }

    private FedoraId createProxy(final FedoraId idcId, final FedoraId memberId,
            final Instant lastModified, final boolean isHead) {
        final var proxyId = mintFedoraId();
        mockGetHeaders(transaction, proxyId, idcId, BASIC_CONTAINER, lastModified, lastModified);
        final var model = ModelFactory.createDefaultModel();
        final var proxyRdfResc = model.getResource(proxyId.getBaseId());
        final var memberRdfResc = model.getResource(memberId.getFullId());
        proxyRdfResc.addProperty(PROXY_FOR, memberRdfResc);
        mockGetTriplesForDC(proxyId, null, model);
        if (!isHead) {
            mockGetTriplesForDC(proxyId, lastModified, model);
        }
        membershipService.resourceCreated(transaction, proxyId);
        return memberId;
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
        mockGetTriplesForDC(dcId, startTime, membershipRescId, relation, useIsMemberOf, null, isHead);
    }

    private void mockGetTriplesForDC(final FedoraId dcId, final Instant startTime, final FedoraId membershipRescId,
            final Property relation, final boolean useIsMemberOf, final Property insertedContentRelation,
            final boolean isHead) {
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
        if (insertedContentRelation != null) {
            dcRdfResc.addProperty(RdfLexicon.INSERTED_CONTENT_RELATION, insertedContentRelation);
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

        assertTrue(membershipList.stream().anyMatch(t -> t.getSubject().equals(subjectNode)
                        && (property == null || t.getPredicate().equals(property.asNode()))
                        && t.getObject().equals(objectNode)),
                "Membership set did not contain: " + subjectId + " " + property + " " + objectId);
    }

    private void assertCommittedMembershipCount(final FedoraId subjectId, final int expected) {
        final var results = membershipService.getMembership(shortLivedTx, subjectId);
        assertEquals(expected, results.count(),
                "Incorrect number of committed membership properties for " + subjectId);
    }

    private void assertCommittedMembershipByObjectCount(final FedoraId objectId, final int expected) {
        final var results = membershipService.getMembershipByObject(shortLivedTx, objectId);
        assertEquals(expected, results.count(),
                "Incorrect number of committed membership properties for object " + objectId);
    }

    private void assertUncommittedMembershipCount(final Transaction transaction,
                                                  final FedoraId subjectId,
                                                  final int expected) {
        final var results = membershipService.getMembership(transaction, subjectId);
        assertEquals(expected, results.count(),
                "Incorrect number of uncommitted membership properties for " + subjectId);
    }

    private void assertUncommittedMembershipByObjectCount(final Transaction transaction,
                                                  final FedoraId objectId,
                                                  final int expected) {
        final var results = membershipService.getMembershipByObject(transaction, objectId);
        assertEquals(expected, results.count(),
                "Incorrect number of uncommitted membership properties for object " + objectId);
    }
}
