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
package org.fcrepo.kernel.modeshape.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeIterator;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.api.services.NodeService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * <p>NodeServiceImplTest class.</p>
 *
 * @author ksclarke
 */
public class NodeServiceImplTest {

    @Mock
    private NodeTypeIterator mockNTI;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot;

    @Mock
    private Node mockObjNode;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private PropertyIterator mockEmptyIterator;

    private NodeService testObj;

    @Mock
    private NamespaceRegistry mockNameReg;

    final private static String MOCK_PREFIX = "valid_ns";

    final private static String MOCK_URI = "http://example.org";

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new NodeServiceImpl();
        when(mockSession.getRootNode()).thenReturn(mockRoot);

        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.getAllNodeTypes()).thenReturn(mockNTI);
        when(mockEmptyIterator.hasNext()).thenReturn(false);
        final String[] mockPrefixes = { MOCK_PREFIX };
        mockNameReg = mock(NamespaceRegistry.class);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNameReg);
        when(mockNameReg.getPrefixes()).thenReturn(mockPrefixes);
        when(mockNameReg.getURI(MOCK_PREFIX)).thenReturn(MOCK_URI);

    }

    @Test
    public void testCopyObject() throws RepositoryException {
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        testObj.copyObject(mockSession, "foo", "bar");
        verify(mockWorkspace).copy("foo", "bar");
    }

    @Test
    public void testMoveObject() throws RepositoryException {
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockSession.getNode("foo")).thenReturn(mockObjNode);
        when(mockObjNode.getDepth()).thenReturn(0);
        testObj.moveObject(mockSession, "foo", "bar");
        verify(mockWorkspace).move("foo", "bar");
    }

    @Test
    public void testExists() throws RepositoryException {
        final String existsPath = "/foo/bar/exists";
        when(mockSession.nodeExists(existsPath)).thenReturn(true);
        assertEquals(true, testObj.exists(mockSession, existsPath));
        assertEquals(false, testObj.exists(mockSession, "/foo/bar"));
    }

    @Test(expected = FedoraInvalidNamespaceException.class)
    public void testInvalidPath() throws RepositoryException {
        final String badPath = "/foo/bad_ns:bar";
        when(mockNameReg.getURI("bad_ns")).thenThrow(new FedoraInvalidNamespaceException("Invalid namespace (bad_ns)"));
        testObj.exists(mockSession, badPath);
    }

    @Test
    public void testGetNodeTypes() throws Exception {
        when(mockNodeTypeManager.getPrimaryNodeTypes()).thenReturn(mock(NodeTypeIterator.class));
        when(mockNodeTypeManager.getMixinNodeTypes()).thenReturn(mock(NodeTypeIterator.class));
        testObj.getNodeTypes(mockSession);

        verify(mockNodeTypeManager).getPrimaryNodeTypes();
        verify(mockNodeTypeManager).getMixinNodeTypes();
    }

    @Test
    public void testRegisterNodeTypes() throws Exception {
        try (final InputStream mockInputStream = mock(InputStream.class)) {
            testObj.registerNodeTypes(mockSession, mockInputStream);

            verify(mockNodeTypeManager).registerNodeTypes(mockInputStream, true);
        }
    }
}
