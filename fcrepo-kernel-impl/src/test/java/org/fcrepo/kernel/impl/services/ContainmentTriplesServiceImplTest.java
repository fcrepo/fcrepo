/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.time.Instant;
import java.util.UUID;

import jakarta.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.impl.TestTransactionHelper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author whikloj
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration("/containmentIndexTest.xml")
public class ContainmentTriplesServiceImplTest {

    @Mock
    private FedoraResource parentResource;

    private Transaction transaction;

    private Transaction transaction2;

    @Inject
    private ContainmentIndex containmentIndex;

    @InjectMocks
    private ContainmentTriplesServiceImpl containmentTriplesService;

    private Transaction readOnlyTx;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        final FedoraId parentId = FedoraId.create(UUID.randomUUID().toString());
        final String txId = UUID.randomUUID().toString();
        transaction = TestTransactionHelper.mockTransaction(txId, false);
        when(parentResource.getFedoraId()).thenReturn(parentId);
        setField(containmentTriplesService, "containmentIndex", containmentIndex);
        readOnlyTx = ReadOnlyTransaction.INSTANCE;
    }

    @AfterEach
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
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), child);
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
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), child1);
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), child2);
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
            containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), child);
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
        final String otherTransactionId = UUID.randomUUID().toString();

        transaction2 = TestTransactionHelper.mockTransaction(otherTransactionId, false);
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), child);
        assertEquals(1, containmentTriplesService.get(transaction, parentResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(child.getFullId()));
        final Model received = containmentTriplesService.get(transaction, parentResource).collect(toModel());
        matchModels(expectedModel, received);
        // Commit and ensure we can see the child.
        containmentIndex.commitTransaction(transaction);
        final Model received2 = containmentTriplesService.get(readOnlyTx, parentResource).collect(toModel());
        matchModels(expectedModel, received2);
        // Now remove the child in a transaction, but verify we can still see it outside the transaction.
        containmentIndex.removeResource(transaction2, child);
        final Model received3 = containmentTriplesService.get(readOnlyTx, parentResource).collect(toModel());
        matchModels(expectedModel, received3);
        // Now commit the transaction and ensure it disappears.
        containmentIndex.commitTransaction(transaction2);
        assertEquals(0, containmentTriplesService.get(readOnlyTx, parentResource).count());
    }

    @Test
    public void testGetContainedByBasicContainer() {
        final FedoraId childId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraResource childResource = mock(FedoraResource.class);
        when(childResource.getFedoraId()).thenReturn(childId);
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), childId);
        assertEquals(1, containmentTriplesService.getContainedBy(transaction, childResource).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(childId.getFullId()));
        final Model received = containmentTriplesService.getContainedBy(transaction, childResource).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testGetContainedByNoParent() {
        assertEquals(0, containmentTriplesService.getContainedBy(transaction, parentResource).count());
    }

    @Test
    public void testGetContainedByBasicContainerMemento() {
        final FedoraId childId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childMementoId = childId.asMemento(Instant.now());
        final FedoraResource mementoResc = mock(FedoraResource.class);
        when(mementoResc.getFedoraId()).thenReturn(childMementoId);
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), childId);
        assertEquals(1, containmentTriplesService.getContainedBy(transaction, mementoResc).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(childId.getFullId()));
        final Model received = containmentTriplesService.getContainedBy(transaction, mementoResc).collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testGetContainedByBinaryDescription() {
        final FedoraId childId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId descriptionId = childId.asDescription();
        final FedoraResource descriptionResc = mock(FedoraResource.class);
        when(descriptionResc.getFedoraId()).thenReturn(descriptionId);
        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), childId);
        assertEquals(1, containmentTriplesService.getContainedBy(transaction, descriptionResc).count());
        final Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(childId.getFullId()));
        final Model received = containmentTriplesService.getContainedBy(transaction, descriptionResc)
                .collect(toModel());
        matchModels(expectedModel, received);
    }

    @Test
    public void testGetContainedByBinaryDescriptionMemento() {
        final var now = Instant.now();
        final FedoraId childId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childMementoId = childId.asMemento(now);
        final FedoraResource mementoResc = mock(FedoraResource.class);
        when(mementoResc.getFedoraId()).thenReturn(childMementoId);

        final FedoraId descriptionId = childId.asDescription();
        final FedoraId descriptionMementoId = descriptionId.asMemento(Instant.now());
        final FedoraResource mementoDescResc = mock(FedoraResource.class);
        when(mementoDescResc.getFedoraId()).thenReturn(descriptionMementoId);

        containmentIndex.addContainedBy(transaction, parentResource.getFedoraId(), childId);

        assertEquals(1, containmentTriplesService.getContainedBy(transaction, mementoDescResc).count());
        final var expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(createResource(parentResource.getFedoraId().getFullId()), CONTAINS,
                createResource(childId.getFullId()));
        final var received = containmentTriplesService.getContainedBy(transaction, mementoDescResc).collect(toModel());
        matchModels(expectedModel, received);
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
