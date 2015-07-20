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
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDanyURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.Class;
import static com.hp.hpl.jena.vocabulary.RDFS.domain;
import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static com.hp.hpl.jena.vocabulary.RDFS.range;
import static com.hp.hpl.jena.vocabulary.RDFS.subClassOf;
import static java.util.Collections.singletonList;
import static javax.jcr.PropertyType.REFERENCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.rdf.impl.mappings.ItemDefinitionToTriples.getResource;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cbeer
 */
public class NodeTypeRdfContextTest {


    @Mock
    private NamespacedNodeType mockNodeType;

    @Mock
    private NamespacedNodeType mockNodeTypeA;

    @Mock
    private NamespacedNodeType mockNodeTypeB;

    private static final String mockNodeTypePrefix = "jcr";

    private static final String mockNodeTypeName = "someType";

    private static final String jcrNamespace = "http://www.jcp.org/jcr/1.0";

    @Mock
    private NamespacedPropertyDefinition mockProperty;

    @Mock
    private NamespacedNodeDefinition mockNodeDefinitionA;

    @Mock
    private NamespacedNodeDefinition mockNodeDefinitionB;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        initNodeTypeMocks(mockNodeType, jcrNamespace, mockNodeTypeName);
        when(mockNodeType.getName()).thenReturn(mockNodeTypePrefix + ":" + mockNodeTypeName);
        when(mockProperty.getName()).thenReturn("a:a");
        when(((Namespaced) mockProperty).getNamespaceURI()).thenReturn("a#");
        when(((Namespaced) mockProperty).getLocalName()).thenReturn("a");

        when(mockNodeTypeB.getName()).thenReturn("b:b");
        initNodeTypeMocks(mockNodeTypeB, "b#", "b");

        when(mockNodeTypeA.getName()).thenReturn("a:a");
        initNodeTypeMocks(mockNodeTypeA, "a#", "a");
        initNamespacedMocks(mockNodeDefinitionA, "a#", "a");
        when(mockNodeDefinitionA.getName()).thenReturn("a:a");

        initNamespacedMocks(mockNodeDefinitionB, "b#", "b");
        when(mockNodeDefinitionB.getName()).thenReturn("b:b");

