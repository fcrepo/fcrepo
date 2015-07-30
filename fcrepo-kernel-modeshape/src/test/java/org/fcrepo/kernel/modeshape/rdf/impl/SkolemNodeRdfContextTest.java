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
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.testutilities.TestPropertyIterator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.REFERENCE;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_SKOLEM;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 * @author ajs6f
 */
public class SkolemNodeRdfContextTest {

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockBlankNode;

    @Mock
    private Node mockNestedBlankNode;

    @Mock
    private Node mockOtherNode;

    @Mock
    private Property mockProperty;

    @Mock
    private Property mockReferenceProperty;

    @Mock
    private Property mockBnodeReferenceProperty;

    @Mock
    private Property mockOtherBnodeReferenceProperty;

    @Mock
    private Value mockReferenceValue;

    @Mock
    private Value mockBnodeValue;

    @Mock
    private Value mockOtherBnodeValue;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private PropertyDefinition mockPropertyDefinition;

    private RdfStream testObj;
    private DefaultIdentifierTranslator subjects;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockResource.getPath()).thenReturn("/x");

        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockBlankNode.getSession()).thenReturn(mockSession);
        when(mockOtherNode.getSession()).thenReturn(mockSession);
        when(mockNestedBlankNode.getSession()).thenReturn(mockSession);

        when(mockProperty.getType()).thenReturn(BINARY);

        when(mockBnodeReferenceProperty.getType()).thenReturn(REFERENCE);
        when(mockBnodeReferenceProperty.getName()).thenReturn("some:property");
        when(mockBnodeReferenceProperty.getValue()).thenReturn(mockBnodeValue);
        when(mockBnodeReferenceProperty.getDefinition()).thenReturn(mockPropertyDefinition);
        when(mockBnodeValue.getString()).thenReturn("xxxx");
        when(mockSession.getNodeByIdentifier("xxxx")).thenReturn(mockBlankNode);
        when(mockBlankNode.isNodeType(FEDORA_SKOLEM)).thenReturn(true);
        when(mockBlankNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockBlankNode.getPath()).thenReturn("/.well-known/gen/xxxx");
        when(mockBlankNode.getPrimaryNodeType()).thenReturn(mockNodeType);

        when(mockOtherBnodeReferenceProperty.getType()).thenReturn(REFERENCE);
        when(mockOtherBnodeReferenceProperty.getName()).thenReturn("some:property");
        when(mockOtherBnodeReferenceProperty.getValue()).thenReturn(mockOtherBnodeValue);
        when(mockOtherBnodeValue.getType()).thenReturn(REFERENCE);
        when(mockOtherBnodeReferenceProperty.getDefinition()).thenReturn(mockPropertyDefinition);
        when(mockOtherBnodeValue.getString()).thenReturn("yyyy");
        when(mockSession.getNodeByIdentifier("yyyy")).thenReturn(mockNestedBlankNode);
        when(mockNestedBlankNode.isNodeType(FEDORA_SKOLEM)).thenReturn(true);
        when(mockNestedBlankNode.getPath()).thenReturn("/.well-known/gen/yyyy");
        when(mockNestedBlankNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[]{});
        when(mockNodeType.getName()).thenReturn("some:type");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("some")).thenReturn("info:some#");

        when(mockReferenceProperty.getType()).thenReturn(REFERENCE);
        when(mockReferenceProperty.getValue()).thenReturn(mockReferenceValue);
        when(mockReferenceValue.getString()).thenReturn("zzzz");
        when(mockSession.getNodeByIdentifier("zzzz")).thenReturn(mockOtherNode);

        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testWithoutProperties() throws RepositoryException {
        when(mockNode.getProperties()).thenReturn(new TestPropertyIterator());
        testObj = new SkolemNodeRdfContext(mockResource, subjects);
        assertTrue("Expected no triples", testObj.asModel().isEmpty());
    }

    @Test
    public void testWithoutReferenceProperties() throws RepositoryException {
        when(mockNode.getProperties()).thenReturn(new TestPropertyIterator(mockProperty));
        testObj = new SkolemNodeRdfContext(mockResource, subjects);
        assertTrue("Expected no triples", testObj.asModel().isEmpty());
    }

    @Test
    public void testWithoutBlanknodeReferences() throws RepositoryException {
        when(mockNode.getProperties()).thenReturn(new TestPropertyIterator(mockReferenceProperty));
        testObj = new SkolemNodeRdfContext(mockResource, subjects);
        assertTrue("Expected no triples", testObj.asModel().isEmpty());
    }

    @Test
    public void testWithBlanknode() throws RepositoryException {
        when(mockBlankNode.getProperties()).thenReturn(new TestPropertyIterator());

        when(mockNode.getProperties()).thenAnswer(new Answer<TestPropertyIterator>() {
            @Override
            public TestPropertyIterator answer(final InvocationOnMock invocationOnMock) {
                return new TestPropertyIterator(mockBnodeReferenceProperty);
            }
        });

        testObj = new SkolemNodeRdfContext(mockResource, subjects);

        final Model actual = testObj.asModel();

        assertTrue(actual.contains(subjects.toDomain("/.well-known/gen/xxxx"),
                type,
                createResource("info:some#type")));
    }

    @Test
    public void testWithNestedBlanknodes() throws RepositoryException {



        when(mockNode.getProperties()).thenAnswer(new Answer<TestPropertyIterator>() {
            @Override
            public TestPropertyIterator answer(final InvocationOnMock invocationOnMock) {
                return new TestPropertyIterator(mockBnodeReferenceProperty);
            }
        });
        when(mockBnodeReferenceProperty.getParent()).thenReturn(mockNode);


        when(mockBlankNode.getProperties()).thenAnswer(new Answer<TestPropertyIterator>() {
            @Override
            public TestPropertyIterator answer(final InvocationOnMock invocationOnMock) {
                return new TestPropertyIterator(mockOtherBnodeReferenceProperty);
            }
        });
        when(mockOtherBnodeReferenceProperty.getParent()).thenReturn(mockBlankNode);


        when(mockNestedBlankNode.getProperties()).thenReturn(new TestPropertyIterator());
        when(mockNestedBlankNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});

        testObj = new SkolemNodeRdfContext(mockResource, subjects);

        final Model actual = testObj.asModel();

        assertTrue(actual.contains(subjects.toDomain("/.well-known/gen/xxxx"),
                type,
                createResource("info:some#type")));

        assertTrue(actual.contains(subjects.toDomain("/.well-known/gen/xxxx"),
                createProperty("some:property"),
                subjects.toDomain("/.well-known/gen/yyyy")));

        assertTrue(actual.contains(subjects.toDomain("/.well-known/gen/yyyy"),
                type,
                createResource("info:some#type")));

    }
}
