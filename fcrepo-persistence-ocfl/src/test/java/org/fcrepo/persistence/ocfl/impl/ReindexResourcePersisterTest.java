/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.REINDEX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ReindexResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for {@link ReindexResourcePersister}
 *
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReindexResourcePersisterTest {

    @Mock
    private ReindexService reindexService;

    @Mock
    private OcflPersistentStorageSession session;

    @Mock
    private TestReindexOperation reindexOp;

    @Mock
    private ResourceOperation nonReindexOp;

    @Mock
    private Transaction transaction;

    private ReindexResourcePersister persister;

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/test-resource");

    @BeforeEach
    public void setup() {
        persister = new ReindexResourcePersister(reindexService);

        // Set up the reindex operation
        when(reindexOp.getType()).thenReturn(REINDEX);
        when(reindexOp.getResourceId()).thenReturn(RESOURCE_ID);
        when(reindexOp.getTransaction()).thenReturn(transaction);

        when(nonReindexOp.getType()).thenReturn(CREATE);
        when(nonReindexOp.getResourceId()).thenReturn(RESOURCE_ID);
        when(nonReindexOp.getTransaction()).thenReturn(transaction);
    }

    @Test
    public void testHandle_ReindexOperation() {
        assertTrue(persister.handle(reindexOp));
    }

    @Test
    public void testHandle_NonReindexOperation() {
        assertFalse(persister.handle(nonReindexOp));
    }

    @Test
    public void testHandle_NullOperation() {
        assertFalse(persister.handle(null));
    }

    @Test
    public void testPersist() throws Exception {
        persister.persist(session, reindexOp);

        // Verify the reindex service was called with the correct parameters
        verify(reindexService).indexOcflObject(transaction, RESOURCE_ID.getBaseId());
    }

    @Test
    public void testPersist_WithException() throws Exception {
        // Make the reindex service throw an exception
        doThrow(new RuntimeException("Test exception"))
                .when(reindexService)
                .indexOcflObject(any(Transaction.class), any(String.class));

        // Verify the exception is wrapped in a PersistentStorageException
        assertThrows(PersistentStorageException.class, () -> {
            persister.persist(session, reindexOp);
        });
    }

    @Test
    public void testPersist_NonReindexOperation() throws Exception {
        // Try to persist a non-reindex operation (should never happen in practice)
        assertThrows(ClassCastException.class, () -> {
            persister.persist(session, nonReindexOp);
        });

        // Verify the reindex service was not called
        verifyNoInteractions(reindexService);
    }

    // The persist method declares that it takes a ResourceOperation, but it actually requires
    // a ReindexResourceOperation, which is not a type of ResourceOperation.
    // This interface is used to allow the test to compile.
    private interface TestReindexOperation extends ResourceOperation, ReindexResourceOperation {}
}
