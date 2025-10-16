/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl;

import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author peichman
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration("/containmentIndexTest.xml")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class ContainmentIndexImplTest {

    @Mock
    private FedoraResource parent1;

    @Mock
    private FedoraResource child1;

    @Mock
    private FedoraResource parent2;

    @Mock
    private FedoraResource child2;

    private Transaction transaction1;

    private Transaction transaction2;

    private Transaction shortLivedTx;

    @Inject
    private ContainmentIndexImpl containmentIndex;

    private final Map<String, FedoraResource> id_to_resource = new HashMap<>();
    private final Map<String, Transaction> id_to_transaction = new HashMap<>();

    @BeforeEach
    @FlywayTest
    public void setUp() {
        id_to_resource.put("parent1", parent1);
        id_to_resource.put("parent2", parent2);
        id_to_resource.put("child1", child1);
        id_to_resource.put("child2", child2);

        transaction1 = TestTransactionHelper.mockTransaction("transaction1", false);
        transaction2 = TestTransactionHelper.mockTransaction("transaction2", false);
        shortLivedTx = TestTransactionHelper.mockTransaction("shortLived", true);

        id_to_transaction.put("transaction1", transaction1);
        id_to_transaction.put("transaction2", transaction2);
    }

    /**
     * Utility method to make it easier to stub the getFedoraId() method and avoid MockitoHints.
     * @param id The resource|transaction ID/name
     */
    private void stubObject(final String id) {
        // Use unique ids for resources and transactions.
        final String uuid = UUID.randomUUID().toString();
        if (id_to_resource.containsKey(id)) {
            final FedoraId fID = FedoraId.create(uuid);
            when(id_to_resource.get(id).getFedoraId()).thenReturn(fID);
        } else if (id_to_transaction.containsKey(id)) {
            when(id_to_transaction.get(id).getId()).thenReturn(uuid);
        }
    }

    @Test
    public void testAddChildInTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        // outside of the transaction, the containment shouldn't show up
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
    }

    @Test
    public void testAddRemoveChildInSameTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
    }

    @Test
    public void testAddRemoveChildInTwoTransactions() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
    }

    @Test
    public void testAddRemovePurgeChildInTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
        containmentIndex.purgeResource(transaction1, child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
    }

    @Test
    public void testAddRemovePurgeChildThreeTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
        containmentIndex.purgeResource(transaction1, child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1, parent1.getFedoraId()).count());
    }

    @Test
    public void testRollbackTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent1.getFedoraId()).findFirst().get());
        assertNull(containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
        containmentIndex.rollbackTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertNull(containmentIndex.getContainedBy(transaction1, child1.getFedoraId()));
    }

    @Test
    public void testCommitTransaction() {
        stubObject("parent1");
        stubObject("child2");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child2.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1, child2.getFedoraId()));
        assertNull(containmentIndex.getContainedBy(shortLivedTx, child2.getFedoraId()));
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1, child2.getFedoraId()));
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child2.getFedoraId()));
    }

    @Test
    public void testSwapContained() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).findFirst().get());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        // Still the same outside
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).findFirst().get());
        // Still the same in a different transaction
        assertEquals(1, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction2, parent1.getFedoraId()).findFirst().get());
        // Inside it has changed
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent1.getFedoraId()).findFirst().get());
        containmentIndex.commitTransaction(transaction1);
        // After commit() it is the same outside transactions.
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).findFirst().get());
        // And now the same in a different transaction
        assertEquals(1, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction2, parent1.getFedoraId()).findFirst().get());
    }

    @Test
    public void testOnlyCommitOne() throws Exception {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(2, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // Delete one object in separate transactions.
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.removeContainedBy(transaction2, parent1.getFedoraId(), child2.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1);
        // Wait a second because containment end time is second accuracy.
        TimeUnit.SECONDS.sleep(1);
        // Now only one record was removed
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        // Except in the second transaction as it should now have 0
        assertEquals(0, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction2);
        // Wait a second because containment end time is second accuracy.
        TimeUnit.SECONDS.sleep(1);
        // Now all are gone
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
    }

    @Test
    public void testTwoTransactionDeleteSameChild() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(2, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // Delete one object in separate transactions.
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.removeContainedBy(transaction2, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1);
        // Now only one record was removed
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction2);
        // No change as the first transaction already committed.
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1.getFedoraId()).count());
    }

    @Test
    public void testContainedByTwoSameTransactionException() {
        stubObject("parent1");
        stubObject("parent2");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent2.getFedoraId()).count());
        // Add it to the first parent.
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent2.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent2.getFedoraId()).count());
        // When you add it to the second parent, it is altered and the first relationship is overwritten.
        containmentIndex.addContainedBy(transaction1, parent2.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent2.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent2.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1);
        // This should be rolled back so the additions should still be in the transaction operation table.
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent2.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent2.getFedoraId()).count());
    }

    @Test
    public void testExistsOutsideTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("transaction2");
        assertFalse(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
        containmentIndex.addContainedBy(transaction2, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2);
        assertTrue(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
    }

    @Test
    public void testExistsInsideTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("transaction2");
        assertFalse(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2, child1.getFedoraId(), false));
        // Only visible in the transaction.
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertFalse(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2, child1.getFedoraId(), false));
        // Rollback transaction.
        containmentIndex.rollbackTransaction(transaction1);
        assertFalse(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2, child1.getFedoraId(), false));
        // Add again in transaction.
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertFalse(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2, child1.getFedoraId(), false));
        // Commit and visible everywhere.
        containmentIndex.commitTransaction(transaction1);
        assertTrue(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction2, child1.getFedoraId(), false));
    }

    @Test
    public void testRemoveResource() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        containmentIndex.removeResource(transaction1, child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // Parent can still be found from the child
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
    }

    @Test
    public void testRemoveNotFromTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("parent2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertNull(containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction2, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2);
        containmentIndex.addContainedBy(transaction1, parent2.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertEquals(1, containmentIndex.getContains(transaction1, parent2.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent2.getFedoraId()).findFirst().get());
        containmentIndex.removeResource(transaction2, child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // Parent can still be found from the child
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertEquals(1, containmentIndex.getContains(transaction1, parent2.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1, parent2.getFedoraId()).findFirst().get());
    }

    @Test
    public void testCommitRemoveFromTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("transaction2");
        containmentIndex.addContainedBy(transaction2, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        containmentIndex.removeResource(transaction1, child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // Parent can still be found from the child.
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
    }

    /**
     * Ensure match the id without a trailing slash.
     */
    @Test
    public void testResourceExistsFedoraIDNoTrailingSlash() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        final FedoraId fedoraID = FedoraId.create(child1.getFedoraId().getFullId());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertTrue(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(shortLivedTx, fedoraID, false));
    }

    /**
     * Ensure match the id with a trailing slash.
     */
    @Test
    public void testResourceExistsFedoraIDTrailingSlash() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        final FedoraId fedoraID = FedoraId.create(child1.getFedoraId().getFullId() + "/");
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(shortLivedTx, child1.getFedoraId()));
        assertTrue(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(shortLivedTx, fedoraID, false));
    }

    @Test
    public void clearIndexWhenReset() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");

        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());

        assertTrue(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));

        containmentIndex.reset();

        assertFalse(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));
    }

    @Test
    public void clearAllTransactions() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("parent2");
        stubObject("child2");
        stubObject("transaction2");

        // Create two hierarchies, one in a committed transaction and the other in an uncommitted one
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction2, parent2.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1);

        assertTrue(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1, child1.getFedoraId(), false));

        assertFalse(containmentIndex.resourceExists(shortLivedTx, child2.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction2, child2.getFedoraId(), false));

        containmentIndex.clearAllTransactions();

        // Committed containment should persist, but uncommitted should not
        assertTrue(containmentIndex.resourceExists(shortLivedTx, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(shortLivedTx, child2.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2, child2.getFedoraId(), false));
    }

    @Test
    public void testHasResourcesStartingFailure() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        // Nothing exists.
        assertFalse(containmentIndex.hasResourcesStartingWith(shortLivedTx, parent1.getFedoraId()));
        // Add the single resource.
        containmentIndex.addContainedBy(transaction1, FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        // Still no similar paths.
        assertFalse(containmentIndex.hasResourcesStartingWith(shortLivedTx, parent1.getFedoraId()));
        // Add a contained resource that does NOT share the URI path.
        assertFalse(child1.getFedoraId().getFullId().startsWith(parent1.getFedoraId().getFullId()));
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        // Still no similar paths.
        assertFalse(containmentIndex.hasResourcesStartingWith(shortLivedTx, parent1.getFedoraId()));
    }

    @Test
    public void testHasResourcesStartingFailureUnderscore() {
        stubObject("parent1");
        final var subSubPathId = parent1.getFedoraId().resolve("abc/123");
        final var subPathIdWithUnderscore = parent1.getFedoraId().resolve("a_c");
        stubObject("transaction1");
        // Add a resource.
        containmentIndex.addContainedBy(transaction1, FedoraId.getRepositoryRootId(), subSubPathId);
        // That resource's ID does not start with the id containing underscores we are checking
        // as underscores are not treated as wildcards
        assertFalse(subSubPathId.getFullId().startsWith(subPathIdWithUnderscore.getFullId()));
        assertFalse(containmentIndex.hasResourcesStartingWith(transaction1, subPathIdWithUnderscore));
    }

    @Test
    public void testHasResourcesStartingFailurePercent() {
        stubObject("parent1");
        final var subSubPathId = parent1.getFedoraId().resolve("abbc/123");
        final var subPathIdWithPercent = parent1.getFedoraId().resolve("a%c");
        stubObject("transaction1");
        // Add a resource.
        containmentIndex.addContainedBy(transaction1, FedoraId.getRepositoryRootId(), subSubPathId);
        // That resource's ID does not start with the id containing percent signs we are checking
        // as percent signs are not treated as wildcards
        assertFalse(subSubPathId.getFullId().startsWith(subPathIdWithPercent.getFullId()));
        assertFalse(containmentIndex.hasResourcesStartingWith(transaction1, subPathIdWithPercent));
    }

    @Test
    public void testHasResourcesStartingSuccess() {
        stubObject("parent1");
        final var subPathId = parent1.getFedoraId().resolve("a/layer/down");
        stubObject("transaction1");
        // Add a resource.
        containmentIndex.addContainedBy(transaction1, FedoraId.getRepositoryRootId(), subPathId);
        // That resource's ID starts with the ID we are checking.
        assertTrue(subPathId.getFullId().startsWith(parent1.getFedoraId().getFullId()));
        assertTrue(containmentIndex.hasResourcesStartingWith(transaction1, parent1.getFedoraId()));
    }

    @Test
    public void testDeletedResourceExists() {
        stubObject("parent1");
        stubObject("transaction1");
        containmentIndex.addContainedBy(transaction1, FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertTrue(containmentIndex.resourceExists(shortLivedTx, parent1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(shortLivedTx, parent1.getFedoraId(), true));
        containmentIndex.removeContainedBy(transaction1, FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertFalse(containmentIndex.resourceExists(shortLivedTx, parent1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(shortLivedTx, parent1.getFedoraId(), true));
        containmentIndex.purgeResource(transaction1, parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        assertFalse(containmentIndex.resourceExists(shortLivedTx, parent1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(shortLivedTx, parent1.getFedoraId(), true));
    }

    @Test
    public void testMementosContainment() throws Exception {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");

        // Parent contains child1 and child2
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1);
        TimeUnit.SECONDS.sleep(1);
        assertEquals(2, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // get the current instant and make a FedoraId for a memento at this instant.
        final var bothTime = Instant.now();
        final var mementoId = parent1.getFedoraId().resolve("fcr:versions/" +
                bothTime.atZone(UTC).format(MEMENTO_LABEL_FORMATTER));
        // Wait.
        TimeUnit.SECONDS.sleep(2);
        // Delete child1
        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(2, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1);
        TimeUnit.SECONDS.sleep(1);
        // Child1 is gone in the current view.
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        // Child1 remains in the memento view.
        assertEquals(2, containmentIndex.getContains(shortLivedTx, mementoId).count());
        // purging child 1
        containmentIndex.purgeResource(transaction1, child1.getFedoraId());
        // stays the same as we haven't committed yet.
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(2, containmentIndex.getContains(shortLivedTx, mementoId).count());
        containmentIndex.commitTransaction(transaction1);
        // Now the memento loses track of child1.
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(shortLivedTx, mementoId).count());
    }

    @Test
    public void testChecksum() throws Exception {
        stubObject("parent1");
        stubObject("transaction1");
        // Need to add a containment record for the parent to hold the updated value.
        containmentIndex.addContainedBy(transaction1, FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        final var empty = containmentIndex.containmentLastUpdated(shortLivedTx, parent1.getFedoraId());
        assertNull(empty);
        // Wait a half second as the ETag is based on the highest value of any child's start_time or end_time.
        TimeUnit.MILLISECONDS.sleep(500);
        final var firstBorn = parent1.getFedoraId().resolve("child1");
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), firstBorn);
        assertEquals(1, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        final var first = containmentIndex.containmentLastUpdated(transaction1, parent1.getFedoraId());
        assertNotNull(first);
        assertNotEquals(empty, first);

        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(first, containmentIndex.containmentLastUpdated(shortLivedTx, parent1.getFedoraId()));

        // Wait half a second, all these children will share a start_time.
        TimeUnit.SECONDS.sleep(1);
        for (var i = 0; i < 30; i += 1) {
            final var kidId = parent1.getFedoraId().resolve("child-" + i);
            containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), kidId);
        }
        assertEquals(31, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        final var allTime = containmentIndex.containmentLastUpdated(transaction1, parent1.getFedoraId());
        assertNotEquals(empty, allTime);
        assertNotEquals(first, allTime);

        containmentIndex.rollbackTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        assertEquals(first, containmentIndex.containmentLastUpdated(shortLivedTx, parent1.getFedoraId()));

        TimeUnit.SECONDS.sleep(1);

        containmentIndex.removeContainedBy(transaction1, parent1.getFedoraId(), firstBorn);
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertNotEquals(first, containmentIndex.containmentLastUpdated(transaction1, parent1.getFedoraId()));
        assertNotEquals(allTime, containmentIndex.containmentLastUpdated(transaction1, parent1.getFedoraId()));

        containmentIndex.commitTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        final var last = containmentIndex.containmentLastUpdated(shortLivedTx, parent1.getFedoraId());
        assertNotNull(last);
        assertNotEquals(first, last);
        assertNotEquals(allTime, last);
    }

    @Test
    public void testLargeContainment() {
        stubObject("transaction1");
        stubObject("parent1");
        containmentIndex.setContainsLimit(5);
        final List<String> expectedChildren = new ArrayList<>(10);
        for (var i = 0; i < 10; i += 1) {
            final FedoraId childId = parent1.getFedoraId().resolve("child_" + i);
            expectedChildren.add(childId.getFullId());
            containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), childId);
        }
        assertEquals(10, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1);
        final var foundChildren = containmentIndex.getContains(shortLivedTx, parent1.getFedoraId())
                .collect(toList());
        assertEquals(10, foundChildren.size());
        assertEquals(expectedChildren, foundChildren);
    }

    @Test
    public void testAddAclInTransaction() {
        stubObject("parent1");
        final FedoraId aclId = FedoraId.create("parent1/fcr:acl");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1, parent1.getFedoraId(), aclId);
        assertEquals(0, containmentIndex.getContains(transaction1, parent1.getFedoraId()).count());
        // outside of the transaction, it still shouldn't show up
        assertEquals(0, containmentIndex.getContains(shortLivedTx, parent1.getFedoraId()).count());
    }

    @Test
    public void testGetContainedByBinaryDescription() {
        stubObject("parent1");
        stubObject("child1");
        final var binaryId1 = child1.getFedoraId();
        final var descriptionId1 = child1.getFedoraId().asDescription();
        when(child1.getFedoraId()).thenReturn(descriptionId1);

        containmentIndex.addContainedBy(shortLivedTx, parent1.getFedoraId(), binaryId1);
        assertNull(containmentIndex.getContainedBy(shortLivedTx, descriptionId1));
    }
}


