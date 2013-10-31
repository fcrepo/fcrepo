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

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class NodeRdfContextTest {

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Mock
    private LowLevelStorageService mockLowLevelStorageService;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Workspace mockWorkspace;

    private static final String mockNodeName = "mockNode";

    private static final String mockNodeTypePrefix = "jcr";

    private static final String mockNodeTypeName = "someType";

    private static final Resource mockNodeSubject = createResource();

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn(
                mockNodeTypePrefix + ":" + mockNodeTypeName);
        when(mockNode.getName()).thenReturn(mockNodeName);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn(JCR_NAMESPACE);
        when(mockGraphSubjects.getGraphSubject(mockNode)).thenReturn(
                mockNodeSubject);
    }

    @Test
    public void testIncludesMixinTriples() throws RepositoryException,
        IOException {
        final Model actual =
            new NodeRdfContext(mockNode, mockGraphSubjects, mockLowLevelStorageService).asModel();
        final Resource expectedRdfType =
            createResource(REPOSITORY_NAMESPACE + mockNodeTypeName);
        logRdf("Constructed RDF: ", actual);
        assertTrue("Didn't find RDF type triple for mixin!", actual.contains(
                mockNodeSubject, type, expectedRdfType));
    }

    @Test(expected = RuntimeException.class)
    public void testBadRepository() throws RepositoryException {
        when(mockNamespaceRegistry.getURI("jcr")).thenThrow(
                new RepositoryException("Expected."));
        new NodeRdfContext(mockNode, mockGraphSubjects,
                    mockLowLevelStorageService).asModel();

    }

    private void logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final Logger LOGGER = getLogger(NodeRdfContextTest.class);

}
