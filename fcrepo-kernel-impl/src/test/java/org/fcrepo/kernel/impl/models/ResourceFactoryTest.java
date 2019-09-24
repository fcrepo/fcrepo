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
package org.fcrepo.kernel.impl.models;

import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.fcrepo.kernel.api.exception.InteractionModelViolationException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ResourceFactoryTest {

    private ResourceFactory factory;

    private final String identifier = "testString";

    private final String directInteractionModel = DIRECT_CONTAINER.toString();

    private final String nonRdfSourceModel = NON_RDF_SOURCE.toString();

    @Mock
    private FedoraResource mockResource;

    @Mock
    private PersistentStorageSession mockSession;

    @Mock
    private PersistentStorageSessionFactory mockFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockFactory.getReadOnlySession()).thenReturn(mockSession);
    }

    @Test(expected = InteractionModelViolationException.class)
    public void testBadInteraction() throws Exception {
        when(mockSession.read(identifier)).thenReturn(mockResource);
        final List<URI> types = Arrays.asList(URI.create(nonRdfSourceModel));
        when(mockResource.getTypes()).thenReturn(types);
        factory = new ResourceFactoryImpl(mockFactory);
        final Container container = factory.findOrInitContainer(identifier, directInteractionModel);
    }

}
