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

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.impl.operations.NonRdfSourceOperationFactoryImpl;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RunWith(MockitoJUnitRunner.Silent.class)
public class UpdateResourceServiceImplTest {

    @Mock
    private PersistentStorageSessionManager psManager;

    private RdfSourceOperationFactory rdfSourceOperationFactory;

    private NonRdfSourceOperationFactory nonRdfSourceOperationFactory;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceHeaders resourceHeaders;

    @Mock
    private NonRdfSourceOperationBuilder builder;

    @Mock
    private NonRdfSourceOperation operation;

    @Mock
    private ExternalContent extContent;

    @InjectMocks
    private UpdateResourceServiceImpl updateResourceService;

    @Mock
    private InputStream inputStream;

    private final static String txId = "tx1234";

    private final static Collection<String> nonRdfSourceTypes;

    private final Collection<String> digests = Stream.of("urn:sha1:12345abced")
            .collect(Collectors.toCollection(HashSet::new));

    static {
        nonRdfSourceTypes = Collections.singleton(NON_RDF_SOURCE.toString());
    }

    @Before
    public void setUp() {
        rdfSourceOperationFactory = Mockito.spy(new RdfSourceOperationFactoryImpl());
        setField(updateResourceService, "rdfSourceOperationFactory", rdfSourceOperationFactory);
        nonRdfSourceOperationFactory =  Mockito.spy(new NonRdfSourceOperationFactoryImpl());
        setField(updateResourceService, "nonRdfSourceOperationFactory", nonRdfSourceOperationFactory);
        when(psManager.getSession(ArgumentMatchers.any())).thenReturn(psSession);
    }

    @Test
    public void testUpdateNonRdfSource() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        updateResourceService.perform(txId, fedoraId, "filename", "text/plain", digests, inputStream, 0, null);
        verify(nonRdfSourceOperationFactory).updateInternalBinaryBuilder(fedoraId, inputStream);
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

    @Test
    public void testUpdateNonRdfSourceExtContent() throws Exception {
        final String fedoraId = UUID.randomUUID().toString();
        final String extUrl = "http://www.example.com/";
        when(psSession.getHeaders(fedoraId, null)).thenReturn(resourceHeaders);
        when(resourceHeaders.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(extContent.getURL()).thenReturn(extUrl);
        when(extContent.getHandling()).thenReturn(ExternalContent.REDIRECT);
        updateResourceService.perform(txId, fedoraId, null, null, digests, null, 0, extContent);
        verify(nonRdfSourceOperationFactory).updateExternalBinaryBuilder(fedoraId, ExternalContent.REDIRECT, URI.create(extUrl));
        verify(psSession).persist(ArgumentMatchers.any(ResourceOperation.class));
    }

}
