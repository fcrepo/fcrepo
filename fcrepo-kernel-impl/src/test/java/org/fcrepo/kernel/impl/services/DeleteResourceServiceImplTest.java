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

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperation;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
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

import java.util.List;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.inject.Inject;

/**
 * DeleteResourceServiceTest
 *
 * @author dbernstein
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class DeleteResourceServiceImplTest {

    @Mock
    private Transaction tx;

    @Mock
    private PersistentStorageSession pSession;

    @Inject
    private ContainmentIndex containmentIndex;

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

    @Captor
    private ArgumentCaptor<DeleteResourceOperation> operationCaptor;

    @InjectMocks
    private DeleteResourceServiceImpl service;

    private static final String RESOURCE_ID =  FEDORA_ID_PREFIX + "test-resource";
    private static final FedoraId RESOURCE_FEDORA_ID = FedoraId.create(RESOURCE_ID);
    private static final String CHILD_RESOURCE_ID = FEDORA_ID_PREFIX + "test-resource-child";
    private static final FedoraId CHILD_RESOURCE_FEDORA_ID = FedoraId.create(CHILD_RESOURCE_ID);
    private static final String RESOURCE_DESCRIPTION_ID = FEDORA_ID_PREFIX + "test-resource-description";
    private static final FedoraId RESOURCE_DESCRIPTION_FEDORA_ID = FedoraId.create(RESOURCE_DESCRIPTION_ID);
    private static final String RESOURCE_ACL_ID = FEDORA_ID_PREFIX + "test-resource-acl";
    private static final FedoraId RESOURCE_ACL_FEDORA_ID = FedoraId.create(RESOURCE_ACL_ID);
    private static final String TX_ID = "tx-1234";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(tx.getId()).thenReturn(TX_ID);
        when(psManager.getSession(anyString())).thenReturn(pSession);
        final DeleteResourceOperationFactoryImpl factoryImpl = new DeleteResourceOperationFactoryImpl();
        setField(service, "deleteResourceFactory", factoryImpl);
        setField(service, "containmentIndex", containmentIndex);
        when(container.getFedoraId()).thenReturn(RESOURCE_FEDORA_ID);
    }

    @After
    public void cleanUp() {
        containmentIndex.rollbackTransaction(tx);
        containmentIndex.getContains(tx, container).forEach(c ->
                containmentIndex.removeContainedBy(tx.getId(), container.getFedoraId(), FedoraId.create(c)));
    }

    @Test
    public void testContainerDelete() throws Exception {
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);

        service.perform(tx, container);
        verifyResourceOperation(RESOURCE_FEDORA_ID, operationCaptor, pSession);
    }

    @Test
    public void testRecursiveDelete() throws Exception {
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);
        when(childContainer.getFedoraId()).thenReturn(CHILD_RESOURCE_FEDORA_ID);
        when(childContainer.isAcl()).thenReturn(false);
        when(childContainer.getAcl()).thenReturn(null);

        when(resourceFactory.getResource(tx, CHILD_RESOURCE_FEDORA_ID)).thenReturn(childContainer);
        containmentIndex.addContainedBy(tx.getId(), container.getFedoraId(), childContainer.getFedoraId());

        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);

        assertEquals(1, containmentIndex.getContains(tx, container).count());
        service.perform(tx, container);

        verify(pSession, times(2)).persist(operationCaptor.capture());
        final List<DeleteResourceOperation> operations = operationCaptor.getAllValues();
        assertEquals(2, operations.size());

        assertEquals(CHILD_RESOURCE_ID, operations.get(0).getResourceId());
        assertEquals(RESOURCE_ID, operations.get(1).getResourceId());

        assertEquals(0, containmentIndex.getContains(tx, container).count());
    }

    private void verifyResourceOperation(final FedoraId fedoraID,
                                         final ArgumentCaptor<DeleteResourceOperation> captor,
                                         final PersistentStorageSession pSession) throws Exception {
        verify(pSession).persist(captor.capture());
        final DeleteResourceOperation containerOperation = captor.getValue();
        assertEquals(fedoraID.getFullId(), containerOperation.getResourceId());
    }

    @Test
    public void testAclDelete() throws Exception {
        when(acl.getFedoraId()).thenReturn(RESOURCE_ACL_FEDORA_ID);
        when(acl.isAcl()).thenReturn(true);
        service.perform(tx, acl);
        verifyResourceOperation(RESOURCE_ACL_FEDORA_ID, operationCaptor, pSession);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testBinaryDescriptionDelete() throws Exception {
        when(binaryDesc.getFedoraId()).thenReturn(RESOURCE_DESCRIPTION_FEDORA_ID);
        service.perform(tx, binaryDesc);
    }

    @Test
    public void testBinaryDeleteWithAcl() throws Exception {
        when(binary.getFedoraId()).thenReturn(RESOURCE_FEDORA_ID);
        when(binary.isAcl()).thenReturn(false);
        when(binary.getDescribedResource()).thenReturn(binaryDesc);
        when(binaryDesc.getFedoraId()).thenReturn(RESOURCE_DESCRIPTION_FEDORA_ID);
        when(binary.getAcl()).thenReturn(acl);
        when(acl.getFedoraId()).thenReturn(RESOURCE_ACL_FEDORA_ID);

        service.perform(tx, binary);

        verify(pSession, times(3)).persist(operationCaptor.capture());
        final List<DeleteResourceOperation> operations = operationCaptor.getAllValues();
        assertEquals(3, operations.size());

        assertEquals(RESOURCE_DESCRIPTION_ID, operations.get(0).getResourceId());
        assertEquals(RESOURCE_ACL_ID, operations.get(1).getResourceId());
        assertEquals(RESOURCE_ID, operations.get(2).getResourceId());
    }
}
