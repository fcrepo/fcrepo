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

import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.IS_CONTENT_OF;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
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

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>PropertiesRdfContextTest class.</p>
 *
 * @author ajs6f
 */
public class PropertiesRdfContextTest {

    @Test
    public void testForLowLevelStorageTriples() throws RepositoryException,
                                               IOException {
        final Model results =
            new PropertiesRdfContext(mockResource, mockGraphSubjects).asModel();
        logRdf("Retrieved RDF for testForLowLevelStorageTriples():", results);
        assertTrue("Didn't find triple showing node has content!", results
                .contains(mockSubject, HAS_CONTENT, mockContentSubject));
        assertTrue("Didn't find triple showing content has node!", results
                .contains(mockContentSubject, IS_CONTENT_OF, mockSubject));
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockBinary.getNode()).thenReturn(mockBinaryNode);
        when(mockBinaryNode.getSession()).thenReturn(mockSession);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn("/mockNode");
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn(JCR_NAMESPACE);
        when(mockResource.getBinary()).thenReturn(mockBinary);
        when(mockNode.hasProperties()).thenReturn(false);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockBinaryNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockBinaryNode.hasProperties()).thenReturn(false);
        when(mockBinary.getPath()).thenReturn("/mockNode/jcr:content");
        mockGraphSubjects = new DefaultIdentifierTranslator(mockSession);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockBinaryNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] {mockNodeType});
        when(mockNodeType.getName()).thenReturn(
                 mockNodeTypePrefix + ":" + mockNodeName);

        //when(mockNodeType.getName()).thenReturn("not:root");
        mockSubject = mockGraphSubjects.reverse().convert(mockResource);
        mockContentSubject = mockGraphSubjects.reverse().convert(mockBinary);
    }

    private Resource mockContentSubject;
    private Resource mockSubject;

    private static final String mockNodeTypePrefix = "jcr";

    private static final String mockNodeName = "mockNode";

    @Mock
    private Datastream mockResource;

    @Mock
    private Node mockNode;

    @Mock
    private FedoraBinary mockBinary;

    @Mock
    private Node mockBinaryNode;


    @Mock
    private NodeType mockNodeType;

    private IdentifierConverter<Resource, FedoraResource> mockGraphSubjects;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    private static void
            logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final Logger LOGGER =
        getLogger(PropertiesRdfContextTest.class);
}
