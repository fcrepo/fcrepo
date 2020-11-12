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
import org.fcrepo.kernel.api.identifiers.FedoraId;
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

/**
 * @author whikloj
 */
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
        final FedoraId parentId = FedoraId.create(UUID.randomUUID().toString());
        final String txId = UUID.randomUUID().toString();
        when(transaction.getId()).thenReturn(txId);
        when(parentResource.getFedoraId()).thenReturn(parentId);
        setField(containmentTriplesService, "containmentIndex", containmentIndex);
    }

    @After
    public void cleanUp() {
        containmentIndex.reset();
    }

    @Test
    public void testNoContains() {
        assertEquals(0, containmentTriplesService.get(transaction, parentResource).count());
    }

    @Test
    public void testOneChild() {
        final FedoraId child = FedoraId.create(UUID.randomUUID().toString());
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getFedoraId(), child);
        assertEquals(1, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(child.getFullId()));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testTwoChildren() {
        final FedoraId child1 = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId child2 = FedoraId.create(UUID.randomUUID().toString());
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getFedoraId(), child1);
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getFedoraId(), child2);
        assertEquals(2, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(child1.getFullId()));
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(child2.getFullId()));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testTenChildren() {
        final Model expectedModel = ModelFactory.createDefaultModel();
        for (var foo = 0; foo < 10; foo += 1) {
            final FedoraId child = FedoraId.create(UUID.randomUUID().toString());
            containmentIndex.addContainedBy(transaction.getId(), parentResource.getFedoraId(), child);
            expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                    createResource(child.getFullId()));
        }
        assertEquals(10, containmentTriplesService.get(transaction, parentResource).count());
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testAddAndRemove() {
        final FedoraId child = FedoraId.create(UUID.randomUUID().toString());
        final String otherTransaction = UUID.randomUUID().toString();
        when(transaction2.getId()).thenReturn(otherTransaction);
        containmentIndex.addContainedBy(transaction.getId(), parentResource.getFedoraId(), child);
        assertEquals(1, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(child.getFullId()));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
        // Commit and ensure we can see the child.
        containmentIndex.commitTransaction(transaction.getId());
        final Model received2 = containmentTriplesService.get(null, parentResource).collect(toModel());
        matchModels(expectedModel, received2);
        // Now remove the child in a transaction, but verify we can still see it outside the transaction.
        containmentIndex.removeResource(otherTransaction, child);
        final Model received3 = containmentTriplesService.get(null, parentResource).collect(toModel());
        matchModels(expectedModel, received3);
        // Now commit the transaction and ensure it disappears.
        containmentIndex.commitTransaction(transaction2.getId());
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
