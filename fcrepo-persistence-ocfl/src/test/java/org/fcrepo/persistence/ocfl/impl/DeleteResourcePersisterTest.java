/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.newResourceHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchCreationHeaders;
import static org.fcrepo.persistence.common.ResourceHeaderUtils.touchModificationHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

/**
 * Delete Persister tests.
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @BeforeEach
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

    @Test
    public void testDeleteSubPathDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(parentId);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getTransaction()).thenReturn(transaction);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        when(session.readHeaders(resourceId.getResourceId())).thenThrow(NotFoundException.class);
        assertThrows(PersistentStorageException.class, () -> persister.persist(psSession, operation));
    }

    @Test
    public void testDeleteFullObjectDoesNotExist() throws Exception {
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getTransaction()).thenReturn(transaction);
        when(index.getMapping(eq(transaction), any()))
                .thenThrow(new FedoraOcflMappingNotFoundException("error"));

        assertThrows(PersistentStorageException.class, () -> persister.persist(psSession, operation));
    }

}
