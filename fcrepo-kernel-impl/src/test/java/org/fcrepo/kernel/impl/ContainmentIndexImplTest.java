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
import org.fcrepo.kernel.api.identifiers.FedoraID;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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

    private Map<String, FedoraResource> id_to_resource = new HashMap<>();
    private Map<String, Transaction> id_to_transaction = new HashMap<>();

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
     * Utility method to make it easier to stub the getId() method and avoid MockitoHints.
     * @param id The resource|transaction ID/name
     */
    private void stubObject(final String id) {
        if (id_to_resource.containsKey(id)) {
            when(id_to_resource.get(id).getId()).thenReturn(FEDORA_ID_PREFIX + id);
        } else if (id_to_transaction.containsKey(id)) {
            when(id_to_transaction.get(id).getId()).thenReturn(id);
        }
    }

    @After
    public void cleanUp() {
        // Rollback any in-process transactions.
        containmentIndex.rollbackTransaction(transaction1);
        containmentIndex.rollbackTransaction(transaction2);
        // Remove all parent's children
        containmentIndex.getContains(null, parent1).forEach(t ->
                containmentIndex.removeContainedBy(null, parent1.getId(), t));
        // Remove all parent2's children
        containmentIndex.getContains(null, parent2).forEach(t ->
                containmentIndex.removeContainedBy(null, parent2.getId(), t));
    }

    @Test
    public void testAddChild() {
        stubObject("parent1");
        stubObject("child1");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(child1.getId(), containmentIndex.getContains(null, parent1).findFirst().get());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
    }

    @Test
    public void testRemoveChild() {
        stubObject("parent1");
        stubObject("child1");
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        containmentIndex.removeContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
    }

    @Test
    public void testAddChildInTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(child1.getId(), containmentIndex.getContains(transaction1, parent1).findFirst().get());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
        // outside of the transaction, the containment shouldn't show up
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
    }

    @Test
    public void testRemoveChildInTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
    }

    @Test
    public void testRollbackTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(child1.getId(), containmentIndex.getContains(transaction1, parent1).findFirst().get());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
        containmentIndex.rollbackTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
        assertNull(containmentIndex.getContainedBy(transaction1.getId(), child1.getId()));
    }

    @Test
    public void testCommitTransaction() {
        stubObject("parent1");
        stubObject("child2");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child2.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(child2.getId(), containmentIndex.getContains(transaction1, parent1).findFirst().get());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(transaction1.getId(), child2.getId()));
        assertNull(containmentIndex.getContainedBy(null, child2.getId()));
        containmentIndex.commitTransaction(transaction1);
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(child2.getId(), containmentIndex.getContains(null, parent1).findFirst().get());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(transaction1.getId(), child2.getId()));
        assertEquals(child2.getId(), containmentIndex.getContains(transaction1, parent1).findFirst().get());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child2.getId()));
    }

    @Test
    public void testSwapContained() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(child1.getId(), containmentIndex.getContains(null, parent1).findFirst().get());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child2.getId());
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        // Still the same outside
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(child1.getId(), containmentIndex.getContains(null, parent1).findFirst().get());
        // Still the same in a different transaction
        assertEquals(1, containmentIndex.getContains(transaction2, parent1).count());
        assertEquals(child1.getId(), containmentIndex.getContains(transaction2, parent1).findFirst().get());
        // Inside it has changed
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(child2.getId(), containmentIndex.getContains(transaction1, parent1).findFirst().get());
        containmentIndex.commitTransaction(transaction1);
        // After commit() it is the same outside transactions.
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(child2.getId(), containmentIndex.getContains(null, parent1).findFirst().get());
        // And now the same in a different transaction
        assertEquals(1, containmentIndex.getContains(transaction2, parent1).count());
        assertEquals(child2.getId(), containmentIndex.getContains(transaction2, parent1).findFirst().get());
    }

    @Test
    public void testOnlyCommitOne() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        containmentIndex.addContainedBy(null, parent1.getId(), child2.getId());
        assertEquals(2, containmentIndex.getContains(null, parent1).count());
        // Delete one object in separate transactions.
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        containmentIndex.removeContainedBy(transaction2.getId(), parent1.getId(), child2.getId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1).count());
        containmentIndex.commitTransaction(transaction1);
        // Now only one record was removed
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        // Except in the second transaction as it should now have 0
        assertEquals(0, containmentIndex.getContains(transaction2, parent1).count());
        containmentIndex.commitTransaction(transaction2);
        // Now all are gone
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(0, containmentIndex.getContains(transaction2, parent1).count());
    }

    @Test
    public void testTwoTransactionDeleteSameChild() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("child2");
        stubObject("transaction1");
        stubObject("transaction2");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        containmentIndex.addContainedBy(null, parent1.getId(), child2.getId());
        assertEquals(2, containmentIndex.getContains(null, parent1).count());
        // Delete one object in separate transactions.
        containmentIndex.removeContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        containmentIndex.removeContainedBy(transaction2.getId(), parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1).count());
        containmentIndex.commitTransaction(transaction1);
        // Now only one record was removed
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1).count());
        containmentIndex.commitTransaction(transaction2);
        // No change as the first transaction already committed.
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction2, parent1).count());
    }

    @Test(expected = DuplicateKeyException.class)
    public void testContainedByTwoParentsException() {
        stubObject("parent1");
        stubObject("parent2");
        stubObject("child1");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(null, parent2).count());
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        containmentIndex.addContainedBy(null, parent2.getId(), child1.getId());
    }

    @Test
    public void testContainedByTwoSameTransactionException() {
        stubObject("parent1");
        stubObject("parent2");
        stubObject("child1");
        stubObject("transaction1");
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(null, parent2).count());
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        containmentIndex.addContainedBy(transaction1.getId(), parent2.getId(), child1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(null, parent2).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent2).count());
        try {
            containmentIndex.commitTransaction(transaction1);
            // We should get an exception.
            fail();
        } catch (RepositoryRuntimeException e) {
            // This was an expected exception. Now continue the test.
        }
        // This should be rolled back so the additions should still be in the transaction operation table.
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertEquals(0, containmentIndex.getContains(null, parent2).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent1).count());
        assertEquals(1, containmentIndex.getContains(transaction1, parent2).count());
    }

    @Test
    public void testExistsOutsideTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        assertFalse(containmentIndex.resourceExists(null, child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertTrue(containmentIndex.resourceExists(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
    }

    @Test
    public void testExistsInsideTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("transaction2");
        assertFalse(containmentIndex.resourceExists(null, child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getId()));
        // Only visible in the transaction.
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertFalse(containmentIndex.resourceExists(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getId()));
        // Rollback transaction.
        containmentIndex.rollbackTransaction(transaction1);
        assertFalse(containmentIndex.resourceExists(null, child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getId()));
        // Add again in transaction.
        containmentIndex.addContainedBy(transaction1.getId(), parent1.getId(), child1.getId());
        assertFalse(containmentIndex.resourceExists(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
        assertFalse(containmentIndex.resourceExists(transaction2.getId(), child1.getId()));
        // Commit and visible everywhere.
        containmentIndex.commitTransaction(transaction1);
        assertTrue(containmentIndex.resourceExists(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(transaction1.getId(), child1.getId()));
        assertTrue(containmentIndex.resourceExists(transaction2.getId(), child1.getId()));
    }

    @Test
    public void testRemoveResource() {
        stubObject("parent1");
        stubObject("child1");
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        containmentIndex.removeResource(null, child1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
    }

    @Test
    public void testRemoveNotFromTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("parent2");
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        containmentIndex.addContainedBy(transaction1.getId(), parent2.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        assertEquals(1, containmentIndex.getContains(transaction1, parent2).count());
        assertEquals(child1.getId(), containmentIndex.getContains(transaction1, parent2).findFirst().get());
        containmentIndex.removeResource(null, child1.getId());
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
        assertEquals(1, containmentIndex.getContains(transaction1, parent2).count());
        assertEquals(child1.getId(), containmentIndex.getContains(transaction1, parent2).findFirst().get());
    }

    @Test
    public void testCommitRemoveFromTransaction() {
        stubObject("parent1");
        stubObject("child1");
        stubObject("transaction1");
        stubObject("parent2");
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        containmentIndex.removeResource(transaction1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
        containmentIndex.commitTransaction(transaction1);
        assertEquals(0, containmentIndex.getContains(null, parent1).count());
        assertNull(containmentIndex.getContainedBy(null, child1.getId()));
        assertEquals(0, containmentIndex.getContains(transaction1, parent1).count());
    }

    /**
     * Ensure match the id without a trailing slash.
     */
    @Test
    public void testResourceExistsFedoraIDNoTrailingSlash() {
        stubObject("parent1");
        stubObject("child1");
        final FedoraID fedoraID = FedoraID.create(child1.getId());
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(null, fedoraID));
    }

    /**
     * Ensure match the id with a trailing slash.
     */
    @Test
    public void testResourceExistsFedoraIDTrailingSlash() {
        stubObject("parent1");
        stubObject("child1");
        final FedoraID fedoraID = FedoraID.create(child1.getId() + "/");
        containmentIndex.addContainedBy(null, parent1.getId(), child1.getId());
        assertEquals(1, containmentIndex.getContains(null, parent1).count());
        assertEquals(parent1.getId(), containmentIndex.getContainedBy(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(null, child1.getId()));
        assertTrue(containmentIndex.resourceExists(null, fedoraID));
    }
}


