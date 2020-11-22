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
package org.fcrepo.kernel.impl;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author peichman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ContainmentIndexImplTest {

    @Mock
    private FedoraResource parent1;

    @Mock
    private FedoraResource child1;

    @Mock
    private FedoraResource parent2;

    @Mock
    private FedoraResource child2;

    @Mock
    private Transaction transaction1;

    @Mock
    private Transaction transaction2;

    @Inject
    private ContainmentIndexImpl containmentIndex;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private final Map<String, FedoraResource> id_to_resource = new HashMap<>();
    private final Map<String, Transaction> id_to_transaction = new HashMap<>();

    @Before
    public void setUp() {
        id_to_resource.put("parent1", parent1);
        id_to_resource.put("parent2", parent2);
        id_to_resource.put("child1", child1);
        id_to_resource.put("child2", child2);
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

    @After
    public void cleanUp() {
        containmentIndex.reset();
    }

    @Test
    public void testAddChildInTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        // outside of the transaction, the containment shouldn't show up
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
    }

    @Test
    public void testAddRemoveChildInSameTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
    }

    @Test
    public void testAddRemoveChildInTwoTransactions() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
    }

    @Test
    public void testAddRemovePurgeChildInTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.purgeResource(transaction1.getId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
    }

    @Test
    public void testAddRemovePurgeChildThreeTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.purgeResource(transaction1.getId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContainsDeleted(transaction1.getId(), parent1.getFedoraId()).count());
    }

    @Test
    public void testRollbackTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).findFirst().get());
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
        containmentIndex.rollbackTransaction(transaction1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getFedoraId()));
    }

    @Test
    public void testCommitTransaction() {
        stubObject("parent1");
        stubObject("child2");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child2.getFedoraId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1.getId(), child2.getFedoraId()));
        assertNull(containmentIndex.getContainedBy(null, child2.getFedoraId()));
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(null, parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(transaction1.getId(), child2.getFedoraId()));
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).findFirst().get());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child2.getFedoraId()));
    }

    @Test
    public void testSwapContained() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(null, parent1.getFedoraId()).findFirst().get());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        // Still the same outside
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(null, parent1.getFedoraId()).findFirst().get());
        // Still the same in a different transaction
        assertEquals(1, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).findFirst().get());
        // Inside it has changed
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).findFirst().get());
        containmentIndex.commitTransaction(transaction1.getId());
        // After commit() it is the same outside transactions.
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(null, parent1.getFedoraId()).findFirst().get());
        // And now the same in a different transaction
        assertEquals(1, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
        assertEquals(child2.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).findFirst().get());
    }

    @Test
    public void testOnlyCommitOne() throws Exception {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(2, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        // Delete one object in separate transactions.
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.removeContainedBy(transaction2.getId(), parent1.getFedoraId(), child2.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1.getId());
        // Wait a second because containment end time is second accuracy.
        TimeUnit.SECONDS.sleep(1);
        // Now only one record was removed
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        // Except in the second transaction as it should now have 0
        assertEquals(0, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction2.getId());
        // Wait a second because containment end time is second accuracy.
        TimeUnit.SECONDS.sleep(1);
        // Now all are gone
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
    }

    @Test
    public void testTwoTransactionDeleteSameChild() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(2, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        // Delete one object in separate transactions.
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.removeContainedBy(transaction2.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1.getId());
        // Now only one record was removed
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction2.getId());
        // No change as the first transaction already committed.
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction2.getId(), parent1.getFedoraId()).count());
    }

    @Test
    public void testContainedByTwoSameTransactionException() {
        stubObject("parent1");
        stubObject("parent2");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(null, parent2.getFedoraId()).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1.getId(), parent2.getFedoraId(), child1.getFedoraId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(null, parent2.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent2.getFedoraId()).count());
        try {
            containmentIndex.commitTransaction(transaction1.getId());
            // We should get an exception.
            fail();
        } catch (final RepositoryRuntimeException e) {
            // This was an expected exception. Now continue the test.
        }
        // This should be rolled back so the additions should still be in the transaction operation table.
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(null, parent2.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent2.getFedoraId()).count());
    }

    @Test
    public void testExistsOutsideTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("transaction2");
        assertFalse(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
        containmentIndex.addContainedBy(transaction2.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2.getId());
        assertTrue(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
    }

    @Test
    public void testExistsInsideTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("transaction2");
        assertFalse(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getFedoraId(), false));
        // Only visible in the transaction.
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertFalse(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getFedoraId(), false));
        // Rollback transaction.
        containmentIndex.rollbackTransaction(transaction1.getId());
        assertFalse(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getFedoraId(), false));
        // Add again in transaction.
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertFalse(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getFedoraId(), false));
        // Commit and visible everywhere.
        containmentIndex.commitTransaction(transaction1.getId());
        assertTrue(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(transaction2.getId(), child1.getFedoraId(), false));
    }

    @Test
    public void testRemoveResource() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child1.getFedoraId()));
        containmentIndex.removeResource(transaction1.getId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
    }

    @Test
    public void testRemoveNotFromTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("parent2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
        containmentIndex.addContainedBy(transaction2.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2.getId());
        containmentIndex.addContainedBy(transaction1.getId(), parent2.getFedoraId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent2.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent2.getFedoraId()).findFirst().get());
        containmentIndex.removeResource(transaction2.getId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent2.getFedoraId()).count());
        assertEquals(child1.getFedoraId().getFullId(),
                containmentIndex.getContains(transaction1.getId(), parent2.getFedoraId()).findFirst().get());
    }

    @Test
    public void testCommitRemoveFromTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("parent2");
        stubObject("transaction1");
        stubObject("transaction2");
        containmentIndex.addContainedBy(transaction2.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction2.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child1.getFedoraId()));
        containmentIndex.removeResource(transaction1.getId(), child1.getFedoraId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
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
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertTrue(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(null, fedoraID, false));
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
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(parent1.getFedoraId().getFullId(),
                containmentIndex.getContainedBy(null, child1.getFedoraId()));
        assertTrue(containmentIndex.resourceExists(null, child1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(null, fedoraID, false));
    }

    @Test
    public void clearIndexWhenReset() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");

        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());

        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));

        containmentIndex.reset();

        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getFedoraId(), false));
    }

    @Test
    public void testHasResourcesStartingFailure() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        // Nothing exists.
        assertFalse(containmentIndex.hasResourcesStartingWith(null, parent1.getFedoraId()));
        // Add the single resource.
        containmentIndex.addContainedBy(transaction1.getId(), FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        // Still no similar paths.
        assertFalse(containmentIndex.hasResourcesStartingWith(null, parent1.getFedoraId()));
        // Add a contained resource that does NOT share the URI path.
        assertFalse(child1.getFedoraId().getFullId().startsWith(parent1.getFedoraId().getFullId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        // Still no similar paths.
        assertFalse(containmentIndex.hasResourcesStartingWith(null, parent1.getFedoraId()));
    }

    @Test
    public void testHasResourcesStartingSuccess() {
        stubObject("parent1");
        final var subPathId = parent1.getFedoraId().resolve("a/layer/down");
        stubObject("transaction1");
        // Add a resource.
        containmentIndex.addContainedBy(transaction1.getId(), FedoraId.getRepositoryRootId(), subPathId);
        // That resource's ID starts with the ID we are checking.
        assertTrue(subPathId.getFullId().startsWith(parent1.getFedoraId().getFullId()));
        assertTrue(containmentIndex.hasResourcesStartingWith(transaction1.getId(), parent1.getFedoraId()));
    }

    @Test
    public void testDeletedResourceExists() {
        stubObject("parent1");
        stubObject("transaction1");
        containmentIndex.addContainedBy(transaction1.getId(), FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertTrue(containmentIndex.resourceExists(null, parent1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(null, parent1.getFedoraId(), true));
        containmentIndex.removeContainedBy(transaction1.getId(), FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertFalse(containmentIndex.resourceExists(null, parent1.getFedoraId(), false));
        assertTrue(containmentIndex.resourceExists(null, parent1.getFedoraId(), true));
        containmentIndex.purgeResource(transaction1.getId(), parent1.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        assertFalse(containmentIndex.resourceExists(null, parent1.getFedoraId(), false));
        assertFalse(containmentIndex.resourceExists(null, parent1.getFedoraId(), true));
    }

    @Test
    public void testMementosContainment() throws Exception {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");

        // Parent contains child1 and child2
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), child2.getFedoraId());
        containmentIndex.commitTransaction(transaction1.getId());
        TimeUnit.SECONDS.sleep(1);
        assertEquals(2, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        // get the current instant and make a FedoraId for a memento at this instant.
        final var bothTime = Instant.now();
        final var mementoId = parent1.getFedoraId().resolve("fcr:versions/" +
                bothTime.atZone(UTC).format(MEMENTO_LABEL_FORMATTER));
        // Wait.
        TimeUnit.SECONDS.sleep(2);
        // Delete child1
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), child1.getFedoraId());
        assertEquals(2, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1.getId());
        TimeUnit.SECONDS.sleep(1);
        // Child1 is gone in the current view.
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        // Child1 remains in the memento view.
        assertEquals(2, containmentIndex.getContains(null, mementoId).count());
        // purging child 1
        containmentIndex.purgeResource(transaction1.getId(), child1.getFedoraId());
        // stays the same as we haven't committed yet.
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(2, containmentIndex.getContains(null, mementoId).count());
        containmentIndex.commitTransaction(transaction1.getId());
        // Now the memento loses track of child1.
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(1, containmentIndex.getContains(null, mementoId).count());
    }

    @Test
    public void testChecksum() throws Exception {
        stubObject("parent1");
        stubObject("transaction1");
        // Need to add a containment record for the parent to hold the updated value.
        containmentIndex.addContainedBy(transaction1.getId(), FedoraId.getRepositoryRootId(), parent1.getFedoraId());
        final var empty = containmentIndex.containmentLastUpdated(null, parent1.getFedoraId());
        assertNull(empty);
        // Wait a half second as the ETag is based on the highest value of any child's start_time or end_time.
        TimeUnit.MILLISECONDS.sleep(500);
        final var firstBorn = parent1.getFedoraId().resolve("child1");
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), firstBorn);
        assertEquals(1, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        final var first = containmentIndex.containmentLastUpdated(transaction1.getId(), parent1.getFedoraId());
        assertNotNull(first);
        assertNotEquals(empty, first);

        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(first, containmentIndex.containmentLastUpdated(null, parent1.getFedoraId()));

        // Wait half a second, all these children will share a start_time.
        TimeUnit.SECONDS.sleep(1);
        for (var i = 0; i < 30; i += 1) {
            final var kidId = parent1.getFedoraId().resolve("child-" + i);
            containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), kidId);
        }
        assertEquals(31, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        final var allTime = containmentIndex.containmentLastUpdated(transaction1.getId(), parent1.getFedoraId());
        assertNotEquals(empty, allTime);
        assertNotEquals(first, allTime);

        containmentIndex.rollbackTransaction(transaction1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        assertEquals(first, containmentIndex.containmentLastUpdated(null, parent1.getFedoraId()));

        TimeUnit.SECONDS.sleep(1);

        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getFedoraId(), firstBorn);
        assertEquals(0, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertNotEquals(first, containmentIndex.containmentLastUpdated(transaction1.getId(), parent1.getFedoraId()));
        assertNotEquals(allTime, containmentIndex.containmentLastUpdated(transaction1.getId(), parent1.getFedoraId()));

        containmentIndex.commitTransaction(transaction1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        final var last = containmentIndex.containmentLastUpdated(null, parent1.getFedoraId());
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
            containmentIndex.addContainedBy(transaction1.getId(), parent1.getFedoraId(), childId);
        }
        assertEquals(10, containmentIndex.getContains(transaction1.getId(), parent1.getFedoraId()).count());
        assertEquals(0, containmentIndex.getContains(null, parent1.getFedoraId()).count());
        containmentIndex.commitTransaction(transaction1.getId());
        final var foundChildren = containmentIndex.getContains(null, parent1.getFedoraId())
                .collect(toList());
        assertEquals(10, foundChildren.size());
        assertEquals(expectedChildren, foundChildren);
    }
}


