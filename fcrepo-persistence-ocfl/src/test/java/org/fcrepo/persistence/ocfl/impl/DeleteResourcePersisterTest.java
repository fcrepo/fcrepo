/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;

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
import static org.mockito.Mockito.never;
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

    @Mock
    private Transaction transaction;

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
        headers.setArchivalGroupId(parentId);
        touchCreationHeaders(headers, null);
        touchModificationHeaders(headers, null);

        when(session.readHeaders(resourceId.getResourceId()))
                .thenReturn(new ResourceHeadersAdapter(headers).asStorageHeaders());
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(parentId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getTransaction()).thenReturn(transaction);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(session.containsResource(resourceId.getResourceId())).thenReturn(true);

        persister.persist(psSession, operation);

        verify(session).deleteContentFile(headersCaptor.capture());
        verify(session, never()).commitType(CommitType.NEW_VERSION);

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
        when(operation.getTransaction()).thenReturn(transaction);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(session.containsResource(resourceId.getResourceId())).thenReturn(false);

        persister.persist(psSession, operation);

        verify(session).deleteContentFile(headersCaptor.capture());
        verify(session).commitType(CommitType.NEW_VERSION);

        final var deleteHeaders = headersCaptor.getValue();
        assertEquals(resourceId.toString(), deleteHeaders.getId());
        assertTrue(deleteHeaders.isDeleted());

        verify(index).removeMapping(transaction, resourceId);
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteSubPathDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(parentId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getTransaction()).thenReturn(transaction);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(session.readHeaders(resourceId.getResourceId())).thenThrow(NotFoundException.class);
        persister.persist(psSession, operation);
    }

    @Test(expected = PersistentStorageException.class)
    public void testDeleteFullObjectDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getTransaction()).thenReturn(transaction);
        when(index.getMapping(eq(transaction), any()))
                .thenThrow(new FedoraOcflMappingNotFoundException("error"));

        persister.persist(psSession, operation);
    }

}
