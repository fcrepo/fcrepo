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

package org.fcrepo.kernel.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ServiceHelpers.class})
public class NodeServiceTest {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot;

    @Mock
    private Node mockObjNode;

    @Mock
    private Workspace mockWorkspace;

    private NodeService testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new NodeService();
        when(mockSession.getRootNode()).thenReturn(mockRoot);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetObjectNames() throws RepositoryException {
        final String objPath = "";
        when(mockObjNode.getName()).thenReturn("foo");
        when(mockObjNode.isNodeType("nt:folder")).thenReturn(true);
        final NodeIterator mockIter = mock(NodeIterator.class);
        when(mockIter.hasNext()).thenReturn(true, false);
        when(mockIter.nextNode()).thenReturn(mockObjNode).thenThrow(
                IndexOutOfBoundsException.class);
        when(mockRoot.getNodes()).thenReturn(mockIter);
        when(mockSession.getNode(objPath)).thenReturn(mockRoot);
        final Set<String> actual = testObj.getObjectNames(mockSession, "");
        verify(mockSession).getNode(objPath);
        assertEquals(1, actual.size());
        assertEquals("foo", actual.iterator().next());
    }

    @Test
    public void testDeleteOBject() throws RepositoryException {
        final String objPath = "foo";
        final Node mockObjectsNode = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockRoot.getNode("objects")).thenReturn(mockObjectsNode);
        when(mockSession.getNode(objPath)).thenReturn(mockObjNode);
        mockStatic(ServiceHelpers.class);
        testObj.deleteObject(mockSession, "foo");
        verify(mockSession).getNode(objPath);
        verify(mockObjNode).remove();
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
        testObj.moveObject(mockSession, "foo", "bar");
        verify(mockWorkspace).move("foo", "bar");
    }
}
