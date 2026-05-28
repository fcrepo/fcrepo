/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.impl.TestTransactionHelper;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
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

import jakarta.inject.Inject;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration("/containmentIndexTest.xml")
public class ResourceFactoryImplTest {

    private final static Instant CREATED_DATE = Instant.parse("2019-11-12T10:00:30.0Z");

    private final static String CREATED_BY = "user1";

    private final static Instant LAST_MODIFIED_DATE = Instant.parse("2019-11-12T14:11:05.0Z");

    private final static String LAST_MODIFIED_BY = "user2";

    private final static String STATE_TOKEN = "stately_value";

    private final static long CONTENT_SIZE = 100L;

    private final static String MIME_TYPE = "text/plain";

    private final static String FILENAME = "testfile.txt";

    private final static URI DIGEST = URI.create("sha:12345");

    private final static Collection<URI> DIGESTS = asList(DIGEST);

    private final static String RELATIVE_RESOURCE_PATH = "some/ocfl/path/v1/content/.fcrepo/fcr-container.json";

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private PersistentStorageSession psSession;

    private ResourceHeadersImpl resourceHeaders;

    private String fedoraIdStr;

    private String sessionId;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    private FedoraId fedoraId;

    private String fedoraMementoIdStr;

    @Mock
    private Transaction mockTx;

    @Mock
    private FedoraResource mockResource;

    @Inject
    private ContainmentIndex containmentIndex;

    @InjectMocks
    private ResourceFactoryImpl factory;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        fedoraIdStr = FEDORA_ID_PREFIX + "/" + UUID.randomUUID().toString();
        fedoraId = FedoraId.create(fedoraIdStr);
        fedoraMementoIdStr = fedoraIdStr + "/fcr:versions/20000102120000";

        sessionId = UUID.randomUUID().toString();
        mockTx = TestTransactionHelper.mockTransaction(sessionId, false);

        factory = new ResourceFactoryImpl();

        setField(factory, "persistentStorageSessionManager", sessionManager);
        setField(factory, "containmentIndex", containmentIndex);

        resourceHeaders = new ResourceHeadersImpl();
        resourceHeaders.setId(fedoraId);

