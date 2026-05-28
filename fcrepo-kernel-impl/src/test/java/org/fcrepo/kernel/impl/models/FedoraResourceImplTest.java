/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.models;

import static java.net.URI.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEGATE_TYPE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author pwinckles
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FedoraResourceImplTest {

    @Mock
    private TimeMap timeMap;

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceHeaders headers;

    @Mock
    private Transaction transaction;

    private static final String ID = "info:fedora/test";

    private static final FedoraId FEDORA_ID = FedoraId.create(ID);

    @Test
    public void findMementoWhenOnlyOneAndBeforeSearch() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172117");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/0", match.getId());
    }

    @Test
    public void findClosestMementoWhenMultiple() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172117", "20200309172118", "20200309172119");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/1", match.getId());
    }

    @Test
    public void findClosestMementoWhenMultipleNoneExact() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172116", "20200309172117", "20200309172119");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/1", match.getId());
    }

    @Test
    public void findClosestMementoMultipleSameSecond() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172117", "20200309172117", "20200309172117");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/2", match.getId());
    }

    @Test
    public void findMementoWhenNonBeforeSearch() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172119", "20200309172120", "20200309172121");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/0", match.getId());
    }

    @Test
    public void findNoMementoWhenThereAreNone() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos();
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertNull(match, "Should not find a memento");
    }

    @Test
    public void testTypesRdfSource() throws Exception {
        final var subject = createResource(ID);
        final String exampleType = "http://example.org/customType";
        final Model userModel = createDefaultModel();
        userModel.add(subject, type, createResource(exampleType));
        final var userStream = fromModel(subject.asNode(), userModel);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);
        when(psSession.getTriples(eq(FEDORA_ID), any())).thenReturn(userStream);

        final List<URI> expectedTypes = List.of(
                create(exampleType),
                create(BASIC_CONTAINER.toString()),
                create(RESOURCE.toString()),
                create(FEDORA_RESOURCE.toString()),
                create(VERSIONED_RESOURCE.getURI()),
                create(VERSIONING_TIMEGATE_TYPE)
        );

        when(transaction.isShortLived()).thenReturn(true);

        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        resource.setInteractionModel(BASIC_CONTAINER.toString());
        resource.setIsArchivalGroup(false);
        final var resourceTypes = resource.getTypes();

        // Initial lengths are the same
        assertEquals(expectedTypes.size(), resourceTypes.size());
        // Only keep the types in the expected list.
        resourceTypes.retainAll(expectedTypes);
        // Lengths are still the same.
        assertEquals(expectedTypes.size(), resourceTypes.size());
    }

    @Test
    public void testTypesNonRdfSource() throws Exception {
        final var descriptionFedoraId = FEDORA_ID.asDescription();
        final var subject = createResource(ID);
        final String exampleType = "http://example.org/customType";
        final Model userModel = createDefaultModel();
        userModel.add(subject, type, createResource(exampleType));
        final var userStream = fromModel(subject.asNode(), userModel);

        final var description = new NonRdfSourceDescriptionImpl(descriptionFedoraId, null, sessionManager,
                resourceFactory, null);

        when(resourceFactory.getResource(any(Transaction.class), eq(descriptionFedoraId))).thenReturn(description);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);
        when(psSession.getTriples(eq(descriptionFedoraId), any())).thenReturn(userStream);

        final List<URI> expectedTypes = List.of(
                create(exampleType),
                create(NON_RDF_SOURCE.toString()),
                create(RESOURCE.toString()),
                create(FEDORA_RESOURCE.toString()),
                create(FEDORA_BINARY.toString()),
                create(VERSIONED_RESOURCE.getURI()),
                create(VERSIONING_TIMEGATE_TYPE)
        );

        when(transaction.isShortLived()).thenReturn(true);

        final var resource = new BinaryImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        resource.setInteractionModel(NON_RDF_SOURCE.toString());
        resource.setIsArchivalGroup(false);
        final var resourceTypes = resource.getTypes();

        // Initial lengths are the same
        assertEquals(expectedTypes.size(), resourceTypes.size());
        // Only keep the types in the expected list.
        resourceTypes.retainAll(expectedTypes);
        // Lengths are still the same.
        assertEquals(expectedTypes.size(), resourceTypes.size());
    }

    @Test
    public void testGetChildren() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, null, sessionManager, resourceFactory, null);
        assertEquals(0, resource.getChildren().count());
    }




    @Test
    public void testGetSystemTypesWithRdfFlag() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        resource.setInteractionModel(BASIC_CONTAINER.toString());
        resource.setIsArchivalGroup(true);
        resource.setIsMemento(false);

        // Get types for RDF serialization
        final var rdfTypes = resource.getSystemTypes(true);

        // Should include base types and interaction model
        assertTrue(rdfTypes.contains(create(BASIC_CONTAINER.toString())));
        assertTrue(rdfTypes.contains(create(RESOURCE.toString())));
        assertTrue(rdfTypes.contains(create(FEDORA_RESOURCE.toString())));

        // Should NOT include non-RDF types
        assertFalse(rdfTypes.contains(create(RdfLexicon.ARCHIVAL_GROUP.toString())));
        assertFalse(rdfTypes.contains(create(VERSIONED_RESOURCE.toString())));
        assertFalse(rdfTypes.contains(create(VERSIONING_TIMEGATE_TYPE)));
    }

    @Test
    public void testGetSystemTypesForMemento() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        resource.setInteractionModel(BASIC_CONTAINER.toString());
        resource.setIsArchivalGroup(false);
        resource.setIsMemento(true);
        resource.setMementoDatetime(Instant.now());

        final var types = resource.getSystemTypes(false);

        assertTrue(types.contains(create(BASIC_CONTAINER.toString())));
        assertTrue(types.contains(create(RdfLexicon.MEMENTO_TYPE)));
        assertFalse(types.contains(create(VERSIONED_RESOURCE.toString())));
        assertFalse(types.contains(create(VERSIONING_TIMEGATE_TYPE)));
    }

    @Test
    public void testGetSystemTypesForRepositoryRoot() {
        final var rootId = FedoraId.create("/");
        final var rootResource = new FedoraResourceImpl(rootId, transaction, sessionManager, resourceFactory, null);
        rootResource.setInteractionModel(BASIC_CONTAINER.toString());

        final var types = rootResource.getSystemTypes(false);

        assertTrue(types.contains(create(RdfLexicon.REPOSITORY_ROOT.toString())));
    }

    @Test
    public void testGetStorageRelativePath() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);

        // Test with null path
        resource.setStorageRelativePath(null);
        assertNull(resource.getStorageRelativePath());

        // Test with a valid storage path
        final String storagePath = "some/resource/path/v1/content/.fcrepo/fcr-root.json";
        resource.setStorageRelativePath(storagePath);
        assertEquals(Paths.get("some/resource/path"), resource.getStorageRelativePath());
    }

    @Test
    public void testGetParent() throws Exception {
        final var parentId = FedoraId.create("info:fedora/parent");
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        resource.setParentId(parentId);

        final var mockParent = mock(FedoraResource.class);
        when(resourceFactory.getResource(any(Transaction.class), eq(parentId)))
                .thenReturn(mockParent);

        final var parent = resource.getParent();
        assertEquals(mockParent, parent);
    }

    @Test
    public void testGetDescribedResource() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        assertEquals(resource, resource.getDescribedResource());
    }

    @Test
    public void testGetOriginalResource_NotMemento() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        resource.setIsMemento(false);

        assertEquals(resource, resource.getOriginalResource());
    }

    @Test
    public void testGetOriginalResource_AsMemento() throws Exception {
        final var mementoId = FEDORA_ID.asMemento("20200309172118");
        final var resource = new FedoraResourceImpl(mementoId, transaction, sessionManager, resourceFactory, null);
        resource.setIsMemento(true);

        final var mockOriginal = mock(FedoraResource.class);
        when(resourceFactory.getResource(any(Transaction.class), eq(FEDORA_ID)))
                .thenReturn(mockOriginal);

        assertEquals(mockOriginal, resource.getOriginalResource());
    }

    @Test
    public void testGetArchivalGroupId() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);

        // Initially should be empty
        assertTrue(resource.getArchivalGroupId().isEmpty());

        // Set and check
        final var agId = FedoraId.create("info:fedora/ag");
        resource.setArchivalGroupId(agId);
        assertEquals(agId, resource.getArchivalGroupId().get());
    }

    @Test
    public void testGetContainer() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);
        final var mockContainer = mock(FedoraResource.class);

        when(resourceFactory.getContainer(any(Transaction.class), eq(FEDORA_ID)))
                .thenReturn(mockContainer);

        assertEquals(mockContainer, resource.getContainer());
    }

    @Test
    public void testIsOriginalResource() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, transaction, sessionManager, resourceFactory, null);

        resource.setIsMemento(false);
        assertTrue(resource.isOriginalResource());

        resource.setIsMemento(true);
        assertFalse(resource.isOriginalResource());
    }

    private void expectMementos(final String... instants) {
        final var mementos = new ArrayList<FedoraResource>(instants.length);
        for (int i = 0; i < instants.length; i++) {
            mementos.add(memento(String.valueOf(i), instant(instants[i])));
        }
        when(timeMap.getChildren()).thenReturn(mementos.stream());
    }

    private FedoraResource resourceWithMockedTimeMap() {
        final var resource = spy(new FedoraResourceImpl(FEDORA_ID, null, sessionManager,
                resourceFactory, null));
        doReturn(timeMap).when(resource).getTimeMap();
        return resource;
    }

    private FedoraResource memento(final String id, final Instant instant) {
        final String mementoTime = VersionService.MEMENTO_LABEL_FORMATTER.format(instant);
        final FedoraId fedoraID = FedoraId.create(id, FCR_VERSIONS, mementoTime);
        final var memento = new FedoraResourceImpl(fedoraID, null, sessionManager, resourceFactory, null);
        memento.setIsMemento(true);
        memento.setMementoDatetime(instant);
        return memento;
    }

    private Instant instant(final String timestamp) {
        return Instant.from(VersionService.MEMENTO_LABEL_FORMATTER.parse(timestamp));
    }

}
