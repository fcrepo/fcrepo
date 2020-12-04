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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.inject.Inject;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InteractionModelViolationException;
import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.impl.operations.CreateNonRdfSourceOperation;
import org.fcrepo.kernel.impl.operations.NonRdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author bseeger
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class CreateResourceServiceImplTest {

    private static final List<String> STRING_TYPES_NOT_VALID = Arrays.asList(MEMENTO_TYPE, RESOURCE.toString(),
            FEDORA_RESOURCE.toString());

    private static final List<String> STRING_TYPES_VALID = Arrays.asList(MEMENTO_TYPE, CONTAINER.toString(),
            BASIC_CONTAINER.toString());

    private final String defaultInteractionModel = DEFAULT_INTERACTION_MODEL.toString();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private PersistentStorageSessionManager psManager;

    private RdfSourceOperationFactory rdfSourceOperationFactory;

    private NonRdfSourceOperationFactory nonRdfSourceOperationFactory;

    @Inject
    private ContainmentIndex containmentIndex;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceHeaders resourceHeaders;

    @Mock
    private ExternalContent extContent;

    @Mock
    private FedoraResource fedoraResource;

    @Mock
    private Transaction transaction;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Captor
    private ArgumentCaptor<ResourceOperation> operationCaptor;

    @Mock
    private EventAccumulator eventAccumulator;

    @InjectMocks
    private CreateResourceServiceImpl createResourceService;

    private static final String TX_ID = "tx1234";

    private static final String USER_PRINCIPAL = "fedoraUser";

    private static final String FILENAME = "file.html";

    private static final String CONTENT_TYPE = "text/html";

    private static final Long CONTENT_SIZE = 100L;

    private static final String EXTERNAL_URL = "http://example.org/rest/object";

    private static final String EXTERNAL_CONTENT_TYPE = "text/plain";

    private final Model model = ModelFactory.createDefaultModel();

    private static final Collection<URI> DIGESTS = singleton(URI.create("urn:sha1:12345abced"));

    private static final List<FedoraId> cleanupList = new ArrayList<>() ;

    private final FedoraId rootId = FedoraId.getRepositoryRootId();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        rdfSourceOperationFactory = new RdfSourceOperationFactoryImpl();
        setField(createResourceService, "rdfSourceOperationFactory", rdfSourceOperationFactory);
        nonRdfSourceOperationFactory = new NonRdfSourceOperationFactoryImpl();
        setField(createResourceService, "nonRdfSourceOperationFactory", nonRdfSourceOperationFactory);
        setField(createResourceService, "containmentIndex", containmentIndex);
        setField(createResourceService, "eventAccumulator", eventAccumulator);
        setField(createResourceService, "referenceService", referenceService);
        setField(createResourceService, "membershipService", membershipService);
        when(psManager.getSession(ArgumentMatchers.any())).thenReturn(psSession);
        when(transaction.getId()).thenReturn(TX_ID);
        // Always try to clean up root.
        cleanupList.add(FedoraId.getRepositoryRootId());
    }

    @After
    public void cleanUp() {
        containmentIndex.reset();
        cleanupList.clear();
    }



    /**
     * Test trying to add a child to a non-existant parent.
     * We recursive to repository root for a parent, so this is now just creating a ghost node?
     */
    @Test
    public void testNoParentRdf() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, model);
        verify(transaction).lockResource(childId);
    }

    @Test
    public void testParentAg() {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, fedoraId, childId);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.isArchivalGroup()).thenReturn(true);
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, model);
        verify(transaction).lockResource(fedoraId);
        verify(transaction).lockResource(childId);
    }

    @Test
    public void testParentContainerInAg() {
        final FedoraId agId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId fedoraId = agId.resolve(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, fedoraId, childId);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.isArchivalGroup()).thenReturn(false);
        when(resourceHeaders.getArchivalGroupId()).thenReturn(agId);
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, model);
        verify(transaction).lockResource(agId);
        verify(transaction).lockResource(childId);
    }

    /**
     * Test creating a RDFSource with a NonRDFSource parent.
     */
    @Test(expected = InteractionModelViolationException.class)
    public void testParentIsBinaryRdf() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, rootId, fedoraId);
        when(resourceHeaders.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, model);
    }

    /**
     * Test creating a NonRDFSource with a NonRDFSource parent.
     */
    @Test(expected = InteractionModelViolationException.class)
    public void testParentIsBinary() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, rootId, fedoraId);
        when(resourceHeaders.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, FILENAME, CONTENT_SIZE, null,
                DIGESTS, null, null);
    }

    /**
     * Test creating an external NonRDFSource with a NonRDFSource parent.
     * TODO: put/post to a binary parent is tested above, might be a duplicate.
     */
    @Test(expected = InteractionModelViolationException.class)
    public void testParentIsExternal() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, rootId, fedoraId);
        when(resourceHeaders.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, FILENAME, CONTENT_SIZE, null,
                DIGESTS, null, extContent);
    }

    /**
     * Test creating a RDFSource with a RDFSource parent.
     */
    @Test
    public void testParentIsRdf() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, rootId, fedoraId);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, model);
        cleanupList.add(fedoraId);
        verify(psSession).persist(operationCaptor.capture());
        final FedoraId persistedId = operationCaptor.getValue().getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.getFullId().startsWith(fedoraId.getFullId()));
        assertEquals(1, containmentIndex.getContains(transaction.getId(), fedoraId).count());

        verify(transaction).lockResource(childId);
    }

    /**
     * Test creating a NonRDFSource with a RDFSource parent.
     */
    @Test
    public void testParentIsRdfBinary() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");
        containmentIndex.addContainedBy(TX_ID, rootId, fedoraId);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        createResourceService.perform(transaction, USER_PRINCIPAL, childId, CONTENT_TYPE,
                FILENAME, CONTENT_SIZE, null, DIGESTS, null, null);
        cleanupList.add(fedoraId);
        verify(psSession, times(2)).persist(operationCaptor.capture());
        final List<ResourceOperation> operations = operationCaptor.getAllValues();
        final var operation = getOperation(operations, CreateNonRdfSourceOperation.class);
        final FedoraId persistedId = operation.getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.getFullId().startsWith(fedoraId.getFullId()));
        assertBinaryPropertiesPresent(operation);
        assertEquals(fedoraId, operation.getParentId());
        assertEquals(1, containmentIndex.getContains(transaction.getId(), fedoraId).count());
        verify(transaction).lockResource(childId);
    }

    /**
     * Test setting some system properties only accessible in relaxed mode.
     */
    @Test
    public void testRdfSetRelaxedProperties_Post() throws Exception {
        final var createdDate = Instant.parse("2019-11-12T10:00:30.0Z");
        final var lastModifiedDate = Instant.parse("2019-11-12T14:11:05.0Z");
        final String relaxedUser = "relaxedUser";
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("testSlug");
        containmentIndex.addContainedBy(TX_ID, rootId, fedoraId);

        final var resc = model.getResource(childId.getFullId());
        resc.addLiteral(LAST_MODIFIED_DATE, Date.from(lastModifiedDate));
        resc.addLiteral(LAST_MODIFIED_BY, relaxedUser);
        resc.addLiteral(CREATED_DATE, Date.from(createdDate));
        resc.addLiteral(CREATED_BY, relaxedUser);

        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null)).thenThrow(PersistentItemNotFoundException.class);

        when(resourceHeaders.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        try {
            System.setProperty(SERVER_MANAGED_PROPERTIES_MODE, "relaxed");
            createResourceService.perform(transaction, USER_PRINCIPAL, childId, null, model);
            cleanupList.add(fedoraId);
        } finally {
            System.clearProperty(SERVER_MANAGED_PROPERTIES_MODE);
        }

        verify(psSession).persist(operationCaptor.capture());

        final var operation = operationCaptor.getValue();
        final FedoraId persistedId = operation.getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.getFullId().startsWith(fedoraId.getFullId()));
        assertEquals(persistedId, childId);

        final var rdfOp = (RdfSourceOperation) operation;
        assertEquals(relaxedUser, rdfOp.getCreatedBy());
        assertEquals(relaxedUser, rdfOp.getLastModifiedBy());
        assertEquals(createdDate, rdfOp.getCreatedDate());
        assertEquals(lastModifiedDate, rdfOp.getLastModifiedDate());
        assertEquals(1, containmentIndex.getContains(transaction.getId(), fedoraId).count());
        verify(transaction).lockResource(childId);
    }

    /**
     * This test now seems to ensure that the createResourceService will overwrite an existing object
     * TODO: Review expectations
     */
    @Test
    public void testWithBinary() throws Exception {
        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("testSlug");
        containmentIndex.addContainedBy(TX_ID, fedoraId, childId);
        containmentIndex.commitTransaction(transaction.getId());
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        createResourceService.perform(transaction, USER_PRINCIPAL, childId,
                CONTENT_TYPE, FILENAME, CONTENT_SIZE, null, DIGESTS, null, null);
        cleanupList.add(fedoraId);
        verify(psSession, times(2)).persist(operationCaptor.capture());
        final List<ResourceOperation> operations = operationCaptor.getAllValues();
        final var operation = getOperation(operations, CreateNonRdfSourceOperation.class);
        final FedoraId persistedId = operation.getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertEquals(childId, persistedId);
        assertTrue(persistedId.getFullId().startsWith(fedoraId.getFullId()));
        assertBinaryPropertiesPresent(operation);
        assertEquals(fedoraId, operation.getParentId());

        final var descOperation = getOperation(operations, CreateRdfSourceOperation.class);
        assertEquals(persistedId.asDescription(), descOperation.getResourceId());
        assertEquals(1, containmentIndex.getContains(transaction.getId(), fedoraId).count());
        verify(transaction).lockResource(childId);
    }

    @Test
    public void testSendingValidInteractionModel() {
        // If you provide a valid interaction model, you should always get it back.
        final String expected = BASIC_CONTAINER.toString();
        final String model1 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, false, false);
        assertEquals(expected, model1);
        final String model2 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, false, true);
        assertEquals(expected, model2);
        final String model3 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, true, false);
        assertEquals(expected, model3);
        final String model4 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, true, true);
        assertEquals(expected, model4);
        final String model5 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, false, false);
        assertEquals(expected, model5);
        final String model6 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, false, true);
        assertEquals(expected, model6);
        final String model7 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, true, false);
        assertEquals(expected, model7);
        final String model8 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, true, true);
        assertEquals(expected, model8);
    }

    @Test
    public void testSendingInvalidInteractionModelIsNotRdf() {
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, false, false,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testNotRdfNoContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, false, false,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testNotRdfContentPresentNotExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, false, true,
                false);
        assertEquals(expected, model);
    }

    @Test
    public void testNotRdfContentPresentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, false, true, true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfNoContentNotExternal() {
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, false,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfNoContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, false, true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfHasContentNotExternal() {
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, true, false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfHasContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, true, true);
        assertEquals(expected, model);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailDblQ() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http" + "://example.org/some/location/image.tiff>; " + "rel=\"http://fedora" +
                        ".info/definitions/fcrepo#ExternalContent\"; " + "handling=\"proxy\"; type=\"image/tiff\"",
                "<http" + "://example.org/some/otherlocation>; rel=\"acl\"");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailSingleQ() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http" + "://example.org/some/location/image.tiff>; " + "rel=\"http://fedora" +
                        ".info/definitions/fcrepo#ExternalContent\"; " + "handling=\"proxy\"; type=\"image/tiff\"",
                "<http" + "://example.org/some/otherlocation>; rel='acl'");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailNoQ() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http" + "://example.org/some/location/image.tiff>; " + "rel=\"http://fedora" +
                        ".info/definitions/fcrepo#ExternalContent\"; " + "handling=\"proxy\"; type=\"image/tiff\"",
                "<http" + "://example.org/some/otherlocation>; rel=acl");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test
    public void testCheckAclLinkHeaderSuccess() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http" + "://example.org/some/location/image.tiff>; " + "rel=\"http://fedora" +
                        ".info/definitions/fcrepo#ExternalContent\"; " + "handling=\"proxy\"; type=\"image/tiff\"");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test
    public void testCopyExternalBinary() throws Exception {
        final var realDigests = asList(URI.create("urn:sha1:94e66df8cd09d410c62d9e0dc59d3a884e458e05"));

        tempFolder.create();
        final File externalFile = tempFolder.newFile();
        final String contentString = "some content";
        FileUtils.write(externalFile, contentString, StandardCharsets.UTF_8);
        final URI uri = externalFile.toURI();
        when(extContent.fetchExternalContent()).thenReturn(Files.newInputStream(externalFile.toPath()));
        when(extContent.getURI()).thenReturn(uri);
        when(extContent.isCopy()).thenReturn(true);
        when(extContent.getHandling()).thenReturn(ExternalContent.COPY);
        when(extContent.getContentType()).thenReturn("text/plain");

        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");

        createResourceService.perform(transaction, USER_PRINCIPAL, childId,
                CONTENT_TYPE, FILENAME, contentString.length(), null, realDigests, null, extContent);

        verify(psSession, times(2)).persist(operationCaptor.capture());
        final List<ResourceOperation> operations = operationCaptor.getAllValues();
        final var operation = getOperation(operations, CreateNonRdfSourceOperation.class);
        final FedoraId persistedId = operation.getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.getFullId().startsWith(fedoraId.getFullId()));
        assertBinaryPropertiesPresent(operation, "text/plain", FILENAME, contentString.length(), realDigests);
        verify(transaction).lockResource(childId);
    }

    @Test
    public void testProxyExternalBinary() throws Exception {
        final var realDigests = asList(URI.create("urn:sha1:94e66df8cd09d410c62d9e0dc59d3a884e458e05"));

        tempFolder.create();
        final File externalFile = tempFolder.newFile();
        final String contentString = "some content";
        FileUtils.write(externalFile, contentString, StandardCharsets.UTF_8);
        final URI uri = externalFile.toURI();
        when(extContent.fetchExternalContent()).thenReturn(Files.newInputStream(externalFile.toPath()));
        when(extContent.getURI()).thenReturn(uri);
        when(extContent.getHandling()).thenReturn(ExternalContent.PROXY);
        when(extContent.getContentType()).thenReturn(EXTERNAL_CONTENT_TYPE);

        final FedoraId fedoraId = FedoraId.create(UUID.randomUUID().toString());
        final FedoraId childId = fedoraId.resolve("child");

        createResourceService.perform(transaction, USER_PRINCIPAL, childId,
                CONTENT_TYPE, FILENAME, contentString.length(), null, realDigests, null, extContent);

        verify(psSession, times(2)).persist(operationCaptor.capture());
        final List<ResourceOperation> operations = operationCaptor.getAllValues();
        final var operation = getOperation(operations, CreateNonRdfSourceOperation.class);
        final FedoraId persistedId = operation.getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.getFullId().startsWith(fedoraId.getFullId()));
        assertBinaryPropertiesPresent(operation, EXTERNAL_CONTENT_TYPE, FILENAME, contentString.length(),
                realDigests);

        assertExternalBinaryPropertiesPresent(operation, uri, ExternalContent.PROXY);
        verify(transaction).lockResource(childId);
    }

    private void assertBinaryPropertiesPresent(final ResourceOperation operation, final String exMimetype,
            final String exFilename, final long exContentSize, final Collection<URI> exDigests) {
        final var nonRdfOperation = (NonRdfSourceOperation) operation;
        assertEquals(exContentSize, nonRdfOperation.getContentSize());
        assertEquals(exFilename, nonRdfOperation.getFilename());
        assertEquals(exMimetype, nonRdfOperation.getMimeType());
        assertTrue(exDigests.containsAll(nonRdfOperation.getContentDigests()));
    }

    private void assertBinaryPropertiesPresent(final ResourceOperation operation) {
        assertBinaryPropertiesPresent(operation, CONTENT_TYPE, FILENAME, CONTENT_SIZE, DIGESTS);
    }

    private void assertExternalBinaryPropertiesPresent(final ResourceOperation operation, final URI exExternalUri,
            final String exHandling) {
        final var nonRdfOperation = (NonRdfSourceOperation) operation;
        assertEquals(exExternalUri, nonRdfOperation.getContentUri());
        assertEquals(exHandling, nonRdfOperation.getExternalHandling());
    }

    private <T extends ResourceOperation> T getOperation(final List<ResourceOperation> operations,
            final Class<T> clazz) {
        return clazz.cast(operations.stream()
                .filter(o -> (clazz.isInstance(o)))
                .findFirst()
                .get());
    }

}
