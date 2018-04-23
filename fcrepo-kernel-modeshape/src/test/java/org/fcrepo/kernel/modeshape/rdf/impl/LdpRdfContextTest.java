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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.ContainerImpl;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

/**
 * @author cabeer
 */
public class LdpRdfContextTest {

    @Mock
    private FedoraResourceImpl mockResource;

    @Mock
    private NonRdfSourceDescriptionImpl mockDescription;

    @Mock
    private ContainerImpl mockContainer;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    private DefaultIdentifierTranslator subjects;

    private LdpRdfContext testObj;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockResource.getPath()).thenReturn("/a");
        when(mockDescription.getPath()).thenReturn("/a/fcr:metadata");
        when(mockContainer.getPath()).thenReturn("/a");
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockDescription.getNode()).thenReturn(mockNode);
        when(mockContainer.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);

        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void shouldIncludeRdfTypeAssertions() {
        testObj = new LdpRdfContext(mockResource, subjects);
        final Model model = testObj.collect(toModel());

        assertTrue(model.contains(subject(), RDF.type, RDF_SOURCE));
    }

    @Test
    public void shouldIncludeDescriptionTypeAssertions() {
        testObj = new LdpRdfContext(mockDescription, subjects);
        final Model model = testObj.collect(toModel());

        assertTrue(model.contains(subject(mockDescription), RDF.type, NON_RDF_SOURCE));
    }

    @Test
    public void shouldIncludeRdfContainerAssertions() {
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.collect(toModel());

        assertTrue(model.contains(subject(), RDF.type, RDF_SOURCE));
        assertTrue(model.contains(subject(), RDF.type, CONTAINER));
    }

    @Test
    public void shouldIncludeRdfDefaultContainerAssertion() {
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.collect(toModel());

        assertTrue(model.contains(subject(), RDF.type, BASIC_CONTAINER));
    }

    @Test
    public void shouldNotIncludeRdfDefaultContainerAssertionIfContainerSet() {
        when(mockContainer.hasType(FEDORA_CONTAINER)).thenReturn(true);
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.collect(toModel());

        assertFalse(model.contains(subject(), RDF.type, BASIC_CONTAINER));
    }

    private Resource subject() {
        return subject(mockResource);
    }

    private Resource subject(final FedoraResource resource) {
        return subjects.reverse().convert(resource);
    }

}