        when(sessionManager.getSession(mockTx)).thenReturn(psSession);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);

        when(psSession.getHeaders(eq(fedoraId), nullable(Instant.class))).thenReturn(resourceHeaders);
    }

    @AfterEach
    public void cleanUp() {
        when(mockResource.getFedoraId()).thenReturn(rootId);
        containmentIndex.reset();
    }

    @Test
    public void getResource_ObjectNotFound() throws Exception {
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);

        assertThrows(PathNotFoundException.class, () -> factory.getResource(mockTx, fedoraId));
    }

    @Test
    public void getResource_NoInteractionModel() throws Exception {
        resourceHeaders.setInteractionModel(null);

        assertThrows(ResourceTypeException.class, () -> factory.getResource(mockTx, fedoraId));
    }

    @Test
    public void getResource_UnknownInteractionModel() throws Exception {
        resourceHeaders.setInteractionModel("http://example.com/mystery_stroop");

        assertThrows(ResourceTypeException.class, () -> factory.getResource(mockTx, fedoraId));
    }

    @Test
    public void getResource_BasicContainer() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        // Need to make a short lived transaction or the verify will fail.
        final var shortTx = TestTransactionHelper.mockTransaction(sessionId, true);
        final var resc = factory.getResource(ReadOnlyTransaction.INSTANCE, fedoraId);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getReadOnlySession();
    }

    @Test
    public void getResource_BasicContainer_WithParent() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var parentId = FedoraId.create(FEDORA_ID_PREFIX + UUID.randomUUID().toString());
        resourceHeaders.setParent(parentId);

        final var parentHeaders = new ResourceHeadersImpl();
        parentHeaders.setId(parentId);
        populateHeaders(parentHeaders, DIRECT_CONTAINER);

        when(psSession.getHeaders(parentId, null)).thenReturn(parentHeaders);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        final var parentResc = resc.getParent();
        assertInstanceOf(Container.class, parentResc, "Parent must be a container");
        assertEquals(parentId, parentResc.getFedoraId());
        assertStateFieldsMatches(parentResc);
    }

    @Test
    public void getResource_BasicContainer_InTransaction() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getSession(mockTx);
    }

    @Test
    public void getResource_BasicContainer_Cast_InTransaction() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId, Container.class);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        verify(sessionManager).getSession(mockTx);
    }

    @Test
    public void getResource_DirectContainer() throws Exception {
        populateHeaders(resourceHeaders, DIRECT_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test
    public void getResource_IndirectContainer() throws Exception {
        populateHeaders(resourceHeaders, INDIRECT_CONTAINER);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
    }

    @Test
    public void getResource_Binary() throws Exception {
        populateHeaders(resourceHeaders, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(resourceHeaders);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(Binary.class, resc, "Factory must return a binary");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
        assertBinaryFieldsMatch(resc);
    }

    @Test
    public void getResource_Binary_StorageException() throws Exception {
        when(psSession.getHeaders(fedoraId, null)).thenThrow(new PersistentStorageException("Boom"));

        populateHeaders(resourceHeaders, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(resourceHeaders);

        assertThrows(RepositoryRuntimeException.class, () -> factory.getResource(mockTx, fedoraId));
    }

    @Test
    public void getResource_ExternalBinary() throws Exception {
        populateHeaders(resourceHeaders, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(resourceHeaders);
        final String externalUrl = "http://example.com/stuff";
        resourceHeaders.setExternalUrl(externalUrl);
        resourceHeaders.setExternalHandling(PROXY);

        final var resc = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(Binary.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);
        assertBinaryFieldsMatch(resc);
        final var binary = (Binary) resc;
        assertEquals(externalUrl, binary.getExternalURL());
        assertTrue(binary.isProxy());
    }

    @Test
    public void getNonRdfSourceDescription() throws Exception {
        final FedoraId descFedoraId = FedoraId.create(FEDORA_ID_PREFIX + "/object1/fcr:metadata");

        final var headers = new ResourceHeadersImpl();
        headers.setId(descFedoraId);
        populateHeaders(headers, ResourceFactory.createResource(FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI));
        when(psSession.getHeaders(descFedoraId, null)).thenReturn(headers);

        final var resc = factory.getResource(mockTx, descFedoraId);
        assertInstanceOf(NonRdfSourceDescriptionImpl.class, resc, "Factory must return a NonRdfSourceDescripton");
    }

    @Test
    public void getChildren_NoChildren() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var childrenStream = factory.getChildren(mockTx, fedoraId);

        assertEquals(0, childrenStream.count());
    }

    @Test
    public void getChildren_DoesNotExist() throws Exception {
        final var childrenStream = factory.getChildren(mockTx, fedoraId);
        assertEquals(0, childrenStream.count());
    }

    @Test
    public void getChildren_WithChildren() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var child1Id = FedoraId.create(UUID.randomUUID().toString());
        final var child1Headers = new ResourceHeadersImpl();
        child1Headers.setId(child1Id);
        populateHeaders(child1Headers, BASIC_CONTAINER);
        when(psSession.getHeaders(child1Id, null)).thenReturn(child1Headers);

        final var childNestedId = FedoraId.create(UUID.randomUUID().toString());
        final var childNestedHeaders = new ResourceHeadersImpl();
        childNestedHeaders.setId(childNestedId);
        populateHeaders(childNestedHeaders, BASIC_CONTAINER);
        when(psSession.getHeaders(childNestedId, null)).thenReturn(childNestedHeaders);

        final var child2Id = FedoraId.create(UUID.randomUUID().toString());
        final var child2Headers = new ResourceHeadersImpl();
        child2Headers.setId(child2Id);
        populateHeaders(child2Headers, NON_RDF_SOURCE);
        populateInternalBinaryHeaders(child2Headers);
        when(psSession.getHeaders(child2Id, null)).thenReturn(child2Headers);

        containmentIndex.addContainedBy(mockTx, rootId, fedoraId);
        containmentIndex.addContainedBy(mockTx, fedoraId, child1Id);
        containmentIndex.addContainedBy(mockTx, child1Id, childNestedId);
        containmentIndex.addContainedBy(mockTx, fedoraId, child2Id);
        containmentIndex.commitTransaction(mockTx);

        final var childrenStream = factory.getChildren(mockTx, fedoraId);
        final var childrenList = childrenStream.collect(Collectors.toList());

        assertEquals(2, childrenList.size());

        final var child1 = childrenList.stream().filter(c -> c.getFedoraId().equals(child1Id)).findFirst();
        assertTrue(child1.isPresent());
        assertInstanceOf(Container.class, child1.get());

        final var child2 = childrenList.stream().filter(c -> c.getFedoraId().equals(child2Id)).findFirst();
        assertTrue(child2.isPresent());
        assertInstanceOf(Binary.class, child2.get());
    }

    @Test
    public void getContainer_ResourceHasContainer() throws Exception {
        // Setup container
        final var containerId = FedoraId.create(UUID.randomUUID().toString());
        final var containerHeaders = new ResourceHeadersImpl();
        containerHeaders.setId(containerId);
        populateHeaders(containerHeaders, BASIC_CONTAINER);

        when(psSession.getHeaders(containerId, null)).thenReturn(containerHeaders);

        containmentIndex.addContainedBy(mockTx, containerId, fedoraId);

        // Call and verify
        final var container = factory.getContainer(mockTx, fedoraId);

        assertNotNull(container);
        assertEquals(containerId, container.getFedoraId());
        assertInstanceOf(Container.class, container);
    }

    @Test
    public void getContainer_ResourceHasNoContainer() throws Exception {
        // Did not add any containment statements, so should return null
        final var container = factory.getContainer(mockTx, fedoraId);

        assertNull(container);
    }

    @Test
    public void getContainer_ContainerDoesNotExist() throws Exception {
        // Setup a container ID that doesn't exist
        final var containerId = FedoraId.create(UUID.randomUUID().toString());

        // Setup containment index to return the container
        when(containmentIndex.getContainedBy(mockTx, fedoraId)).thenReturn(containerId.getResourceId());

        // Make the container not found
        when(psSession.getHeaders(containerId, null)).thenThrow(PersistentItemNotFoundException.class);

        // Call and verify
        final var container = factory.getContainer(mockTx, fedoraId);

        assertNull(container);
    }

    @Test
    public void getResource_DeletedResource() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);
        resourceHeaders.setDeleted(true);

        final var resource = factory.getResource(mockTx, fedoraId);

        assertInstanceOf(TombstoneImpl.class, resource, "Factory must return a tombstone for deleted resource");
        assertEquals(fedoraIdStr, resource.getId());
        assertEquals(LAST_MODIFIED_DATE, resource.getLastModifiedDate());

        // We should be able to get the original resource through the tombstone
        final var original = ((TombstoneImpl) resource).getDeletedObject();
        assertInstanceOf(Container.class, original);
    }

    @Test
    public void getResource_Timemap() throws Exception {
        // Create a timemap ID
        final var timemapId = fedoraId.asTimemap();

        // Setup headers for the original resource
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        // Setup the session to return resource headers when asked for timemap
        when(psSession.getHeaders(timemapId, null)).thenReturn(resourceHeaders);

        final var resource = factory.getResource(mockTx, timemapId);
        assertInstanceOf(TimeMap.class, resource, "Factory must return a TimeMap");
    }

    @Test
    public void getResource_BasicContainer_WithHeaders() throws Exception {
        populateHeaders(resourceHeaders, BASIC_CONTAINER);

        final var resc = factory.getResource(mockTx, resourceHeaders);

        assertInstanceOf(Container.class, resc, "Factory must return a container");
        assertEquals(fedoraIdStr, resc.getId());
        assertStateFieldsMatches(resc);

        // No session calls should be made when headers are provided
        verify(sessionManager, never()).getSession(mockTx);
    }

    private void assertStateFieldsMatches(final FedoraResource resc) {
        assertEquals(CREATED_DATE, resc.getCreatedDate());
        assertEquals(CREATED_BY, resc.getCreatedBy());
        assertEquals(LAST_MODIFIED_DATE, resc.getLastModifiedDate());
        assertEquals(LAST_MODIFIED_BY, resc.getLastModifiedBy());
        assertEquals(STATE_TOKEN, resc.getStateToken());
        assertEquals(STATE_TOKEN, resc.getEtagValue());
    }

    private static void populateHeaders(final ResourceHeadersImpl headers, final Resource ixModel) {
        headers.setInteractionModel(ixModel.getURI());
        headers.setCreatedBy(CREATED_BY);
        headers.setCreatedDate(CREATED_DATE);
        headers.setLastModifiedBy(LAST_MODIFIED_BY);
        headers.setLastModifiedDate(LAST_MODIFIED_DATE);
        headers.setStateToken(STATE_TOKEN);
        headers.setStorageRelativePath(RELATIVE_RESOURCE_PATH);
    }

    private void assertBinaryFieldsMatch(final FedoraResource resc) {
        final Binary binary = (Binary) resc;
        assertEquals(CONTENT_SIZE, binary.getContentSize());
        assertEquals(MIME_TYPE, binary.getMimeType());
        assertEquals(DIGEST, binary.getContentDigests().stream().findFirst().get());
        assertEquals(FILENAME, binary.getFilename());
    }

    private static void populateInternalBinaryHeaders(final ResourceHeadersImpl headers) {
        headers.setContentSize(CONTENT_SIZE);
        headers.setMimeType(MIME_TYPE);
        headers.setDigests(DIGESTS);
        headers.setFilename(FILENAME);
    }

}