        when(mockNodeDefinitionA.getRequiredPrimaryTypes()).thenReturn(new NodeType[]{});
        when(mockNodeDefinitionB.getRequiredPrimaryTypes()).thenReturn(new NodeType[]{});

    }

    @Test
    public void testShouldMapASimpleNodeTypeToRdf() throws RepositoryException {
        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();
        assertTrue(actual.contains(createResource(REPOSITORY_NAMESPACE
                + mockNodeTypeName), type, Class));
        assertTrue(actual.contains(createResource(REPOSITORY_NAMESPACE
                + mockNodeTypeName), label, mockNodeTypePrefix + ":"
                + mockNodeTypeName));
    }

    @Test
    public void testShouldMapNodeTypeIteratorToRdf() throws RepositoryException {
        final List<NodeType> mockNodeTypeList = singletonList((NodeType) mockNodeType);
        final Model actual = new NodeTypeRdfContext(mockNodeTypeList.iterator()).asModel();
        assertTrue(actual.contains(createResource(REPOSITORY_NAMESPACE + mockNodeTypeName),
                type, Class));
    }

    @Test
    public void testShouldMapNodeTypeManagerToRdf() throws RepositoryException {
        final NodeTypeManager mockNodeTypeManager = mock(NodeTypeManager.class);
        final NodeTypeIterator mockNodeTypeIterator = mock(NodeTypeIterator.class);
        when(mockNodeTypeIterator.next()).thenReturn(mockNodeType);
        when(mockNodeTypeIterator.hasNext()).thenReturn(true, false);
        when(mockNodeTypeManager.getPrimaryNodeTypes()).thenReturn(mockNodeTypeIterator);

        final NodeTypeIterator mockMixinTypeIterator = mock(NodeTypeIterator.class);

        when(mockMixinTypeIterator.next()).thenReturn(mockNodeTypeB);
        when(mockMixinTypeIterator.hasNext()).thenReturn(true, false);
        when(mockNodeTypeManager.getMixinNodeTypes()).thenReturn(mockMixinTypeIterator);

        final Model actual = new NodeTypeRdfContext(mockNodeTypeManager).asModel();
        assertTrue(actual.contains(
                ResourceFactory.createResource(REPOSITORY_NAMESPACE + mockNodeTypeName), type, Class));
        assertTrue(actual.contains(ResourceFactory.createResource("b#b"), type, Class));
    }

    @Test
    public void testShouldIncludeSupertypeInformation() throws RepositoryException {

        when(mockNodeType.getDeclaredSupertypes()).thenReturn(new NodeType[] { mockNodeTypeA, mockNodeTypeB });
        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();
        assertTrue(actual.contains(
                getResource((NodeType)mockNodeType), subClassOf, getResource((NodeType)mockNodeTypeA)));
        assertTrue(actual.contains(
                getResource((NodeType)mockNodeType), subClassOf, getResource((NodeType)mockNodeTypeB)));
    }

    @Test
    public void testShouldIncludeChildNodeDefinitions() throws RepositoryException {

        when(mockNodeType.getDeclaredChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mockNodeDefinitionA, mockNodeDefinitionB});

        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();
        assertTrue(actual.contains(
                getResource((NodeDefinition) mockNodeDefinitionA), domain,
                getResource((NodeType) mockNodeType)));
        assertTrue(actual.contains(
                getResource((NodeDefinition) mockNodeDefinitionB), domain,
                getResource((NodeType) mockNodeType)));

    }

    @Test
    public void testShouldIncludeChildNodeRangeWhenTheChildNodeDeclaresRequiredType() throws RepositoryException {

        when(mockNodeDefinitionA.getRequiredPrimaryTypes()).thenReturn(new NodeType[] { mockNodeTypeB });
        when(mockNodeType.getDeclaredChildNodeDefinitions()).thenReturn(new NodeDefinition[] { mockNodeDefinitionA });

        final Model actual  = new NodeTypeRdfContext(mockNodeType).asModel();

        assertTrue(actual.contains(
                getResource((NodeDefinition) mockNodeDefinitionA), domain,
                getResource((NodeType) mockNodeType)));
        assertTrue(actual.contains(
                getResource((NodeDefinition) mockNodeDefinitionA), range,
                getResource((NodeType) mockNodeTypeB)));

    }

    @Test
    public void testShouldSkipChildNodesAsResidualSet() throws RepositoryException, IOException {

        when(mockNodeDefinitionA.getName()).thenReturn("*");
        when(mockNodeType.getDeclaredChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mockNodeDefinitionA});

        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();
        logRdf("Retrieved RDF for testShouldSkipChildNodesAsResidualSet():",
                actual);
        assertFalse(actual.listResourcesWithProperty(domain,
                getResource((NodeType)mockNodeType)).hasNext());
    }

    @Test
    public void testShouldIncludePropertyDefinitions() throws RepositoryException {
        when(mockNodeType.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[] { mockProperty });

        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();

        assertTrue(actual.contains(
                getResource((PropertyDefinition) mockProperty), domain,
                getResource((NodeType) mockNodeType)));
    }


    @Test
    public void testShouldIncludePropertyDefinitionsRequiredTypeAsRange() throws RepositoryException {
        when(mockProperty.getRequiredType()).thenReturn(REFERENCE);
        when(mockNodeType.getDeclaredPropertyDefinitions()).thenReturn(
                new PropertyDefinition[] {mockProperty});

        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();

        assertTrue(actual.contains(getResource((PropertyDefinition)mockProperty), domain,
                getResource((NodeType) mockNodeType)));
        assertTrue(actual.contains(getResource((PropertyDefinition)mockProperty), range,
                createResource(XSDanyURI.getURI())));
    }

    @Test
    public void testShouldExcludePropertyDefinitionsForResidualSets() throws RepositoryException {
        when(mockProperty.getName()).thenReturn("*");
        when(mockNodeType.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[] { mockProperty });

        final Model actual = new NodeTypeRdfContext(mockNodeType).asModel();

        assertFalse(actual.listResourcesWithProperty(domain, getResource((NodeType)mockNodeType)).hasNext());
    }

    private static void initNodeTypeMocks(final NodeType mockNodeType,
                                   final String mockNamespaceUri,
                                   final String mockNodeTypeName) throws RepositoryException {

        initNamespacedMocks((Namespaced) mockNodeType, mockNamespaceUri, mockNodeTypeName);
        when(mockNodeType.getDeclaredSupertypes()).thenReturn(new NodeType[] {});
        when(mockNodeType.getDeclaredChildNodeDefinitions()).thenReturn(new NodeDefinition[] {});
        when(mockNodeType.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[] {});

    }

    private static void initNamespacedMocks(final Namespaced namedspacedObject,
                                            final String mockNamespaceUri,
                                            final String mockNodeTypeName) throws RepositoryException {
        when(namedspacedObject.getNamespaceURI()).thenReturn(mockNamespaceUri);
        when(namedspacedObject.getLocalName()).thenReturn(mockNodeTypeName);
    }

    private static interface NamespacedNodeType extends Namespaced, NodeType {
    }

    private static interface NamespacedPropertyDefinition extends Namespaced, PropertyDefinition {
    }

    private static interface NamespacedNodeDefinition extends Namespaced, NodeDefinition {
    }

    private static void logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final Logger LOGGER = getLogger(NodeTypeRdfContextTest.class);


}
