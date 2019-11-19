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

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.impl.services.functions.FedoraUtils.addToIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.XSD;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;
import org.fcrepo.kernel.impl.operations.NonRdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateResourceServiceImplTest {

    private static final List<String> STRING_TYPES_NOT_VALID = Arrays.asList(MEMENTO_TYPE, RESOURCE.toString(),
            FEDORA_RESOURCE.toString());

    private static final List<String> STRING_TYPES_VALID = Arrays.asList(MEMENTO_TYPE, CONTAINER.toString(),
            BASIC_CONTAINER.toString());

    private final String defaultInteractionModel = DEFAULT_INTERACTION_MODEL.toString();

    @Mock
    private PersistentStorageSessionManager psManager;

    private RdfSourceOperationFactory rdfSourceOperationFactory;

    private NonRdfSourceOperationFactory nonRdfSourceOperationFactory;

    @Mock
    private UniqueValueSupplier minter;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceHeaders resourceHeaders;

    @Mock
    private ExternalContent extContent;

    @Captor
    private ArgumentCaptor<ResourceOperation> operationCaptor;

    @InjectMocks
    private CreateResourceServiceImpl createResourceService;

    private static final String TX_ID = "tx1234";

    private static final Collection<String> NON_RDF_SOURCE_TYPES = Collections.singleton(NON_RDF_SOURCE.toString());

    private static final Collection<String> BASIC_CONTAINER_TYPES = Collections.singleton(BASIC_CONTAINER.toString());

    private final Model model = ModelFactory.createDefaultModel();

    private final Collection<String> digests = Collections.singleton("urn:sha1:12345abced");

    @Before
    public void setUp() {
        rdfSourceOperationFactory = new RdfSourceOperationFactoryImpl();
        setField(createResourceService, "rdfSourceOperationFactory", rdfSourceOperationFactory);
        nonRdfSourceOperationFactory = new NonRdfSourceOperationFactoryImpl();
        setField(createResourceService, "nonRdfSourceOperationFactory", nonRdfSourceOperationFactory);
        when(psManager.getSession(ArgumentMatchers.any())).thenReturn(psSession);
        when(minter.get()).thenReturn(UUID.randomUUID().toString());
        when(extContent.getURL()).thenReturn("http://example.org/rest/object");
        when(extContent.getHandling()).thenReturn(ExternalContent.PROXY);
        when(extContent.getContentType()).thenReturn("text/plain");
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNoParentRdf_Post() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, model);
    }

    @Test
    public void testNoParentRdf_Put() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(TX_ID, fedoraId, null, false, null, model);
        verify(psSession).persist(operationCaptor.capture());
        assertEquals(fedoraId, operationCaptor.getValue().getResourceId());
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNoParentBinary_Post() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, null, digests,
                null, null);
    }

    @Test
    public void testNoParentBinary_Put() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(TX_ID, fedoraId, null, false, null, null, digests,
                null, null);
        verify(psSession).persist(operationCaptor.capture());
        assertEquals(fedoraId, operationCaptor.getValue().getResourceId());
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsBinaryRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(NON_RDF_SOURCE_TYPES);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, model);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsBinaryBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(NON_RDF_SOURCE_TYPES);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, null, digests,
                null, null);
    }

    @Test
    public void testSlugIsNullRdf_Post() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, model);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.startsWith(fedoraId));
    }

    @Test
    public void testSlugIsNullRdf_Put() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, null, false, null, model);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertEquals(fedoraId, persistedId);
    }

    @Test
    public void testSlugIsNullBinary_Post() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, null, digests,
                null, null);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertTrue(persistedId.startsWith(fedoraId));
    }

    @Test
    public void testSlugIsNullBinary_Put() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, null, false, null, null, digests,
                null, null);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertEquals(fedoraId, persistedId);
    }

    @Test
    public void testWithSlugExistsRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        final String childId = addToIdentifier(fedoraId, "testSlug");
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, "testSlug", true, null, model);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertNotEquals(childId, persistedId);
        assertTrue(persistedId.startsWith(fedoraId));
    }

    @Test
    public void testWithSlugExistsBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        final String childId = addToIdentifier(fedoraId, "testSlug");
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, "testSlug", true, null, null, digests,
                null, null);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertNotEquals(childId, persistedId);
        assertTrue(persistedId.startsWith(fedoraId));
    }

    @Test
    public void testWithSlugDoesntExistsRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        final String childId = addToIdentifier(fedoraId, "testSlug");
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, "testSlug", true, null, model);
        verify(psSession).persist(operationCaptor.capture());
        assertEquals(childId, operationCaptor.getValue().getResourceId());
    }

    @Test
    public void testWithSlugDoesntExistsBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(addToIdentifier(fedoraId, "testSlug"), null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, "testSlug", true, null, null, digests,
                null, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNoParentExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, null, digests,
                null, extContent);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(NON_RDF_SOURCE_TYPES);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, null, digests,
                null, extContent);
    }

    @Test
    public void testSlugIsNullExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, null, true, null, null, digests,
                null, extContent);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugExistsExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        final String childId = addToIdentifier(fedoraId, "testSlug");
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, "testSlug", true, null, null, digests,
                null, extContent);
        verify(psSession).persist(operationCaptor.capture());
        final String persistedId = operationCaptor.getValue().getResourceId();
        assertNotEquals(fedoraId, persistedId);
        assertNotEquals(childId, persistedId);
        assertTrue(persistedId.startsWith(fedoraId));
    }

    @Test
    public void testWithSlugDoesntExistsExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        final String childId = addToIdentifier(fedoraId, "testSlug");
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(childId, null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(BASIC_CONTAINER_TYPES);
        createResourceService.perform(TX_ID, fedoraId, "testSlug", true, null, null, digests,
                null, extContent);
        verify(psSession).persist(operationCaptor.capture());
        assertEquals(childId, operationCaptor.getValue().getResourceId());
    }

    @Test
    public void testSendingValidInteractionModel() {
        // If you provide a valid interaction model, you should always get it back.
        final String expected = BASIC_CONTAINER.toString();
        final String model1 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, false,
                false);
        assertEquals(expected, model1);
        final String model2 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, false,
                true);
        assertEquals(expected, model2);
        final String model3 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, true,
                false);
        assertEquals(expected, model3);
        final String model4 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, false, true,
                true);
        assertEquals(expected, model4);
        final String model5 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, false,
                false);
        assertEquals(expected, model5);
        final String model6 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, false,
                true);
        assertEquals(expected, model6);
        final String model7 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, true,
                false);
        assertEquals(expected, model7);
        final String model8 = createResourceService.determineInteractionModel(STRING_TYPES_VALID, true, true,
                true);
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
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, false, true,
                true);
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
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, false,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfHasContentNotExternal() {
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, true,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfHasContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = createResourceService.determineInteractionModel(STRING_TYPES_NOT_VALID, true, true,
                true);
        assertEquals(expected, model);
    }

    @Test(expected = MalformedRdfException.class)
    public void testCheckServerManagedLdpType() throws Exception {
        final InputStream graph = IOUtils.toInputStream("@prefix ldp: <" + LDP_NAMESPACE + "> .\n@prefix dc: <" + DC_11.getURI() + "> .\n" +
                "@prefix example: <http://example.org/stuff#> .\n" +
                "<> a example:Thing, ldp:BasicContainer ; dc:title \"The thing\" .", "UTF-8");
        final Model model = ModelFactory.createDefaultModel();
        model.read(graph, "http://localhost:8080/rest/test1", "TURTLE");
        createResourceService.checkForSmtsLdpTypes(model);
    }

    @Test(expected = MalformedRdfException.class)
    public void testCheckServerManagedPredicate() throws Exception {
        final InputStream graph = IOUtils.toInputStream("@prefix fr: <" + REPOSITORY_NAMESPACE + "> .\n@prefix dc: <" + DC_11.getURI() + "> .\n" +
                "@prefix example: <http://example.org/stuff#> .\n@prefix xsd: <" + XSD.getURI() + ">.\n" +
                "<> a example:Thing; dc:title \"The thing\"; fr:lastModified \"2000-01-01T00:00:00Z\"^^xsd:datetime .", "UTF-8");
        final Model model = ModelFactory.createDefaultModel();
        model.read(graph, "http://localhost:8080/rest/test1", "TURTLE");
        createResourceService.checkForSmtsLdpTypes(model);
    }

    @Test
    public void testCheckServerManagedSuccess() throws Exception {
        final InputStream graph = IOUtils.toInputStream("@prefix dc: <" + DC_11.getURI() + "> .\n" +
                "@prefix example: <http://example.org/stuff#> .\n@prefix xsd: <" + XSD.getURI() + ">.\n" +
                "<> a example:Thing; dc:title \"The thing\"; example:lastModified \"2000-01-01T00:00:00Z\"^^xsd:datetime .", "UTF-8");
        final Model model = ModelFactory.createDefaultModel();
        model.read(graph, "http://localhost:8080/rest/test1", "TURTLE");
        createResourceService.checkForSmtsLdpTypes(model);
    }

    @Test(expected = ServerManagedTypeException.class)
    public void testHasRestrictedPathFail() throws Exception {
        final String path = UUID.randomUUID().toString() + "/fedora:stuff/" + UUID.randomUUID().toString();
        createResourceService.hasRestrictedPath(path);
    }

    @Test
    public void testHasRestrictedPathPass() throws Exception {
        final String path = UUID.randomUUID().toString() + "/dora:stuff/" + UUID.randomUUID().toString();
        createResourceService.hasRestrictedPath(path);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailDblQ() throws Exception {
        final List<String> links =Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                        "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                        "handling=\"proxy\"; type=\"image/tiff\"", "<http://example.org/some/otherlocation>; rel=\"acl\"");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailSingleQ() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                        "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                        "handling=\"proxy\"; type=\"image/tiff\"", "<http://example.org/some/otherlocation>; rel='acl'");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailNoQ() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                        "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                        "handling=\"proxy\"; type=\"image/tiff\"", "<http://example.org/some/otherlocation>; rel=acl");
        createResourceService.checkAclLinkHeader(links);
    }

    @Test
    public void testCheckAclLinkHeaderSuccess() throws Exception {
        final List<String> links = Arrays.asList("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                        "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                        "handling=\"proxy\"; type=\"image/tiff\"");
        createResourceService.checkAclLinkHeader(links);
    }
}
