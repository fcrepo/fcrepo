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
// TODO: Keep for eventually injecting implementation classes into services.
//import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;
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
import java.util.UUID;


@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateResourceServiceImplTest {

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    private RdfSourceOperationFactory rdfSourceOperationFactory;

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

    @InjectMocks
    private CreateResourceServiceImpl createResourceService;

    private final static String txId = "tx1234";

    private final static Collection<String> nonRdfSourceTypes;

    private final static Collection<String> basicContainerTypes;

    static {
        nonRdfSourceTypes = Collections.singleton(NON_RDF_SOURCE.toString());
        basicContainerTypes = Collections.singleton(BASIC_CONTAINER.toString());
    }

    @Before
    public void setUp() {
        // TODO: replace mocked RdfSourceOperationFactory with an actual implementation, once its complete.
        //rdfSourceOperationFactory = new RdfSourceOperationFactoryImpl();
        //setField(createResourceService, "rdfSourceOperationFactory", rdfSourceOperationFactory);
        when(psManager.getSession(ArgumentMatchers.any())).thenReturn(psSession);
        when(minter.get()).thenReturn(UUID.randomUUID().toString());
        when(rdfSourceOperationFactory.createBuilder(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(builder);
        when(builder.triples(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(builder);
        when(builder.build()).thenReturn(operation);
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNoParent() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenThrow(PersistentItemNotFoundException.class);
        createResourceService.perform(txId, fedoraId, null, null, false, null,
                null, null, null);
    }

    @Test(expected = CannotCreateResourceException.class)
    public void testParentIsBinary() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(resourceHeaders.getTypes()).thenReturn(nonRdfSourceTypes);
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        createResourceService.perform(txId, fedoraId, null, null, false, null,
                null, null, null);

    }

    @Test
    public void testSlugIsNull() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, null, null, false, null,
                null, null, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugExists() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, false, null,
                null, null, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testWithSlugDoesntExists() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(psSession.getHeaders(fedoraId + "/" + "testSlug", null))
                .thenThrow(PersistentItemNotFoundException.class);
        when(resourceHeaders.getTypes()).thenReturn(basicContainerTypes);
        createResourceService.perform(txId, fedoraId, "testSlug", null, false, null,
                null, null, null);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }
}
