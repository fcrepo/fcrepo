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
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperation;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationBuilder;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.stream.Stream;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private DeleteResourceOperationFactory factory;

    @Mock
    private DeleteResourceOperationBuilder builder;

    @Mock
    private DeleteResourceOperation operation;

    @Mock
    private Container container;

    @Mock
    private Binary binary;

    @Mock
    private WebacAcl acl;

    @Mock
    private NonRdfSourceDescription binaryDesc;

    @InjectMocks
    private DeleteResourceServiceImpl service;

    private static final String RESOURCE_ID = "test-resource";
    private static final String RESOURCE_DESCRIPTION_ID = "test-resource-description";
    private static final String RESOURCE_ACL_ID = "test-resource-acl";
    private static final String TX_ID = "tx-1234";

    @Before
    public void setup() {
        when(tx.getId()).thenReturn(TX_ID);
        when(psManager.getSession(anyString())).thenReturn(pSession);
    }

    @Test
    public void testContainerDelete() throws Exception {
        when(container.getId()).thenReturn(RESOURCE_ID);
        when(containmentIndex.getContainedBy(eq(tx), eq(container))).thenReturn(Stream.<String>builder().build());
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);
        when(factory.deleteBuilder(eq(RESOURCE_ID))).thenReturn(builder);
        when(builder.build()).thenReturn(operation);
        service.perform(tx, container);
        verify(pSession).persist(eq(operation));
        verify(containmentIndex).getContainedBy(eq(tx), eq(container));
    }

    @Test
    public void testAclDelete() throws Exception {
        when(acl.getId()).thenReturn(RESOURCE_ACL_ID);
        when(acl.isAcl()).thenReturn(true);
        when(factory.deleteBuilder(eq(RESOURCE_ACL_ID))).thenReturn(builder);
        when(builder.build()).thenReturn(operation);
        service.perform(tx, acl);
        verify(pSession).persist(eq(operation));
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testBinaryDescriptionDelete() throws Exception {
        when(binaryDesc.getId()).thenReturn(RESOURCE_DESCRIPTION_ID);
        service.perform(tx, binaryDesc);
    }

    @Test
    public void testBinaryDeleteWithAcl() throws Exception {
        when(binary.getId()).thenReturn(RESOURCE_ID);
        when(binary.isAcl()).thenReturn(false);
        when(factory.deleteBuilder(eq(RESOURCE_ID))).thenReturn(builder);
        when(builder.build()).thenReturn(operation);

        when(binary.getDescribedResource()).thenReturn(binaryDesc);
        when(binaryDesc.getId()).thenReturn(RESOURCE_DESCRIPTION_ID);
        final DeleteResourceOperationBuilder binaryDescBuilder = mock(DeleteResourceOperationBuilder.class);
        final DeleteResourceOperation binaryDescDeleteOperation = mock(DeleteResourceOperation.class);
        when(factory.deleteBuilder(eq(RESOURCE_DESCRIPTION_ID))).thenReturn(binaryDescBuilder);
        when(binaryDescBuilder.build()).thenReturn(binaryDescDeleteOperation);

        when(binary.getAcl()).thenReturn(acl);
        when(acl.getId()).thenReturn(RESOURCE_ACL_ID);
        final DeleteResourceOperationBuilder aclBuilder = mock(DeleteResourceOperationBuilder.class);
        final DeleteResourceOperation aclOperation = mock(DeleteResourceOperation.class);
        when(factory.deleteBuilder(eq(RESOURCE_ACL_ID))).thenReturn(aclBuilder);
        when(aclBuilder.build()).thenReturn(aclOperation);

        service.perform(tx, binary);
        verify(pSession).persist(eq(operation));
        verify(pSession).persist(eq(binaryDescDeleteOperation));
        verify(pSession).persist(eq(aclOperation));
    }
}
