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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.NotFoundException;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Delete Persister tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DeleteResourcePersisterTest {

    @Mock
    private FedoraOcflMapping mapping;

    @Mock
    private OcflObjectSession session;

    @Mock
    private ResourceOperation operation;

    @Mock
    private FedoraToOcflObjectIndex index;

    @Mock
    private OcflPersistentStorageSession psSession;

    @Captor
    private ArgumentCaptor<ResourceHeaders> headersCaptor;

    private DeleteResourcePersister persister;

    private static final String SESSION_ID = "SOME-SESSION-ID";

    private FedoraId resourceId = FedoraId.create("info:fedora/an-ocfl-object/some-subpath");
    private FedoraId parentId = FedoraId.create("info:fedora/an-ocfl-object");

    @Before
    public void setup() throws Exception {
        operation = mock(ResourceOperation.class);
        persister = new DeleteResourcePersister(this.index);
        when(psSession.getId()).thenReturn(SESSION_ID);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
    }

    @Test
    public void testDeleteSubPathBinary() throws Exception {
        final var resourceId = FedoraId.create();

        final var headers = newResourceHeaders(
                parentId,
                resourceId,
                NON_RDF_SOURCE.toString());
        touchCreationHeaders(headers, null);
        touchModificationHeaders(headers, null);

        when(session.readHeaders(resourceId.getResourceId()))
                .thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(parentId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        when(session.containsResource(resourceId.getResourceId())).thenReturn(true);

        persister.persist(psSession, operation);

        verify(session).deleteContentFile(headersCaptor.capture());

        final var deleteHeaders = headersCaptor.getValue();
        assertEquals(resourceId.toString(), deleteHeaders.getId());
        assertTrue(deleteHeaders.isDeleted());
    }

    @Test
    public void deleteWhenEntireResourceRemoved() throws Exception {
        final var resourceId = FedoraId.create();

        final var headers = newResourceHeaders(
                parentId,
                resourceId,
                NON_RDF_SOURCE.toString());
        touchCreationHeaders(headers, null);
        touchModificationHeaders(headers, null);

        when(session.readHeaders(resourceId.getResourceId()))
                .thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(parentId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        when(session.containsResource(resourceId.getResourceId())).thenReturn(false);

        persister.persist(psSession, operation);

        verify(session).deleteContentFile(headersCaptor.capture());

        final var deleteHeaders = headersCaptor.getValue();
        assertEquals(resourceId.toString(), deleteHeaders.getId());
        assertTrue(deleteHeaders.isDeleted());

        verify(index).removeMapping(SESSION_ID, resourceId);
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteSubPathDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(parentId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(SESSION_ID), any())).thenReturn(mapping);
        when(session.readHeaders(resourceId.getResourceId())).thenThrow(NotFoundException.class);
        persister.persist(psSession, operation);
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteFullObjectDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(index.getMapping(eq(SESSION_ID), any())).thenThrow(new FedoraOcflMappingNotFoundException("error"));

        persister.persist(psSession, operation);
    }

}
