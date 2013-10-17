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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class HierarchyRdfContextTest {

    // for mocks see bottom of this class

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockGraphSubjects.getContext()).thenReturn(testPage);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(new NodeType[] {});
        when(mockNodeType.getChildNodeDefinitions()).thenReturn(
                new NodeDefinition[] {mock(NodeDefinition.class)});
        when(mockNode.getPath()).thenReturn(MOCK_NODE_PATH);
        when(mockParentNode.getPath()).thenReturn(MOCK_PARENT_NODE_PATH);
        when(mockNode.getParent()).thenReturn(mockParentNode);
        when(mockNode.getNodes()).thenReturn(mockNodes);
        when(mockParentNode.hasProperties()).thenReturn(false);
        when(mockNode.hasProperties()).thenReturn(false);
        when(mockGraphSubjects.getGraphSubject(mockNode)).thenReturn(
                testSubject);
        when(mockGraphSubjects.getGraphSubject(mockParentNode)).thenReturn(
                testParentSubject);
    }

    @Test
    public void testOneNodeAndParent() throws RepositoryException, IOException {
        when(mockNode.hasNodes()).thenReturn(false);
        final Model results =
            new HierarchyRdfContext(mockNode, mockGraphSubjects,
                    mockLowLevelStorageService).asModel();
        logRdf("Created RDF for testOneNodeAndParent()", results);

        // assertTrue("Didn't find node described!", results.list);

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

    private static final String RESOURCE_PREFIX = "http://example.com";

    private static final Resource testPage = createResource(RESOURCE_PREFIX
            + "/page");

    private static final Resource testSubject = createResource(RESOURCE_PREFIX
            + MOCK_NODE_PATH);

    private static final Resource testParentSubject =
        createResource(RESOURCE_PREFIX + MOCK_PARENT_NODE_PATH);

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode, mockParentNode, mockChildNode;

    @Mock
    private NodeIterator mockNodes;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private LowLevelStorageService mockLowLevelStorageService;

    private static final Logger LOGGER =
        getLogger(HierarchyRdfContextTest.class);

}
