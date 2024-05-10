/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentSessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.ocfl.api.MutableOcflRepository;
import io.ocfl.api.model.ObjectVersionId;

/**
 * Test delete resource persister for stamping versions of deleted resources in manually versioned repository.
 * @author whikloj
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/manual-versioning-config.xml")
public class DeleteResourcePersisterIT {

    @Autowired
    private OcflPersistentSessionManager sessionManager;

    @Autowired
    private RdfSourceOperationFactory rdfSourceOpFactory;

    @Autowired
    private DeleteResourceOperationFactory deleteResourceOpFactory;

    @Autowired
    private MutableOcflRepository ocflRepository;

    @Autowired
    private TransactionManager txManager;

    private Transaction transaction;

    private FedoraId rescId;

    @Before
    public void setup() {
        rescId = FedoraId.create(UUID.randomUUID().toString());
        transaction = mock(Transaction.class);
        when(transaction.getId()).thenReturn(UUID.randomUUID().toString());
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return null;
        }).when(transaction).doInTx(any(Runnable.class));
        when(txManager.create()).thenReturn(transaction);
    }

    @Test
    public void testDeleteAgResource() {

        final var tx = txManager.create();
        final PersistentStorageSession storageSession1 = startWriteSession(tx);
        // Create an AG resource.
        final var agOp = rdfSourceOpFactory
                .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.STRICT)
                .archivalGroup(true)
                .parentId(FedoraId.getRepositoryRootId())
                .build();
        storageSession1.persist(agOp);
        storageSession1.prepare();
        storageSession1.commit();

        // Assert it exists
        assertTrue(ocflRepository.containsObject(rescId.getResourceId()));
        // Assert it has a mutable head.
        assertTrue(ocflRepository.hasStagedChanges(rescId.getResourceId()));

        // In a new session delete it
        final var tx2 = txManager.create();
        final PersistentStorageSession storageSession2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory
                .deleteBuilder(tx2, rescId)
                .build();
        storageSession2.persist(deleteOp);
        storageSession2.prepare();
        storageSession2.commit();

        // Assert it still exists.
        assertTrue(ocflRepository.containsObject(rescId.getResourceId()));
        // Assert it is committed.
        assertFalse(ocflRepository.hasStagedChanges(rescId.getResourceId()));
    }

    @Test
    public void testDeleteResourceInAg() {
        final String childResourceId = UUID.randomUUID().toString();
        final FedoraId childId = rescId.resolve(childResourceId);
        final Transaction tx = txManager.create();
        final PersistentStorageSession storageSession1 = startWriteSession(tx);
        // Create an AG resource.
        final var agOp = rdfSourceOpFactory
                .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.STRICT)
                .archivalGroup(true)
                .parentId(FedoraId.getRepositoryRootId())
                .build();
        storageSession1.persist(agOp);
        storageSession1.prepare();
        storageSession1.commit();

        // Assert it exists
        assertTrue(ocflRepository.containsObject(rescId.getResourceId()));
        // Assert it has a mutable head.
        assertTrue(ocflRepository.hasStagedChanges(rescId.getResourceId()));

        final Transaction tx2 = txManager.create();
        final PersistentStorageSession storageSession2 = startWriteSession(tx2);

        // Create a resource in the AG
        final var agChild = rdfSourceOpFactory
                .createBuilder(tx2, childId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.STRICT)
                .parentId(rescId)
                .build();
        storageSession2.persist(agChild);
        storageSession2.prepare();
        storageSession2.commit();

        // Assert the child exists
        final var child = ocflRepository.getObject(ObjectVersionId.head(rescId.getResourceId()));
        assertTrue(child.containsFile(childResourceId + "/fcr-container.nt"));
        // Assert the resource still has a mutable head.
        assertTrue(ocflRepository.hasStagedChanges(rescId.getResourceId()));

        // In a new session delete the child resource.
        final Transaction tx3 = txManager.create();
        final PersistentStorageSession storageSession3 = startWriteSession(tx3);
        final var deleteOp = deleteResourceOpFactory
                .deleteBuilder(tx3, childId)
                .build();
        storageSession3.persist(deleteOp);
        storageSession3.prepare();
        storageSession3.commit();

        // Assert the AG resource still exists.
        assertTrue(ocflRepository.containsObject(rescId.getResourceId()));
        // Assert the child file is now gone.
        final var child2 = ocflRepository.getObject(ObjectVersionId.head(rescId.getResourceId()));
        assertFalse(child2.containsFile(childResourceId + "/fcr-container.nt"));
        // Assert the AG resource still has a mutable head.
        assertTrue(ocflRepository.hasStagedChanges(rescId.getResourceId()));
    }

    @Test
    public void testDeleteAtomicResource() {
        final Transaction tx = txManager.create();
        final PersistentStorageSession storageSession1 = startWriteSession(tx);
        // Create an atomic resource.
        final var op = rdfSourceOpFactory
                .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
                .parentId(FedoraId.getRepositoryRootId())
                .build();
        storageSession1.persist(op);
        storageSession1.prepare();
        storageSession1.commit();

        // Assert it exists.
        assertTrue(ocflRepository.containsObject(rescId.getResourceId()));
        // Assert it has a mutable head.
        assertTrue(ocflRepository.hasStagedChanges(rescId.getResourceId()));

        // In a new session delete it
        final Transaction tx2 = txManager.create();
        final PersistentStorageSession storageSession2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory
                .deleteBuilder(tx2, rescId)
                .build();
        storageSession2.persist(deleteOp);
        storageSession2.prepare();
        storageSession2.commit();

        // Assert it still exist.
        assertTrue(ocflRepository.containsObject(rescId.getResourceId()));
        // Assert it is committed.
        assertFalse(ocflRepository.hasStagedChanges(rescId.getResourceId()));
    }

    private PersistentStorageSession startWriteSession(final Transaction transaction) {
        sessionManager.removeSession(transaction.getId());
        return sessionManager.getSession(transaction);
    }
}
