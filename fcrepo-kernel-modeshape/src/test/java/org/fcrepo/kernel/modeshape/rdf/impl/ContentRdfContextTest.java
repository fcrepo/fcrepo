/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBES;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NAMESPACE;
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

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * <p>ContentRdfContextTest class.</p>
 *
 * @author ajs6f
 */
public class ContentRdfContextTest {

    @Test
    public void testForLowLevelStorageTriples() throws IOException {
        try (final ContentRdfContext contentRdfContext = new ContentRdfContext(mockResource, idTranslator)) {
            final Model results = contentRdfContext.collect(toModel());
            logRdf("Retrieved RDF for testForLowLevelStorageTriples():", results);
            assertTrue("Didn't find triple showing node has content!", results.contains(mockSubject, DESCRIBES,
                    mockContentSubject));
        }
    }

    @Test
    public void testFedoraBinaryTriples() {

        try (final ContentRdfContext contentRdfContext = new ContentRdfContext(mockBinary, idTranslator)) {
            final Model results = contentRdfContext.collect(toModel());
            assertTrue("Didn't find triple showing content has node!", results.contains(mockContentSubject,
                    DESCRIBED_BY, mockSubject));
        }
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockBinary.getNode()).thenReturn(mockBinaryNode);
        when(mockBinary.getDescription()).thenReturn(mockResource);
        when(mockBinaryNode.getSession()).thenReturn(mockSession);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn("/mockNode");
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("jcr")).thenReturn(JCR_NAMESPACE);
        when(mockResource.getDescribedResource()).thenReturn(mockBinary);
        when(mockNode.hasProperties()).thenReturn(false);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockBinaryNode.getMixinNodeTypes()).thenReturn(new NodeType[]{});
        when(mockBinaryNode.hasProperties()).thenReturn(false);
        when(mockBinary.getPath()).thenReturn("/mockNode/jcr:content");
        idTranslator = new DefaultIdentifierTranslator(mockSession);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockBinaryNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getSupertypes()).thenReturn(new NodeType[] {mockNodeType});
        when(mockNodeType.getName()).thenReturn(
                 mockNodeTypePrefix + ":" + mockNodeName);

        //when(mockNodeType.getName()).thenReturn("not:root");
        mockSubject = idTranslator.reverse().convert(mockResource);
        mockContentSubject = idTranslator.reverse().convert(mockBinary);
    }

    private Resource mockContentSubject;
    private Resource mockSubject;

    private static final String mockNodeTypePrefix = "jcr";

    private static final String mockNodeName = "mockNode";

    @Mock
    private NonRdfSourceDescriptionImpl mockResource;

    @Mock
    private Node mockNode, mockBinaryNode;

    @Mock
    private FedoraBinaryImpl mockBinary;

    @Mock
    private NodeType mockNodeType;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

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
        getLogger(ContentRdfContextTest.class);
}
