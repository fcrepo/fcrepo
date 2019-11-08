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
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperation;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * DeleteResourceServiceTest
 *
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.Strict.class)
public class DeleteResourceServiceImplTest {

    @Mock
    private Transaction tx;

    @Mock
    private PersistentStorageSession pSession;

    @Mock
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

    @InjectMocks
    private DeleteResourceServiceImpl service;

    private static final String RESOURCE_ID = "test-resource";
    private static final String CHILD_RESOURCE_ID = "test-resource-child";
    private static final String RESOURCE_DESCRIPTION_ID = "test-resource-description";
    private static final String RESOURCE_ACL_ID = "test-resource-acl";
    private static final String TX_ID = "tx-1234";

    @Before
    public void setup() {
        when(tx.getId()).thenReturn(TX_ID);
        when(psManager.getSession(anyString())).thenReturn(pSession);
        final DeleteResourceOperationFactoryImpl factoryImpl = new DeleteResourceOperationFactoryImpl();
        setField(service, "deleteResourceFactory", factoryImpl);
    }

    @Test
    public void testContainerDelete() throws Exception {
        when(container.getId()).thenReturn(RESOURCE_ID);
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);

        when(containmentIndex.getContainedBy(eq(tx), eq(container))).thenReturn(Stream.<String>builder().build());

        final ArgumentCaptor<DeleteResourceOperation> containerOperationCaptor = ArgumentCaptor.forClass(DeleteResourceOperation.class);
        service.perform(tx, container);
        verifyResourceOperation(RESOURCE_ID, containerOperationCaptor, pSession);
        verify(containmentIndex).getContainedBy(eq(tx), eq(container));
    }

    @Test
    public void testRecursiveDelete() throws Exception {
        when(container.getId()).thenReturn(RESOURCE_ID);
        when(container.getId()).thenReturn(RESOURCE_ID);
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);
        when(childContainer.getId()).thenReturn(CHILD_RESOURCE_ID);
        when(childContainer.isAcl()).thenReturn(false);
        when(childContainer.getAcl()).thenReturn(null);

        when(resourceFactory.getResource(tx, CHILD_RESOURCE_ID)).thenReturn(childContainer);
        when(containmentIndex.getContainedBy(eq(tx), eq(container))).thenReturn(Stream.<String>builder()
                                                                                      .add(CHILD_RESOURCE_ID).build());
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);
        final ArgumentCaptor<DeleteResourceOperation> captor = ArgumentCaptor.forClass(DeleteResourceOperation.class);
        service.perform(tx, container);

        verify(pSession, times(2)).persist(captor.capture());
        final List<DeleteResourceOperation> operations = captor.getAllValues();
        assertEquals(2, operations.size());

        assertEquals(CHILD_RESOURCE_ID, operations.get(0).getResourceId());
        assertEquals(RESOURCE_ID, operations.get(1).getResourceId());

        verify(containmentIndex).getContainedBy(eq(tx), eq(container));
    }

    private void verifyResourceOperation(final String resourceId,
                                         final ArgumentCaptor<DeleteResourceOperation> captor,
                                         final PersistentStorageSession pSession) throws Exception {
        verify(pSession).persist(captor.capture());
        final DeleteResourceOperation containerOperation = captor.getValue();
        assertEquals(resourceId, containerOperation.getResourceId());
    }

    @Test
    public void testAclDelete() throws Exception {
        when(acl.getId()).thenReturn(RESOURCE_ACL_ID);
        when(acl.isAcl()).thenReturn(true);
        final ArgumentCaptor<DeleteResourceOperation> aclCaptor = ArgumentCaptor.forClass(DeleteResourceOperation.class);
        service.perform(tx, acl);
        verifyResourceOperation(RESOURCE_ACL_ID, aclCaptor, pSession);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testBinaryDescriptionDelete() throws Exception {
        when(binaryDesc.getId()).thenReturn(RESOURCE_DESCRIPTION_ID);
        service.perform(tx, binaryDesc);
    }

    @Test
    public void testBinaryDeleteWithAcl() throws Exception {
        final ArgumentCaptor<DeleteResourceOperation> captor = ArgumentCaptor.forClass(DeleteResourceOperation.class);
        when(binary.getId()).thenReturn(RESOURCE_ID);
        when(binary.isAcl()).thenReturn(false);
        when(binary.getDescribedResource()).thenReturn(binaryDesc);
        when(binaryDesc.getId()).thenReturn(RESOURCE_DESCRIPTION_ID);
        when(binary.getAcl()).thenReturn(acl);
        when(acl.getId()).thenReturn(RESOURCE_ACL_ID);

        service.perform(tx, binary);

        verify(pSession, times(3)).persist(captor.capture());
        final List<DeleteResourceOperation> operations = captor.getAllValues();
        assertEquals(3, operations.size());

        assertEquals(RESOURCE_DESCRIPTION_ID, operations.get(0).getResourceId());
        assertEquals(RESOURCE_ACL_ID, operations.get(1).getResourceId());
        assertEquals(RESOURCE_ID, operations.get(2).getResourceId());

    }
}
