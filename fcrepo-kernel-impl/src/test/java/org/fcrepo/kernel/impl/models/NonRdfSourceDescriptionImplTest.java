/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createStringLiteral;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.apache.jena.rdf.model.Model;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author bbpennel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class NonRdfSourceDescriptionImplTest {
    @Mock
    private Transaction transaction;

    @Mock
    private PersistentStorageSessionManager psSessionManager;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private UserTypesCache userTypesCache;

    @Mock
    private Binary binaryResource;

    private String descId;
    private FedoraId descFedoraId;
    private String binaryId;
    private FedoraId binaryFedoraId;
    private NonRdfSourceDescriptionImpl description;

    @BeforeEach
    public void setup() throws Exception {
        binaryFedoraId = FedoraId.create(UUID.randomUUID().toString());
        binaryId = binaryFedoraId.getResourceId();
        descFedoraId = binaryFedoraId.asDescription();
        descId = descFedoraId.getResourceId();

        when(psSessionManager.getReadOnlySession()).thenReturn(psSession);
        when(resourceFactory.getResource(any(Transaction.class), any(FedoraId.class))).thenReturn(binaryResource);
        when(binaryResource.getFedoraId()).thenReturn(binaryFedoraId);
        when(binaryResource.getId()).thenReturn(binaryId);

        description = new NonRdfSourceDescriptionImpl(descFedoraId, transaction,
                psSessionManager, resourceFactory, userTypesCache);
        description.setInteractionModel(RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI);
    }

    @Test
    public void testGetId() {
        assertEquals(descId, description.getId());
    }

    @Test
    public void testGetSystemTypes() {
        final List<URI> types = description.getSystemTypes(false);

        assertTrue(types.contains(URI.create(RdfLexicon.RDF_SOURCE.getURI())));
        assertTrue(types.contains(URI.create(RdfLexicon.RESOURCE.toString())));
        assertTrue(types.contains(URI.create(RdfLexicon.FEDORA_RESOURCE.toString())));
        assertTrue(types.contains(URI.create(RdfLexicon.VERSIONED_RESOURCE.toString())));
        assertTrue(types.contains(URI.create(RdfLexicon.VERSIONING_TIMEGATE_TYPE)));
    }

    @Test
    public void testGetDescribedResource() {
        final var described = description.getDescribedResource();

        assertEquals(binaryResource, described);
        assertEquals(binaryId, described.getId());
    }

    @Test
    public void testGetDescribedResourceOfMemento() throws Exception {
        final var mementoId = descFedoraId.asMemento("20200309172118");
        description = new NonRdfSourceDescriptionImpl(mementoId, transaction,
                psSessionManager, resourceFactory, userTypesCache);
        description.setInteractionModel(RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI);

        reset(resourceFactory);
        final var binaryMementoId = binaryFedoraId.asMemento("20200309172118");
        when(resourceFactory.getResource(any(Transaction.class), eq(binaryMementoId))).thenReturn(binaryResource);
        when(binaryResource.getFedoraId()).thenReturn(binaryMementoId);
        when(binaryResource.getId()).thenReturn(binaryMementoId.getResourceId());

        final var described = description.getDescribedResource();

        assertEquals(binaryResource, described);
        assertEquals(binaryMementoId.getResourceId(), described.getId());
        assertEquals(binaryMementoId, described.getFedoraId());
    }

    @Test
    public void testGetTriples() {
        // Create test triples with NRDS as subject
        final Model model = createDefaultModel();
        final var subject = createResource(descId);
        final var predicate = createProperty("http://example.org/test");
        final var object = createStringLiteral("value1");
        model.add(subject, predicate, object);

        final var binarySubject = createResource(binaryId);
        final var object2 = createStringLiteral("value2");
        model.add(binarySubject, predicate, object2);

        // Create another triple with a different subject
        final var otherSubject = createResource("http://example.org/other");
        final var object3 = createStringLiteral("value3");
        model.add(otherSubject, predicate, object3);

        final var userStream = fromModel(subject.asNode(), model);

        when(psSessionManager.getReadOnlySession()).thenReturn(psSession);
        when(psSession.getTriples(eq(descFedoraId), any())).thenReturn(userStream);

        final var results = description.getTriples().collect(Collectors.toList());
        assertEquals(3, results.size());

        // Find the triple with the resource as the subject
        final var descTriple = results.stream()
                .filter(t -> t.getObject().getLiteralValue().toString().equals("value1"))
                .findFirst().orElse(null);
        assertNotNull(descTriple);
        assertEquals(binarySubject.getURI(), descTriple.getSubject().getURI());

        final var binaryTriple = results.stream()
                .filter(t -> t.getObject().getLiteralValue().toString().equals("value2"))
                .findFirst().orElse(null);
        assertNotNull(binaryTriple);
        assertEquals(binarySubject.getURI(), binaryTriple.getSubject().getURI());

        final var otherTriple = results.stream()
                .filter(t -> t.getObject().getLiteralValue().toString().equals("value3"))
                .findFirst().orElse(null);
        assertNotNull(otherTriple);
        assertEquals(otherSubject.getURI(), otherTriple.getSubject().getURI());
    }
}
