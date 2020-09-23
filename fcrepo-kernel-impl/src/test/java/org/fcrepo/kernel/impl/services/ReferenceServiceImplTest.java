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

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Reference Service Tests
 * @author whikloj
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ReferenceServiceImplTest {

    @Inject
    private ReferenceService referenceService;

    @Mock
    private FedoraResource targetResource;

    @Mock
    private Binary binaryResource;

    @Mock
    private NonRdfSourceDescription binaryDescriptionResource;

    private FedoraId subject1Id;

    private FedoraId subject2Id;

    private Resource subject1;

    private Resource subject2;

    private Resource target;

    private static final Property referenceProp = ResourceFactory.createProperty("http://example.org/pointer");

    private String transactionId;

    private static final String TEST_USER = "someUser";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        transactionId = UUID.randomUUID().toString();
        subject1Id = FedoraId.create(UUID.randomUUID().toString());
        subject2Id = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId targetId = FedoraId.create(UUID.randomUUID().toString());
        when(targetResource.getFedoraId()).thenReturn(targetId);
        subject1 =  ResourceFactory.createResource(subject1Id.getFullId());
        subject2 = ResourceFactory.createResource(subject2Id.getFullId());
        target = ResourceFactory.createResource(targetId.getFullId());
        referenceService.reset();
    }

    @Test
    public void testAddAReference() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        referenceService.commitTransaction(transactionId);

        final List<Triple> refs = referenceService.getInboundReferences(null, targetResource)
                .collect(Collectors.toList());

        assertEquals(1, refs.size());
        assertEquals(subject1Id.getFullId(), refs.get(0).getSubject().getURI());
        assertEquals(referenceProp.getURI(), refs.get(0).getPredicate().getURI());
    }

    @Test
    public void testAddNoReference() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, "http://some/text/uri");
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        referenceService.commitTransaction(transactionId);
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
    }

    @Test
    public void testAdd2References() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        referenceService.commitTransaction(transactionId);
        final List<Triple> refs = referenceService.getInboundReferences(null, targetResource).collect(
                Collectors.toList());
        assertEquals(1, refs.size());
        assertEquals(subject1Id.getFullId(), refs.get(0).getSubject().getURI());
        // Now make another object reference the target.
        model.remove(subject1, referenceProp, target);
        model.add(subject2, referenceProp, target);
        final RdfStream stream2 = fromModel(subject2.asNode(),
                model);
        referenceService.updateReferences(transactionId, subject2Id, TEST_USER, stream2);
        referenceService.commitTransaction(transactionId);
        final List<Triple> refs2 = referenceService.getInboundReferences(null, targetResource).collect(
                Collectors.toList());
        assertEquals(2, refs2.size());
        assertEquals(subject2Id.getFullId(), refs2.get(1).getSubject().getURI());
    }

    @Test
    public void testAddExternalReference() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        final Model model = createDefaultModel();
        final Resource external = ResourceFactory.createResource("http://someother.org/pointer");
        model.add(external, referenceProp, target);
        final RdfStream stream = fromModel(external.asNode(), model);

        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        referenceService.commitTransaction(transactionId);
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        model.remove(external, referenceProp, target);
        model.add(subject1, referenceProp, target);
        final RdfStream stream2 = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream2);
        referenceService.commitTransaction(transactionId);
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());

    }

    @Test
    public void testRollback() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        // Still nothing outside the transaction.
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
        // One inside the transaction
        assertEquals(1, referenceService.getInboundReferences(transactionId, targetResource).count());
        referenceService.rollbackTransaction(transactionId);
        // Still nothing outside or inside the transaction.
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
        assertEquals(0, referenceService.getInboundReferences(transactionId, targetResource).count());
    }

    @Test
    public void testCommit() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        // Still nothing outside the transaction.
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
        // One inside the transaction
        assertEquals(1, referenceService.getInboundReferences(transactionId, targetResource).count());
        referenceService.commitTransaction(transactionId);
        // Now 1 outside or inside the transaction.
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());
        assertEquals(1, referenceService.getInboundReferences(transactionId, targetResource).count());
    }

    @Test
    public void commitTransactionNotExist() {
        final String txID = UUID.randomUUID().toString();
        referenceService.commitTransaction(txID);
    }

    @Test
    public void ensureNoCrossTransactionLeakage() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);

        final String transaction2 = UUID.randomUUID().toString();
        // Still not public.
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
        // Available to current transaction
        assertEquals(1, referenceService.getInboundReferences(transactionId, targetResource).count());
        // But not to other transactions.
        assertEquals(0, referenceService.getInboundReferences(transaction2, targetResource).count());
        referenceService.commitTransaction(transactionId);
        // Now all return the one.
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());
        assertEquals(1, referenceService.getInboundReferences(transactionId, targetResource).count());
        assertEquals(1, referenceService.getInboundReferences(transaction2, targetResource).count());
    }

    @Test
    public void testAddAndRemove() {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        // Add a reference.
        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        referenceService.commitTransaction(transactionId);
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());

        // Change the RDF to remove the reference.
        final String transaction2 = UUID.randomUUID().toString();
        model.add(subject1, ResourceFactory.createProperty("http://someother/description"), "Some text");
        model.remove(subject1, referenceProp, target);
        final RdfStream stream2 = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transaction2, subject1Id, TEST_USER, stream2);
        // Reference still available outside transaction.
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());
        // But gone inside the transaction
        assertEquals(0, referenceService.getInboundReferences(transaction2, targetResource).count());
        referenceService.commitTransaction(transaction2);
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
    }

    @Test
    public void testBinaryDescriptionListAllReferences() {
        final FedoraId binaryId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId descriptionId = binaryId.resolve(FCR_METADATA);
        final Resource binaryUri = ResourceFactory.createResource(binaryId.getFullId());
        final Resource binaryDescUri = ResourceFactory.createResource(descriptionId.getFullId());
        when(binaryResource.getFedoraId()).thenReturn(binaryId);
        when(binaryDescriptionResource.getFedoraId()).thenReturn(descriptionId);
        when(binaryDescriptionResource.getDescribedResource()).thenReturn(binaryResource);
        assertEquals(0, referenceService.getInboundReferences(null, binaryDescriptionResource).count());

        // Add a reference to binary
        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, binaryUri);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        // Check before committing
        assertEquals(0, referenceService.getInboundReferences(null, binaryDescriptionResource).count());
        assertEquals(1, referenceService.getInboundReferences(transactionId, binaryDescriptionResource)
                .count());
        // Commit
        referenceService.commitTransaction(transactionId);
        assertEquals(1, referenceService.getInboundReferences(null, binaryDescriptionResource).count());

        // Add a second to the description.
        final Model model2 = createDefaultModel();
        model2.add(subject2, referenceProp, binaryDescUri);
        final RdfStream stream2 = fromModel(subject2.asNode(), model2);
        referenceService.updateReferences(transactionId, subject2Id, TEST_USER, stream2);
        // One reference outside the transaction
        assertEquals(1, referenceService.getInboundReferences(null, binaryDescriptionResource).count());
        // Two inside
        assertEquals(2, referenceService.getInboundReferences(transactionId, binaryDescriptionResource)
                .count());
        // Commit the transaction
        referenceService.commitTransaction(transactionId);

        // Verify both the reference to the binary and the description are returned.
        final Model rdfModel = referenceService.getInboundReferences(null, binaryDescriptionResource).collect(
                toModel());
        assertTrue(rdfModel.contains(subject1, referenceProp, binaryUri));
        assertTrue(rdfModel.contains(subject2, referenceProp, binaryDescUri));
    }

    @Test
    public void testReferencesFromTwoSources() throws Exception {
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());

        // Add a reference.
        final Model model = createDefaultModel();
        model.add(subject1, referenceProp, target);
        final RdfStream stream = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject1Id, TEST_USER, stream);
        referenceService.commitTransaction(transactionId);
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());

        // Add the same reference from another resource.
        final RdfStream stream2 = fromModel(subject1.asNode(), model);
        referenceService.updateReferences(transactionId, subject2Id, TEST_USER, stream2);
        referenceService.commitTransaction(transactionId);
        assertEquals(2, referenceService.getInboundReferences(null, targetResource).count());
        final Triple expectedTriple = Triple.create(subject1.asNode(), referenceProp.asNode(), target.asNode());
        assertTrue(referenceService.getInboundReferences(null, targetResource)
                .allMatch(t -> t.equals(expectedTriple)));
        // Delete the first resource and see the second reference remains.
        referenceService.deleteAllReferences(transactionId, subject1Id);
        referenceService.commitTransaction(transactionId);
        assertEquals(1, referenceService.getInboundReferences(null, targetResource).count());
        assertTrue(referenceService.getInboundReferences(null, targetResource)
                .allMatch(t -> t.equals(expectedTriple)));

        // Delete the second resource and see all references gone.
        referenceService.deleteAllReferences(transactionId, subject2Id);
        referenceService.commitTransaction(transactionId);
        assertEquals(0, referenceService.getInboundReferences(null, targetResource).count());
    }
}
