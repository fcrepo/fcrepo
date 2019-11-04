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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;


import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperation;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperationBuilder;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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


    @InjectMocks
    private DeleteResourceServiceImpl service;

    private final String id = "test-resource";
    private final String txId = "tx-1234";

    @Test
    public void testContainerDelete() throws Exception {
        when(tx.getId()).thenReturn(txId);
        when(container.getId()).thenReturn(id);
        when(psManager.getSession(anyString())).thenReturn(pSession);
        when(containmentIndex.getContainedBy(eq(tx), eq(container))).thenReturn(Stream.<String>builder().build());
        when(container.isAcl()).thenReturn(false);
        when(container.getAcl()).thenReturn(null);
        when(factory.deleteBuilder(eq(id))).thenReturn(builder);
        when(builder.build()).thenReturn(operation);
        service.perform(tx, container);
        verify(pSession).persist(eq(operation));
        verify(containmentIndex).getContainedBy(eq(tx), eq(container));
    }


}
