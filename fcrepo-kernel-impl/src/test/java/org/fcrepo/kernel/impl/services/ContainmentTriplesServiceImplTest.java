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
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.impl.services.functions.FedoraIdUtils.ensurePrefix;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.inject.Inject;

import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ContainmentTriplesServiceImplTest {

    @Mock
    private FedoraResource parentResource;

    @Mock
    private Transaction transaction;

    @Mock
    private Transaction transaction2;

    @Inject
    private ContainmentIndex containmentIndex;

    @InjectMocks
    private ContainmentTriplesServiceImpl containmentTriplesService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final String parentId = ensurePrefix(UUID.randomUUID().toString());
        final String txId = UUID.randomUUID().toString();
        when(transaction.getId()).thenReturn(txId);
        when(parentResource.getId()).thenReturn(parentId);
        setField(containmentTriplesService, "containmentIndex", containmentIndex);
    }

    @After
    public void cleanUp() {
        containmentIndex.getContains(transaction, parentResource).forEach(c ->
                containmentIndex.removeContainedBy(transaction.getId(), parentResource.getId(), c));
    }

    @Test
    public void testNoContains() {
        assertEquals(0, containmentTriplesService.get(transaction, parentResource).count());
    }

    @Test
    public void testOneChild() {
        final String child = ensurePrefix(UUID.randomUUID().toString());
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getId(), child);
        assertEquals(1, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getId()), CONTAINS, createResource(child));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testTwoChildren() {
        final String child1 = ensurePrefix(UUID.randomUUID().toString());
        final String child2 = ensurePrefix(UUID.randomUUID().toString());
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getId(), child1);
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getId(), child2);
        assertEquals(2, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getId()), CONTAINS, createResource(child1));
        expectedModel.add(createResource(parentResource.getId()), CONTAINS, createResource(child2));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testTenChildren() {
        final Model expectedModel = ModelFactory.createDefaultModel();
        for (var foo = 0; foo < 10; foo += 1) {
            final String child = ensurePrefix(UUID.randomUUID().toString());
            containmentIndex.addContainedBy(transaction.getId(), parentResource.getId(), child);
            expectedModel.add(createResource(parentResource.getId()), CONTAINS, createResource(child));
        }
        assertEquals(10, containmentTriplesService.get(transaction, parentResource).count());
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testAddAndRemove() {
        final String child = ensurePrefix(UUID.randomUUID().toString());
        final String otherTransaction = UUID.randomUUID().toString();
        when(transaction2.getId()).thenReturn(otherTransaction);
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getId(), child);
        assertEquals(1, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getId()), CONTAINS, createResource(child));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
        // Commit and ensure we can see the child.
        containmentIndex.commitTransaction(transaction);
        final Model received2 = containmentTriplesService.get(null, parentResource).collect(toModel());
        matchModels(expectedModel, received2);
        // Now remove the child in a transaction, but verify we can still see it outside the transaction.
        containmentIndex.removeResource(otherTransaction, child);
        final Model received3 = containmentTriplesService.get(null, parentResource).collect(toModel());
        matchModels(expectedModel, received3);
        // Now commit the transaction and ensure it disappears.
        containmentIndex.commitTransaction(transaction2);
        assertEquals(0, containmentTriplesService.get(null, parentResource).count());
    }

    /**
     * Ensure the test model contains all the expected statements from the expected model.
     * @param expected The expected model.
     * @param test The model to be tested.
     */
    private void matchModels(final Model expected, final Model test) {
        for (final StmtIterator it = expected.listStatements(); it.hasNext(); ) {
            final Statement t = it.next();
            assertTrue(test.contains(t));
        }
    }
}
