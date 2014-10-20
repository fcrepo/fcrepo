/**
 * Copyright 2014 DuraSpace, Inc.
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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.jcr.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.RDF_SOURCE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class LdpRdfContextTest {

    @Mock
    private FedoraResource mockResource;

    @Mock
    private FedoraObject mockContainer;
    @Mock
    private Session mockSession;

    private DefaultIdentifierTranslator subjects;

    private LdpRdfContext testObj;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockResource.getPath()).thenReturn("/a");

        when(mockContainer.getPath()).thenReturn("/a");

        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void shouldIncludeRdfTypeAssertions() throws RepositoryException {
        testObj = new LdpRdfContext(mockResource, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), RDF.type, RDF_SOURCE));
    }

    @Test
    public void shouldIncludeRdfContainerAssertions() throws RepositoryException {
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), RDF.type, RDF_SOURCE));
        assertTrue(model.contains(subject(), RDF.type, CONTAINER));
        assertTrue(model.contains(subject(), RDF.type, DIRECT_CONTAINER));
    }

    @Test
    public void shouldIncludeDefaultContainerProperties() throws RepositoryException {
        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.asModel();

        assertTrue(model.contains(subject(), HAS_MEMBER_RELATION, LDP_MEMBER));
        assertTrue(model.contains(subject(), MEMBERSHIP_RESOURCE, subject()));
    }

    @Test
    public void shouldNotIncludeDefaultContainerPropertiesWhenSet() throws RepositoryException {
        when(mockContainer.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainer.hasProperty(LDP_MEMBER_RESOURCE)).thenReturn(true);

        testObj = new LdpRdfContext(mockContainer, subjects);
        final Model model = testObj.asModel();

        assertFalse(model.contains(subject(), HAS_MEMBER_RELATION, LDP_MEMBER));
        assertFalse(model.contains(subject(), MEMBERSHIP_RESOURCE, subject()));
    }

    private Resource subject() {
        return subjects.reverse().convert(mockResource);
    }

}