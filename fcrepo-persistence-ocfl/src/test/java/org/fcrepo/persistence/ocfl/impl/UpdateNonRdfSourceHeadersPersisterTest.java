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
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE_HEADERS;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperation;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author mikejritter
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateNonRdfSourceHeadersPersisterTest {
    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/parent/child");
    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");
    private static final String USER_PRINCIPAL = "fedoraUser";

    @Mock
    private UpdateNonRdfSourceHeadersOperation operation;

    @Mock
    private OcflObjectSession session;

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession psSession;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    private UpdateNonRdfSourceHeadersPersister persister;

    @Mock
    private Transaction transaction;

    @Before
    public void setup() throws Exception {
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(operation.getType()).thenReturn(UPDATE_HEADERS);
        when(operation.getTransaction()).thenReturn(transaction);

        persister = new UpdateNonRdfSourceHeadersPersister(index);
    }

    @Test
    public void testHandle() {
        assertTrue(persister.handle(operation));
        final NonRdfSourceOperation badOperation = mock(NonRdfSourceOperation.class);
        assertFalse(persister.handle(badOperation));
    }

    @Test
    public void testPersistHeaders() {
        final var now = Instant.now();
        final var user = "some-user";
        final var objectId = "object-id";

        when(operation.getResourceId()).thenReturn(RESOURCE_ID);
        when(mapping.getOcflObjectId()).thenReturn(objectId);

        // setup headers
        final var headers = newResourceHeaders(ROOT_RESOURCE_ID, RESOURCE_ID, BASIC_CONTAINER.toString());
        touchCreationHeaders(headers, USER_PRINCIPAL);
        touchModificationHeaders(headers, USER_PRINCIPAL);
        when(session.readHeaders(anyString())).thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());

        when(operation.getCreatedDate()).thenReturn(now);
        when(operation.getLastModifiedDate()).thenReturn(now);
        when(operation.getCreatedBy()).thenReturn(user);
        when(operation.getLastModifiedBy()).thenReturn(user);

        persister.persist(psSession, operation);

        verify(session).writeHeaders(headersCaptor.capture());

        final var resultHeaders = headersCaptor.getValue();
        assertEquals(now, resultHeaders.getCreatedDate());
        assertEquals(now, resultHeaders.getLastModifiedDate());
        assertEquals(user, resultHeaders.getCreatedBy());
        assertEquals(user, resultHeaders.getLastModifiedBy());
    }

}