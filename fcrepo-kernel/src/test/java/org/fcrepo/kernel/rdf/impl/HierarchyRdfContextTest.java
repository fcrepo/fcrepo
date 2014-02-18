/**
 * Copyright 2013 DuraSpace, Inc.
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

import static com.google.common.collect.ImmutableSet.of;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_OBJECT;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_PREDICATE;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_SUBJECT;
import static org.fcrepo.kernel.RdfLexicon.MEMBERS_INLINED;
import static org.fcrepo.kernel.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.RdfLexicon.PAGE;
import static org.fcrepo.kernel.RdfLexicon.PAGE_OF;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.testutilities.TestNodeIterator.nodeIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.testutilities.TestPropertyIterator;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.slf4j.Logger;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class HierarchyRdfContextTest {

    // for mocks and setup gear see after tests

    @Test
    public void testParentTriples() throws RepositoryException, IOException {
        nodeIsContainer();
        when(mockNode.hasNodes()).thenReturn(false);
        // not really a child of the root node, but this is just for test
        when(mockNode.getDepth()).thenReturn(1);
        final Model results = getResults();
        logRdf("Retrieved RDF for testParentTriples(): ", results);
        assertTrue("Node doesn't have a parent!", results.contains(testSubject,
                HAS_PARENT, testParentSubject));
        assertTrue("Node doesn't have a parent!", results.contains(
                testParentSubject, HAS_CHILD, testSubject));
    }

    @Test
    public void shouldIncludeChildNodeInformation() throws RepositoryException,
                                                   IOException {
        nodeIsContainer();
        when(mockNode.hasNodes()).thenReturn(true);
        when(mockNode.getDepth()).thenReturn(0);
        buildChildNodes();
        when(mockNode.getNodes()).thenReturn(
                nodeIterator(mockChildNode, mockChildNode2, mockChildNode3,
                        mockChildNode4, mockChildNode5));
        // we exhaust the original mock from setUp() so that the preceding
        // iterator is returned instead
        mockNode.getNodes();

        final Model actual = getResults();
        logRdf("Retrieved RDF for shouldIncludeChildNodeInformation() as follows: ",
                actual);
        assertEquals("Didn't find enough children!", 5, Iterators.size(actual
                .listObjectsOfProperty(HAS_CHILD)));
        assertEquals("Found too many parents!", 1, Iterators.size(actual
                .listObjectsOfProperty(HAS_PARENT)));

    }

    @Test
    public void testNodeWithContent() throws RepositoryException, IOException {
        buildContentNode();
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        nodeIsContainer();
        final Model actual = getResults();
        logRdf("Created RDF for testNodeWithContent()", actual);
        assertFalse("Should not have reported on content node!", Iterators
                .contains(actual.listSubjects(), testContentSubject.asNode()));
    }

    @Test
    public void testNotContainer() throws RepositoryException, IOException {
        nodeIsNotContainer();
        when(mockNode.hasNodes()).thenReturn(false);
        final Model actual = getResults();
        logRdf("Created RDF for testNotContainer()", actual);
        assertTrue(actual.contains(testPage, type, PAGE));
        assertFalse(actual.contains(testPage, MEMBERS_INLINED, actual
                .createTypedLiteral(true)));
        assertFalse(actual.contains(testSubject, type, CONTAINER));

        assertFalse(actual.contains(testSubject, MEMBERSHIP_SUBJECT,
                testSubject));
        assertFalse(actual.contains(testSubject, MEMBERSHIP_PREDICATE,
                HAS_CHILD));
        assertFalse(actual.contains(testSubject, MEMBERSHIP_OBJECT,
                MEMBER_SUBJECT));

    }

    @Test
    public void testForLDPTriples() throws RepositoryException, IOException {
        nodeIsContainer();
        when(mockNode.hasNodes()).thenReturn(false);
        final Model results = getResults();

        logRdf("Created RDF for testForLDPTriples()", results);

        // check for LDP-specified Page information
        assertTrue("Didn't find page described as LDP Page!", results.contains(
                testPage, type, PAGE));
        assertTrue("Didn't find page described as LDP Page of correct node!",
                results.contains(testPage, PAGE_OF, testSubject));
        assertTrue("Didn't find page described as having inlined members!",
                results.contains(testPage, MEMBERS_INLINED,
                        createPlainLiteral("true")));

        // check for LDP-specified node information
        assertTrue(
                "Didn't find node described as being the subject of membership!",
                results.contains(testSubject, MEMBERSHIP_SUBJECT, testSubject));
        assertTrue("Didn't find node described as being an LDP Container!",
                results.contains(testSubject, type, CONTAINER));
        assertTrue(
                "Didn't find node described as have an LDP membership object!",
                results.contains(testSubject, MEMBERSHIP_OBJECT, MEMBER_SUBJECT));
        assertTrue(
                "Didn't find node described as using the correct LDP membership predicate!",
                results.contains(testSubject, MEMBERSHIP_PREDICATE, HAS_CHILD));
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getName()).thenReturn("mockNode");
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockGraphSubjects.getContext()).thenReturn(testPage);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockParentNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("not:root");
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockParentNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockNode.getPath()).thenReturn(MOCK_NODE_PATH);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockParentNode.getPath()).thenReturn(MOCK_PARENT_NODE_PATH);
        when(mockNode.getParent()).thenReturn(mockParentNode);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockParentNode.hasProperties()).thenReturn(false);
        when(mockNode.hasProperties()).thenReturn(false);
        when(mockGraphSubjects.getGraphSubject(mockNode)).thenReturn(
                testSubject);
        when(mockGraphSubjects.getGraphSubject(mockParentNode)).thenReturn(
                testParentSubject);
        when(mockGraphSubjects.getGraphSubject(mockChildNode)).thenReturn(
                testChildSubject);
        when(mockNodeType.isNodeType("mode:system")).thenReturn(false);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] {mockNodeType});
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("not")).thenReturn(JCR_NAMESPACE);
        when(mockParentNode.getSession()).thenReturn(mockSession);
        
    }

    private void nodeIsContainer() {
        when(mockNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mock(NodeDefinition.class)});
    }

    private void nodeIsNotContainer() {
        when(mockNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {});
    }

    private void buildChildNodes() throws RepositoryException {

        when(mockChildNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockChildNode2.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode2.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockChildNode3.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode3.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockChildNode4.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode4.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockChildNode5.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockChildNode5.getMixinNodeTypes()).thenReturn(new NodeType[] {});

        when(mockChildNode.hasProperties()).thenReturn(false);
        when(mockChildNode2.hasProperties()).thenReturn(false);
        when(mockChildNode3.hasProperties()).thenReturn(false);
        when(mockChildNode4.hasProperties()).thenReturn(false);
        when(mockChildNode5.hasProperties()).thenReturn(false);

        when(mockChildNode.getName()).thenReturn("mockChildNode");
        when(mockChildNode2.getName()).thenReturn("mockChildNode2");
        when(mockChildNode3.getName()).thenReturn("mockChildNode3");
        when(mockChildNode4.getName()).thenReturn("mockChildNode4");
        when(mockChildNode5.getName()).thenReturn("mockChildNode5");

        when(mockChildNode.getParent()).thenReturn(mockNode);
        when(mockChildNode2.getParent()).thenReturn(mockNode);
        when(mockChildNode3.getParent()).thenReturn(mockNode);
        when(mockChildNode4.getParent()).thenReturn(mockNode);
        when(mockChildNode5.getParent()).thenReturn(mockNode);

        when(mockChildNode.getPath()).thenReturn(MOCK_CHILD_NODE_PATH);
        when(mockChildNode2.getPath()).thenReturn(MOCK_NODE_PATH + "/2");
        when(mockChildNode3.getPath()).thenReturn(MOCK_NODE_PATH + "3");
        when(mockChildNode4.getPath()).thenReturn(MOCK_NODE_PATH + "4");
        when(mockChildNode5.getPath()).thenReturn(MOCK_NODE_PATH + "5");

        when(mockGraphSubjects.getGraphSubject(mockChildNode2)).thenReturn(
                createResource(RESOURCE_PREFIX + "/2"));
        when(mockGraphSubjects.getGraphSubject(mockChildNode3)).thenReturn(
                createResource(RESOURCE_PREFIX + "/3"));
        when(mockGraphSubjects.getGraphSubject(mockChildNode4)).thenReturn(
                createResource(RESOURCE_PREFIX + "/4"));
        when(mockGraphSubjects.getGraphSubject(mockChildNode5)).thenReturn(
                createResource(RESOURCE_PREFIX + "/5"));
        
        when(mockChildNode.getSession()).thenReturn(mockSession);
        when(mockChildNode2.getSession()).thenReturn(mockSession);
        when(mockChildNode3.getSession()).thenReturn(mockSession);
        when(mockChildNode4.getSession()).thenReturn(mockSession);
        when(mockChildNode5.getSession()).thenReturn(mockSession);

    }

    private void buildContentNode() throws RepositoryException {
        when(mockContentNode.getSession()).thenReturn(mockSession);
        when(mockContentNode.getPath())
                .thenReturn(MOCK_NODE_PATH + JCR_CONTENT);
        when(mockContentNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockContentNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockBinary.getKey()).thenReturn(new BinaryKey(testBinaryKey));
        when(mockBinaryProperty.getBinary()).thenReturn(mockBinary);
        when(mockContentNode.getProperty(JCR_DATA)).thenReturn(
                mockBinaryProperty);
        when(mockBinaryProperty.getName()).thenReturn(JCR_DATA);
        when(mockBinaryProperty.getParent()).thenReturn(mockContentNode);
        when(mockCacheEntry.getExternalIdentifier()).thenReturn(
                testExternalIdentifier);
        when(
                mockLowLevelStorageService
                        .getLowLevelCacheEntries(mockContentNode)).thenReturn(
                of(mockCacheEntry));

        when(mockContentNode.getProperties()).thenReturn(
                new TestPropertyIterator(mockBinaryProperty));
    }

    private Model getResults() throws RepositoryException {
        return new HierarchyRdfContext(mockNode, mockGraphSubjects,
                mockLowLevelStorageService).asModel();
    }

    private void
            logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final String MOCK_PARENT_NODE_PATH = "/mockNodeParent";

    private static final String MOCK_NODE_PATH = MOCK_PARENT_NODE_PATH
            + "/mockNode";

    private static final String MOCK_CHILD_NODE_PATH = MOCK_NODE_PATH + "/1";

    private static final String RESOURCE_PREFIX = "http://example.com";

    private static final Resource testPage = createResource(RESOURCE_PREFIX
            + "/page");

    private static final Resource testSubject = createResource(RESOURCE_PREFIX
            + MOCK_NODE_PATH);

    private static final Resource testParentSubject =
        createResource(RESOURCE_PREFIX + MOCK_PARENT_NODE_PATH);

    private static final Resource testChildSubject =
        createResource(RESOURCE_PREFIX + MOCK_CHILD_NODE_PATH);

    private static final Resource testContentSubject =
        createResource(MOCK_NODE_PATH + JCR_CONTENT);

    private static final String testBinaryKey = "testBinaryKey";

    private static final String testExternalIdentifier =
        "testExternalIdentifier";

    @Mock
    private Session mockSession;
    
    @Mock
    private Repository mockRepository;
    
    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Node mockNode, mockParentNode, mockChildNode, mockChildNode2,
            mockChildNode3, mockChildNode4, mockChildNode5, mockContentNode;

    @Mock
    private NodeIterator mockNodes;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private LowLevelStorageService mockLowLevelStorageService;

    @Mock
    private BinaryValue mockBinary;

    @Mock
    private Property mockBinaryProperty;

    @Mock
    private LowLevelCacheEntry mockCacheEntry;

    private static final Logger LOGGER =
        getLogger(HierarchyRdfContextTest.class);

}
