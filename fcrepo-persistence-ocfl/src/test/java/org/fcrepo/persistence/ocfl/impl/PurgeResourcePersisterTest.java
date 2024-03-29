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
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Purge Persister tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PurgeResourcePersisterTest {

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

    private PurgeResourcePersister persister;

    private static String SESSION_ID = "SOME-SESSION-ID";

    @Before
    public void setup() throws Exception {
        operation = mock(ResourceOperation.class);
        persister = new PurgeResourcePersister(this.index);
        when(psSession.getId()).thenReturn(SESSION_ID);
        when(psSession.findOrCreateSession(anyString())).thenReturn(session);
        when(transaction.getId()).thenReturn(SESSION_ID);
        when(operation.getTransaction()).thenReturn(transaction);
    }

    @Test
    public void testPurgeSubPathBinary() throws Exception {
        final var resourceId = FedoraId.create("info:fedora/an-ocfl-object/some-subpath");
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        persister.persist(psSession, operation);
        verify(session).deleteResource(resourceId.getResourceId());
    }

    @Test(expected = PersistentStorageException.class)
    public void testPurgeSubPathDoesNotExist() throws Exception {
        final var resourceId = FedoraId.create("info:fedora/an-ocfl-object/some-subpath");
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        doThrow(NotFoundException.class)
            .when(session).deleteResource(resourceId.getResourceId());
        persister.persist(psSession, operation);
    }

}
