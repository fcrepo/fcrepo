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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @BeforeEach
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

    @Test
    public void testPurgeSubPathDoesNotExist() throws Exception {
        final var resourceId = FedoraId.create("info:fedora/an-ocfl-object/some-subpath");
        when(mapping.getOcflObjectId()).thenReturn("some-ocfl-id");
        when(mapping.getRootObjectIdentifier()).thenReturn(FedoraId.create("info:fedora/an-ocfl-object"));
        when(operation.getResourceId()).thenReturn(resourceId);
        when(index.getMapping(eq(transaction), any())).thenReturn(mapping);
        doThrow(NotFoundException.class)
            .when(session).deleteResource(resourceId.getResourceId());
        assertThrows(PersistentStorageException.class, () -> persister.persist(psSession, operation));
    }

}
