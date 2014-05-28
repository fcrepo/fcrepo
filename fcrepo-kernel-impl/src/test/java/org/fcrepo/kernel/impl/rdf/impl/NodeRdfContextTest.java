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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>NodeRdfContextTest class.</p>
 *
 * @author ajs6f
 */
public class NodeRdfContextTest {

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockPrimaryNodeType;

    @Mock
    private NodeType mockMixinNodeType;

    @Mock
    private NodeType mockPrimarySuperNodeType;

    @Mock
    private NodeType mockMixinSuperNodeType;

    @Mock
    private IdentifierTranslator mockGraphSubjects;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Workspace mockWorkspace;

    private static final String mockNodeName = "mockNode";

    private static final String mockNodeTypePrefix = "jcr";

    private static final String mockPrimaryNodeTypeName = "somePrimaryType";
    private static final String mockMixinNodeTypeName = "someMixinType";
    private static final String mockPrimarySuperNodeTypeName = "somePrimarySuperType";
    private static final String mockMixinSuperNodeTypeName = "someMixinSuperType";

    private static final Resource mockNodeSubject = createResource();

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockPrimaryNodeType);
        when(mockPrimaryNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockPrimaryNodeTypeName);

        when(mockNode.getName()).thenReturn(mockNodeName);

        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockMixinNodeType});
        when(mockMixinNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockMixinNodeTypeName);

        when(mockPrimaryNodeType.getSupertypes()).thenReturn(
                new NodeType[] {mockPrimarySuperNodeType});
        when(mockPrimarySuperNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockPrimarySuperNodeTypeName);

        when(mockMixinNodeType.getSupertypes()).thenReturn(
                new NodeType[] {mockMixinSuperNodeType});
        when(mockMixinSuperNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockMixinSuperNodeTypeName);

        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn(JCR_NAMESPACE);
        when(mockGraphSubjects.getSubject(mockNode.getPath())).thenReturn(mockNodeSubject);
    }

    @Test
    public void testRdfTypesForNodetypes() throws RepositoryException,
        IOException {
        final Model actual =
            new NodeRdfContext(mockNode, mockGraphSubjects).asModel();
        final Resource expectedRdfTypePrimary =
            createResource(REPOSITORY_NAMESPACE + mockPrimaryNodeTypeName);
        final Resource expectedRdfTypeMixin =
                createResource(REPOSITORY_NAMESPACE + mockMixinNodeTypeName);
        final Resource expectedRdfTypePrimarySuper =
                createResource(REPOSITORY_NAMESPACE + mockPrimarySuperNodeTypeName);
        final Resource expectedRdfTypeMixinSuper =
                createResource(REPOSITORY_NAMESPACE + mockMixinSuperNodeTypeName);
        logRdf("Constructed RDF: ", actual);
        assertTrue("Didn't find RDF type triple for primarytype!", actual.contains(
                mockNodeSubject, type, expectedRdfTypePrimary));
        assertTrue("Didn't find RDF type triple for mixintype!", actual.contains(
                mockNodeSubject, type, expectedRdfTypeMixin));
        assertTrue("Didn't find RDF type triple for primarysupertype!", actual.contains(
                mockNodeSubject, type, expectedRdfTypePrimarySuper));
        assertTrue("Didn't find RDF type triple for mixinsupertype!", actual.contains(
                mockNodeSubject, type, expectedRdfTypeMixinSuper));
    }

    @Test(expected = RuntimeException.class)
    public void testBadRepository() throws RepositoryException {
        when(mockNamespaceRegistry.getURI("jcr")).thenThrow(
                new RepositoryException("Expected."));
        new NodeRdfContext(mockNode, mockGraphSubjects).asModel();

    }

    private static void logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final Logger LOGGER = getLogger(NodeRdfContextTest.class);

}
