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
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.kernel.RdfLexicon.IS_CONTENT_OF;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class PropertiesRdfContextTest {

    @Test
    public void testForLowLevelStorageTriples() throws RepositoryException,
                                               IOException {
        final Model results =
            new PropertiesRdfContext(mockNode, mockGraphSubjects,
                    mockLowLevelStorageService).asModel();
        logRdf("Retrieved RDF for testForLowLevelStorageTriples():", results);
        assertTrue("Didn't find triple showing node has content!", results
                .contains(mockSubject, HAS_CONTENT, mockContentSubject));
        assertTrue("Didn't find triple showing content has node!", results
                .contains(mockContentSubject, IS_CONTENT_OF, mockSubject));
        assertTrue("Didn't find triple showing content has location!", results
                .contains(mockContentSubject, HAS_LOCATION,
                        MOCK_EXTERNAL_IDENTIFIER));
    }

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockNode.hasNode(JCR_CONTENT)).thenReturn(true);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockNode.hasProperties()).thenReturn(false);
        when(mockContentNode.hasProperties()).thenReturn(false);
        when(
                mockLowLevelStorageService
                        .getLowLevelCacheEntries(mockContentNode)).thenReturn(
                ImmutableSet.of(mockLowLevelCacheEntry));
        when(mockLowLevelCacheEntry.getExternalIdentifier()).thenReturn(
                MOCK_EXTERNAL_IDENTIFIER);
        when(mockGraphSubjects.getGraphSubject(mockNode)).thenReturn(
                mockSubject);
        when(mockGraphSubjects.getGraphSubject(mockContentNode)).thenReturn(
                mockContentSubject);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockContentNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("not:root");
    }

    private static final String MOCK_EXTERNAL_IDENTIFIER =
        "external-identifier";

    private static final Resource mockContentSubject =
        createResource("http://example.com/node/jcr:content");;

    private static final Resource mockSubject =
        createResource("http://example.com/node");

    @Mock
    private Node mockNode, mockContentNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private LowLevelStorageService mockLowLevelStorageService;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Mock
    private LowLevelCacheEntry mockLowLevelCacheEntry;

    private void
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
