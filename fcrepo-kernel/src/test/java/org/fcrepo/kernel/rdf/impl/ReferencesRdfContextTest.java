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
package org.fcrepo.kernel.rdf.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import static javax.jcr.PropertyType.REFERENCE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cbeer
 */
public class ReferencesRdfContextTest {

    public static final Resource NODE_A = ResourceFactory.createResource("info:a");
    public static final Resource NODE_B = ResourceFactory.createResource("info:b");
    @Mock
    private Property mockProperty;

    @Mock
    private Value mockValue;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockOtherNode;

    @Mock
    private Node mockAnotherNode;

    @Test
    public void testForStrongReferencesTriples() throws RepositoryException {
        when(mockNode.getReferences()).thenReturn(mockReferences);
        when(mockNode.getWeakReferences()).thenReturn(emptyPropertyIterator);
        when(mockReferences.hasNext()).thenReturn(true, true, false);
        when(mockReferences.next()).thenReturn(mockProperty);
        final ReferencesRdfContext triples = new ReferencesRdfContext(mockNode, mockGraphSubjects);
        final Model model = triples.asModel();
        assertTrue(model.contains(NODE_B, ResourceFactory.createProperty("some-property"), NODE_A));
    }

    @Test
    public void testForWeakReferencesTriples() throws RepositoryException {
        when(mockNode.getWeakReferences()).thenReturn(mockReferences);
        when(mockNode.getReferences()).thenReturn(emptyPropertyIterator);
        when(mockReferences.hasNext()).thenReturn(true, true, false);
        when(mockReferences.next()).thenReturn(mockProperty);
        final ReferencesRdfContext triples = new ReferencesRdfContext(mockNode, mockGraphSubjects);
        final Model model = triples.asModel();
        assertTrue(model.contains(NODE_B, ResourceFactory.createProperty("some-property"), NODE_A));
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockNode.getPath()).thenReturn("a");
        when(mockOtherNode.getPath()).thenReturn("b");
        when(mockProperty.getName()).thenReturn("some-property");
        when(mockProperty.getParent()).thenReturn(mockOtherNode);
        when(mockProperty.getNode()).thenReturn(mockNode);
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getType()).thenReturn(REFERENCE);
        when(mockValue.getString()).thenReturn("some-uuid");
        when(mockProperty.getSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier("some-uuid")).thenReturn(mockNode);
        when(mockGraphSubjects.getSubject("a")).thenReturn(NODE_A);
        when(mockGraphSubjects.getSubject("b")).thenReturn(NODE_B);
        when(emptyPropertyIterator.hasNext()).thenReturn(false);
    }

    @Mock
    private Node mockNode;

    @Mock
    private IdentifierTranslator mockGraphSubjects;

    @Mock
    private PropertyIterator mockReferences;

    @Mock
    private PropertyIterator emptyPropertyIterator;

}
