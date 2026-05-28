/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ReindexResourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationBuilder;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test for ReindexServiceImpl
 *
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class ReindexServiceImplTest {

    @Mock
    private TransactionManager transactionManager;

    @Mock
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Mock
    private ReindexResourceOperationFactory resourceOperationFactory;

    @Mock
    private Transaction transaction;

    @Mock
    private PersistentStorageSession persistentStorageSession;

    @Mock
    private ResourceOperationBuilder operationBuilder;

    @Mock
    private ResourceOperation operation;

    @InjectMocks
    private ReindexServiceImpl reindexService;

    @Test
    public void testReindexByFedoraId() {
        final var fedoraId = FedoraId.create("info:fedora/test-resource");
        final var principal = "testUser";
        final var txId = "tx-123";

        // Setup mocks
        when(transaction.getId()).thenReturn(txId);
        when(transactionManager.get(txId)).thenReturn(transaction);
        when(persistentStorageSessionManager.getSession(transaction)).thenReturn(persistentStorageSession);
        when(resourceOperationFactory.create(transaction, fedoraId)).thenReturn(operationBuilder);
        when(operationBuilder.userPrincipal(principal)).thenReturn(operationBuilder);
        when(operationBuilder.build()).thenReturn(operation);

        // Call the service
        reindexService.reindexByFedoraId(transaction, principal, fedoraId);

        // Verify interactions
        verify(transaction).lockResource(fedoraId);
        verify(persistentStorageSession).persist(operation);
    }
}