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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.impl.operations.VersionResourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceImplTest {

    private VersionServiceImpl service;

    @Mock
    private EventAccumulator eventAccumulator;

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    private PersistentStorageSession session;

    @Mock
    private Transaction transaction;

    private final String TX_ID = "tx1";

    @Before
    public void setup() {
        service = new VersionServiceImpl();
        setField(service, "eventAccumulator", eventAccumulator);
        service.setPsManager(psManager);
        service.setVersionOperationFactory(new VersionResourceOperationFactoryImpl());

        when(transaction.getId()).thenReturn(TX_ID);
        when(psManager.getSession(TX_ID)).thenReturn(session);
    }

    @Test
    public void createPersistOperation() throws PersistentStorageException {
        final var fedoraId = FedoraId.create("info:fedora/test");
        final var user = "me";

        service.createVersion(transaction, fedoraId, user);

        final var captor = ArgumentCaptor.forClass(ResourceOperation.class);
        verify(session).persist(captor.capture());
        final var captured = captor.getValue();

        assertEquals(fedoraId.getResourceId(), captured.getResourceId());
        assertEquals(user, captured.getUserPrincipal());
    }

}
