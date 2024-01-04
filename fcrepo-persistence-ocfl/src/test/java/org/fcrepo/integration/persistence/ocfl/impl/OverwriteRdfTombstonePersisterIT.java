/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author mikejritter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/fcrepo-config.xml")
public class OverwriteRdfTombstonePersisterIT {

    @Autowired
    private OcflPersistentSessionManager sessionManager;

    @Autowired
    private RdfSourceOperationFactory rdfSourceOpFactory;

    @Autowired
    private DeleteResourceOperationFactory deleteResourceOpFactory;

    @Autowired
    private TransactionManager txManager;

    private FedoraId rescId;
    private Transaction tx;


    @Before
    public void setup() {
        rescId = makeRescId();
        tx = mock(Transaction.class);
        when(tx.getId()).thenReturn(UUID.randomUUID().toString());
        when(tx.isShortLived()).thenReturn(true);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return null;
        }).when(tx).doInTx(any(Runnable.class));
        when(txManager.create()).thenReturn(tx);
    }

    @Test
    public void overwriteArchivalGroupTombstone() {
        final var readSession = sessionManager.getReadOnlySession();

        final var tx1 = txManager.create();
        final var session1 = startWriteSession(tx1);
        final var createOp = rdfSourceOpFactory
            .createBuilder(tx1, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .archivalGroup(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();
        session1.persist(createOp);
        session1.prepare();
        session1.commit();

        // Check the persisted AG details
        final var agHeaders = readSession.getHeaders(rescId, null);
        assertEquals(rescId, agHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), agHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), agHeaders.getInteractionModel());

        final var tx2 = txManager.create();
        final var session2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory.deleteBuilder(tx2, rescId).build();
        session2.persist(deleteOp);
        session2.prepare();
        session2.commit();

        final var deletedHeaders = readSession.getHeaders(rescId, null);
        assertTrue(deletedHeaders.isDeleted());

        final var tx3 = txManager.create();
        final var session3 = startWriteSession(tx3);
        final var overwriteOp = rdfSourceOpFactory
            .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .archivalGroup(true)
            .isOverwrite(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();

        session3.persist(overwriteOp);
        session3.prepare();
        session3.commit();

        // Check the overwritten AG details
        final var overwrittenHeaders = readSession.getHeaders(rescId, null);
        assertFalse(overwrittenHeaders.isDeleted());
        assertEquals(rescId, overwrittenHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), overwrittenHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), overwrittenHeaders.getInteractionModel());
    }

    @Test
    public void overwriteAtomicTombstone() {
        final var readSession = sessionManager.getReadOnlySession();

        final var tx1 = txManager.create();
        final var session1 = startWriteSession(tx1);
        final var createOp = rdfSourceOpFactory
            .createBuilder(tx1, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .parentId(FedoraId.getRepositoryRootId())
            .build();
        session1.persist(createOp);
        session1.prepare();
        session1.commit();

        // Check the persisted resource details
        final var agHeaders = readSession.getHeaders(rescId, null);
        assertEquals(rescId, agHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), agHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), agHeaders.getInteractionModel());

        final var tx2 = txManager.create();
        final var session2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory.deleteBuilder(tx2, rescId).build();
        session2.persist(deleteOp);
        session2.prepare();
        session2.commit();

        final var deletedHeaders = readSession.getHeaders(rescId, null);
        assertTrue(deletedHeaders.isDeleted());

        final var tx3 = txManager.create();
        final var session3 = startWriteSession(tx3);
        final var overwriteOp = rdfSourceOpFactory
            .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .isOverwrite(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();

        session3.persist(overwriteOp);
        session3.prepare();
        session3.commit();

        // Check the overwritten resource details
        final var overwrittenHeaders = readSession.getHeaders(rescId, null);
        assertFalse(overwrittenHeaders.isDeleted());
        assertEquals(rescId, overwrittenHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), overwrittenHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), overwrittenHeaders.getInteractionModel());
    }

    @Test
    public void overwriteResourceTombstoneInAG() {
        final var readSession = sessionManager.getReadOnlySession();

        final var tx1 = txManager.create();
        final var session1 = startWriteSession(tx1);
        final var createOp = rdfSourceOpFactory
            .createBuilder(tx1, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .archivalGroup(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();
        session1.persist(createOp);

        final var containerId = rescId.resolve(UUID.randomUUID().toString());
        final var createOp2 = rdfSourceOpFactory
            .createBuilder(tx1, containerId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .parentId(rescId)
            .build();
        session1.persist(createOp2);
        session1.prepare();
        session1.commit();

        // Check the persisted AG + container details
        final var agHeaders = readSession.getHeaders(rescId, null);
        assertEquals(rescId, agHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), agHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), agHeaders.getInteractionModel());
        final var containerHeaders = readSession.getHeaders(containerId, null);
        assertEquals(containerId, containerHeaders.getId());
        assertEquals(rescId, containerHeaders.getArchivalGroupId());

        final var tx2 = txManager.create();
        final var session2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory.deleteBuilder(tx2, containerId).build();
        session2.persist(deleteOp);
        session2.prepare();
        session2.commit();

        final var deletedHeaders = readSession.getHeaders(containerId, null);
        assertTrue(deletedHeaders.isDeleted());

        final var tx3 = txManager.create();
        final var session3 = startWriteSession(tx3);
        final var overwriteOp = rdfSourceOpFactory
            .createBuilder(tx, containerId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .isOverwrite(true)
            .parentId(rescId)
            .build();

        session3.persist(overwriteOp);
        session3.prepare();
        session3.commit();

        // Check the overwritten resource details
        final var overwrittenHeaders = readSession.getHeaders(containerId, null);
        assertFalse(overwrittenHeaders.isDeleted());
        assertEquals(containerId, overwrittenHeaders.getId());
        assertEquals(rescId, overwrittenHeaders.getArchivalGroupId());
        assertEquals(rescId, overwrittenHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), overwrittenHeaders.getInteractionModel());
    }

    @Test(expected = PersistentItemConflictException.class)
    public void changeArchivalGroupTombstoneInteractionModel() {
        final var readSession = sessionManager.getReadOnlySession();

        final var tx1 = txManager.create();
        final var session1 = startWriteSession(tx1);
        final var createOp = rdfSourceOpFactory
            .createBuilder(tx1, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .archivalGroup(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();
        session1.persist(createOp);
        session1.prepare();
        session1.commit();

        // Check the persisted AG details
        final var agHeaders = readSession.getHeaders(rescId, null);
        assertEquals(rescId, agHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), agHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), agHeaders.getInteractionModel());

        final var tx2 = txManager.create();
        final var session2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory.deleteBuilder(tx2, rescId).build();
        session2.persist(deleteOp);
        session2.prepare();
        session2.commit();

        final var deletedHeaders = readSession.getHeaders(rescId, null);
        assertTrue(deletedHeaders.isDeleted());

        final var tx3 = txManager.create();
        final var session3 = startWriteSession(tx3);
        final var overwriteOp = rdfSourceOpFactory
            .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .archivalGroup(false)
            .isOverwrite(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();

        session3.persist(overwriteOp);
        session3.prepare();
        session3.commit();
    }

    @Test(expected = PersistentItemConflictException.class)
    public void changeAtomicTombstoneInteractionModel() {
        final var readSession = sessionManager.getReadOnlySession();

        final var tx1 = txManager.create();
        final var session1 = startWriteSession(tx1);
        final var createOp = rdfSourceOpFactory
            .createBuilder(tx1, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .parentId(FedoraId.getRepositoryRootId())
            .build();
        session1.persist(createOp);
        session1.prepare();
        session1.commit();

        // Check the persisted AG details
        final var agHeaders = readSession.getHeaders(rescId, null);
        assertEquals(rescId, agHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), agHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), agHeaders.getInteractionModel());

        final var tx2 = txManager.create();
        final var session2 = startWriteSession(tx2);
        final var deleteOp = deleteResourceOpFactory.deleteBuilder(tx2, rescId).build();
        session2.persist(deleteOp);
        session2.prepare();
        session2.commit();

        final var deletedHeaders = readSession.getHeaders(rescId, null);
        assertTrue(deletedHeaders.isDeleted());

        final var tx3 = txManager.create();
        final var session3 = startWriteSession(tx3);
        final var overwriteOp = rdfSourceOpFactory
            .createBuilder(tx, rescId, BASIC_CONTAINER.getURI(), ServerManagedPropsMode.RELAXED)
            .archivalGroup(true)
            .isOverwrite(true)
            .parentId(FedoraId.getRepositoryRootId())
            .build();

        session3.persist(overwriteOp);
        session3.prepare();
        session3.commit();
    }

    private PersistentStorageSession startWriteSession(final Transaction tx) {
        when(tx.getId()).thenReturn(UUID.randomUUID().toString());
        return sessionManager.getSession(tx);
    }

    private FedoraId makeRescId(final String... parentIds) {
        String parents = "";
        if (parentIds != null && parentIds.length > 0) {
            parents = Arrays.stream(parentIds).map(p -> p.replace("info:fedora/", ""))
                            .collect(Collectors.joining("/", "", "/"));
        }
        return FedoraId.create("info:fedora/" + parents + UUID.randomUUID());
    }
}
