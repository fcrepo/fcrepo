/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.rdf.impl;

import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.RdfLexicon.RDF_SOURCE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Session;

import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author cabeer
 */
public class LdpRdfContextTest {

    @Mock
    private FedoraResource mockResource;

    @Mock
    private FedoraBinary mockBinary;

    @Mock
    private Container mockContainer;
    @Mock
    private Session mockSession;

    private DefaultIdentifierTranslator subjects;

    private LdpRdfContext testObj;


    @Before
    public void setUp() {
        initMocks(this);
        when(mockResource.getPath()).thenReturn("/a");
        when(mockBinary.getPath()).thenReturn("/a");
        when(mockContainer.getPath()).thenReturn("/a");

        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void shouldIncludeRdfTypeAssertions() {
        testObj = new LdpRdfContext(mockResource, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), RDF.type, RDF_SOURCE));
    }

    @Test
    public void shouldIncludeBinaryTypeAssertions() {
        testObj = new LdpRdfContext(mockBinary, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), RDF.type, NON_RDF_SOURCE));
    }

    @Test
    public void shouldIncludeRdfContainerAssertions() {
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), RDF.type, RDF_SOURCE));
        assertTrue(model.contains(subject(), RDF.type, CONTAINER));
    }

    @Test
    public void shouldIncludeRdfDefaultContainerAssertion() {
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), RDF.type, BASIC_CONTAINER));
    }

    @Test
    public void shouldNotIncludeRdfDefaultContainerAssertionIfContainerSet() {
        when(mockContainer.hasType(FEDORA_CONTAINER)).thenReturn(true);
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.asModel();

        assertFalse(model.contains(subject(), RDF.type, BASIC_CONTAINER));
    }

    private Resource subject() {
        return subjects.reverse().convert(mockResource);
    }

}
