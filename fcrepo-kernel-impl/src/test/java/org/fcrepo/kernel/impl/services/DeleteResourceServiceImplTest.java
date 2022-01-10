/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.impl.TestTransactionHelper;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperation;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.search.api.SearchIndex;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * DeleteResourceServiceTest
 *
 * @author dbernstein
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class DeleteResourceServiceImplTest {

    private static final String USER = "fedoraAdmin";

    private Transaction tx;

    @Mock
    private EventAccumulator eventAccumulator;

    @Mock
    private PersistentStorageSession pSession;

    @Inject
    private ContainmentIndex containmentIndex;

    @Mock
    private SearchIndex searchIndex;

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private Container container;

    @Mock
    private Container childContainer;

    @Mock
    private Binary binary;

    @Mock
    private WebacAcl acl;

    @Mock
    private NonRdfSourceDescription binaryDesc;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private ResourceHeaders resourceHeaders;
    @Mock
    private ResourceHeaders childHeaders;
    @Mock
    private ResourceHeaders descHeaders;
    @Mock
    private ResourceHeaders aclHeaders;

    @Mock
    private Cache<String, Optional<ACLHandle>> authHandleCache;

    @Captor
    private ArgumentCaptor<DeleteResourceOperation> operationCaptor;

    @InjectMocks
    private DeleteResourceServiceImpl service;

    private static final FedoraId RESOURCE_ID = FedoraId.create("test-resource");
    private static final FedoraId CHILD_RESOURCE_ID = RESOURCE_ID.resolve("test-resource-child");
    private static final FedoraId RESOURCE_DESCRIPTION_ID = RESOURCE_ID.resolve("fcr:metadata");
    private static final FedoraId RESOURCE_ACL_ID = RESOURCE_ID.resolve("fcr:acl");

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        final String txId = UUID.randomUUID().toString();
        tx = TestTransactionHelper.mockTransaction(txId, false);
        when(psManager.getSession(any(Transaction.class))).thenReturn(pSession);
        final DeleteResourceOperationFactoryImpl factoryImpl = new DeleteResourceOperationFactoryImpl();
        setField(service, "deleteResourceFactory", factoryImpl);
        setField(service, "containmentIndex", containmentIndex);
        setField(service, "eventAccumulator", eventAccumulator);
        setField(service, "referenceService", referenceService);
        setField(service, "membershipService", membershipService);
        setField(service, "searchIndex", searchIndex);
        when(container.getFedoraId()).thenReturn(RESOURCE_ID);

        when(pSession.getHeaders(RESOURCE_ID, null)).thenReturn(resourceHeaders);
        when(pSession.getHeaders(CHILD_RESOURCE_ID, null)).thenReturn(childHeaders);
        when(pSession.getHeaders(RESOURCE_DESCRIPTION_ID, null)).thenReturn(descHeaders);
        when(pSession.getHeaders(RESOURCE_ACL_ID, null)).thenReturn(aclHeaders);
    }

    @After
    public void cleanUp() {
        containmentIndex.reset();
    }

    @Test
    public void testContainerDelete() throws Exception {
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);

        service.perform(tx, container, USER);
        containmentIndex.commitTransaction(tx);
        verifyResourceOperation(RESOURCE_ID, operationCaptor, pSession);
    }

    @Test
    public void testRecursiveDelete() throws Exception {
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);
        when(childContainer.getFedoraId()).thenReturn(CHILD_RESOURCE_ID);
        when(childContainer.isAcl()).thenReturn(false);
        when(childContainer.getAcl()).thenReturn(null);

        when(resourceFactory.getResource(tx, CHILD_RESOURCE_ID)).thenReturn(childContainer);
        containmentIndex.addContainedBy(tx, container.getFedoraId(), childContainer.getFedoraId());

        assertEquals(1, containmentIndex.getContains(tx, RESOURCE_ID).count());
        service.perform(tx, container, USER);

        verify(pSession, times(2)).persist(operationCaptor.capture());
        final List<DeleteResourceOperation> operations = operationCaptor.getAllValues();
        assertEquals(2, operations.size());

        assertEquals(CHILD_RESOURCE_ID, operations.get(0).getResourceId());
        assertEquals(RESOURCE_ID, operations.get(1).getResourceId());

        assertEquals(0, containmentIndex.getContains(tx, RESOURCE_ID).count());

        verify(tx).lockResourceAndGhostNodes(RESOURCE_ID);
        verify(tx).lockResourceAndGhostNodes(CHILD_RESOURCE_ID);
    }

    private void verifyResourceOperation(final FedoraId fedoraID,
                                         final ArgumentCaptor<DeleteResourceOperation> captor,
                                         final PersistentStorageSession pSession) throws Exception {
        verify(pSession).persist(captor.capture());
        final DeleteResourceOperation containerOperation = captor.getValue();
        assertEquals(fedoraID, containerOperation.getResourceId());
    }

    @Test
    public void testAclDelete() throws Exception {
        when(acl.getFedoraId()).thenReturn(RESOURCE_ACL_ID);
        when(acl.isAcl()).thenReturn(true);
        service.perform(tx, acl, USER);
        verifyResourceOperation(RESOURCE_ACL_ID, operationCaptor, pSession);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testBinaryDescriptionDelete() throws Exception {
        when(binaryDesc.getFedoraId()).thenReturn(RESOURCE_DESCRIPTION_ID);
        service.perform(tx, binaryDesc, USER);
    }

    @Test
    public void testBinaryDeleteWithAcl() throws Exception {
        when(binary.getFedoraId()).thenReturn(RESOURCE_ID);
        when(binary.isAcl()).thenReturn(false);
        when(binary.getDescription()).thenReturn(binaryDesc);
        when(binaryDesc.getFedoraId()).thenReturn(RESOURCE_DESCRIPTION_ID);
        when(binary.getAcl()).thenReturn(acl);
        when(acl.getFedoraId()).thenReturn(RESOURCE_ACL_ID);

        service.perform(tx, binary, USER);

        verify(pSession, times(3)).persist(operationCaptor.capture());
        final List<DeleteResourceOperation> operations = operationCaptor.getAllValues();
        assertEquals(3, operations.size());

        assertEquals(RESOURCE_DESCRIPTION_ID, operations.get(0).getResourceId());
        assertEquals(RESOURCE_ACL_ID, operations.get(1).getResourceId());
        assertEquals(RESOURCE_ID, operations.get(2).getResourceId());

        verify(tx).lockResourceAndGhostNodes(RESOURCE_ID);
        verify(tx).lockResource(RESOURCE_DESCRIPTION_ID);
        verify(tx).lockResource(RESOURCE_ACL_ID);
    }

}
