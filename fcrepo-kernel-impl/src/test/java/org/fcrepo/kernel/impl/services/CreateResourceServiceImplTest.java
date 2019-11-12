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
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
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
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateResourceServiceImplTest {

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
    private RdfSourceOperationBuilder builder;

    @Mock
    private RdfSourceOperation operation;

    @Mock
    private ExternalContent extContent;

    @InjectMocks
    private CreateResourceServiceImpl createResourceService;

    private final static String txId = "tx1234";

    private final static Collection<String> nonRdfSourceTypes;

    private final static Collection<String> basicContainerTypes;

    private final Model model = ModelFactory.createDefaultModel();

    private final Collection<String> digests = Stream.of("urn:sha1:12345abced")
            .collect(Collectors.toCollection(HashSet::new));

    static {
        nonRdfSourceTypes = Collections.singleton(NON_RDF_SOURCE.toString());
        basicContainerTypes = Collections.singleton(BASIC_CONTAINER.toString());
    }

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
    public void testNoParentRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(txId, fedoraId, null, null, null, model);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsBinaryRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(nonRdfSourceTypes);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(txId, fedoraId, null, null, null, model);

    }

    @Test
    public void testSlugIsNullRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, null, null, null, model);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugExistsRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, null, model);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugDoesntExistsRdf() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, null, model);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNoParentBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(txId, fedoraId, null, null, null, null, digests,
                null, 0, null);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(nonRdfSourceTypes);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(txId, fedoraId, null, null, null, null, digests,
                null, 0, null);
    }

    @Test
    public void testSlugIsNullBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, null, null, null, null, digests,
        null, 0, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugExistsBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, null, null, digests,
            null, 0, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugDoesntExistsBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, null, null, digests,
            null, 0, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNoParentExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(txId, fedoraId, null, null, null, null, digests,
                null, 0, extContent);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(nonRdfSourceTypes);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(txId, fedoraId, null, null, null, null, digests,
                null, 0, extContent);
    }

    @Test
    public void testSlugIsNullExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, null, null, null, null, digests,
                null, 0, extContent);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugExistsExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, null, null, digests,
                null, 0, extContent);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugDoesntExistsExternal() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, null, null, digests,
                null, 0, extContent);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }
}
