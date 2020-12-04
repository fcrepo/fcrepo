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

import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
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

/**
 * @author mdurbin
 */
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

    @Mock
    private ResourceHeaders headers;

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

        when(session.getHeaders(fedoraId, null)).thenReturn(headers);
        when(headers.getInteractionModel()).thenReturn(RdfLexicon.RDF_SOURCE.toString());

        service.createVersion(transaction, fedoraId, user);

        final var captor = ArgumentCaptor.forClass(ResourceOperation.class);
        verify(transaction).lockResource(fedoraId);
        verify(session).persist(captor.capture());
        final var captured = captor.getValue();

        assertEquals(fedoraId, captured.getResourceId());
        assertEquals(user, captured.getUserPrincipal());
    }

    @Test
    public void createPersistOperationBinaryDesc() throws PersistentStorageException {
        final var fedoraId = FedoraId.create("info:fedora/test").asDescription();
        final var user = "me";

        when(session.getHeaders(fedoraId, null)).thenReturn(headers);
        when(headers.getInteractionModel()).thenReturn(RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI);

        service.createVersion(transaction, fedoraId, user);

        final var captor = ArgumentCaptor.forClass(ResourceOperation.class);
        verify(transaction).lockResource(fedoraId);
        verify(transaction).lockResource(fedoraId.asBaseId());
        verify(session).persist(captor.capture());
        final var captured = captor.getValue();

        assertEquals(fedoraId, captured.getResourceId());
        assertEquals(user, captured.getUserPrincipal());
    }

    @Test
    public void createPersistOperationAgPart() throws PersistentStorageException {
        final var agId = FedoraId.create("ag");
        final var fedoraId = agId.resolve("test");
        final var user = "me";

        when(session.getHeaders(fedoraId, null)).thenReturn(headers);
        when(headers.getArchivalGroupId()).thenReturn(agId);
        when(headers.getInteractionModel()).thenReturn(RdfLexicon.NON_RDF_SOURCE.toString());

        service.createVersion(transaction, fedoraId, user);

        final var captor = ArgumentCaptor.forClass(ResourceOperation.class);
        verify(transaction).lockResource(agId);
        verify(transaction).lockResource(fedoraId.asDescription());
        verify(transaction).lockResource(fedoraId);
        verify(session).persist(captor.capture());
        final var captured = captor.getValue();

        assertEquals(fedoraId, captured.getResourceId());
        assertEquals(user, captured.getUserPrincipal());
    }

}
