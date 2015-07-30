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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.STRING;
import static org.fcrepo.kernel.modeshape.testutilities.TestNodeIterator.nodeIterator;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 * @author ajs6f
 */
public class HashRdfContextTest {

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    private DefaultIdentifierTranslator subjects;

    @Mock
    private Node mockChildNode;

    @Mock
    private Node mockContainer;

    @Mock
    private Property mockProperty;

    @Mock
    private PropertyDefinition mockPropertyDefinition;

    @Mock
    private Value mockValue;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockResource.getPath()).thenReturn("/a");
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getNode("#")).thenReturn(mockContainer);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockChildNode.getSession()).thenReturn(mockSession);
        when(mockNodeType.getName()).thenReturn("some:type");
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[]{});

        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testHashContextWithNoHashChildren() throws RepositoryException {

        when(mockNode.hasNode("#")).thenReturn(false);

        final Model actual = new HashRdfContext(mockResource, subjects).asModel();

        assertTrue("Expected the result to be empty", actual.isEmpty());
    }

    @Test
    public void testHashContextWithHashChildren() throws RepositoryException {

        when(mockNode.hasNode("#")).thenReturn(true);
        when(mockContainer.getNodes()).thenReturn(nodeIterator(mockChildNode));

        when(mockChildNode.getPath()).thenReturn("/a/#/123");
        when(mockChildNode.hasProperties()).thenReturn(true);
        when(mockChildNode.getProperties()).thenAnswer(new Answer<PropertyIterator>() {
            @Override
            public PropertyIterator answer(final InvocationOnMock invocationOnMock) {
                return new TestPropertyIterator(mockProperty);
            }
        });
        when(mockChildNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        when(mockProperty.getParent()).thenReturn(mockChildNode);
        when(mockProperty.getType()).thenReturn(STRING);
        when(mockProperty.getDefinition()).thenReturn(mockPropertyDefinition);
        when(mockProperty.isMultiple()).thenReturn(false);
        when(mockProperty.getName()).thenReturn("info:y");
        when(mockProperty.getValue()).thenReturn(mockValue);
        when(mockValue.getString()).thenReturn("x");

        final Model actual = new HashRdfContext(mockResource, subjects).asModel();

        assertFalse("Expected the result to not be empty", actual.isEmpty());
        assertTrue("Expected to find child properties",
                actual.contains(createResource("info:fedora/a#123"),
                        createProperty("info:y"),
                        createPlainLiteral("x")));
    }
}
