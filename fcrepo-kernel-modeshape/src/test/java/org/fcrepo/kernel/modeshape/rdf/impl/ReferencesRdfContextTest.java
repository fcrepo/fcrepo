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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.testutilities.TestPropertyIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cbeer
 */
public class ReferencesRdfContextTest {

    @Mock
    private Session mockSession;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Node mockNode;

    private DefaultIdentifierTranslator translator;

    private ReferencesRdfContext testObj;
    private javax.jcr.PropertyIterator weakReferencesProperties;
    private javax.jcr.PropertyIterator strongReferencesProperties;

    @Mock
    private Property mockWeakProperty;

    @Mock
    private Property mockStrongProperty;

    @Mock
    private Node mockPropertyParent;

    @Mock
    private Value mockWeakValue;

    @Mock
    private Value mockStrongValue;

    @Mock
    private PropertyIterator mockPropertyIterator;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        translator = new DefaultIdentifierTranslator(mockSession);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockResource.getPath()).thenReturn("/a");
        when(mockNode.getPath()).thenReturn("/a");
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);

        when(mockPropertyParent.getPath()).thenReturn("/b");

        when(mockWeakProperty.getName()).thenReturn("info:weak");
        when(mockWeakProperty.getParent()).thenReturn(mockPropertyParent);
        when(mockWeakProperty.getValue()).thenReturn(mockWeakValue);
        when(mockWeakValue.getType()).thenReturn(WEAKREFERENCE);
        when(mockWeakValue.getString()).thenReturn("uuid");

        when(mockStrongProperty.getName()).thenReturn("info:strong");
        when(mockStrongProperty.getParent()).thenReturn(mockPropertyParent);
        when(mockStrongProperty.getValue()).thenReturn(mockStrongValue);
        when(mockStrongValue.getType()).thenReturn(REFERENCE);
        when(mockStrongValue.getString()).thenReturn("uuid");

        weakReferencesProperties = new TestPropertyIterator(mockWeakProperty);
        when(mockNode.getWeakReferences()).thenReturn(weakReferencesProperties);

        strongReferencesProperties = new TestPropertyIterator(mockStrongProperty);
        when(mockNode.getReferences()).thenReturn(strongReferencesProperties);

        when(mockPropertyParent.getProperties()).thenReturn(mockPropertyIterator);
        when(mockPropertyIterator.hasNext()).thenReturn(false);

        testObj = new ReferencesRdfContext(mockResource, translator);
    }

    @Test
    public void testStrongReferences() {
        final Model model = testObj.asModel();
        assertTrue(model.contains(createResource("info:fedora/b"),
                createProperty("info:strong"),
                createResource("info:fedora/a")));
    }

    @Test
    public void testWeakReferences() {

        final Model model = testObj.asModel();
        assertTrue(model.contains(createResource("info:fedora/b"),
                createProperty("info:weak"),
                createResource("info:fedora/a")));

    }


}
