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
package org.fcrepo.kernel.impl.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.JcrTools;

/**
 * <p>ObjectServiceImplTest class.</p>
 *
 * @author ksclarke
 */
public class ObjectServiceImplTest implements FedoraJcrTypes {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
    }

    @Test
    public void testCreateObject() throws Exception {
        final String testPath = "/foo";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        when(mockNodeType.getName()).thenReturn(FEDORA_OBJECT);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});

        final ObjectService testObj = new ObjectServiceImpl();
        final Node actual =
                testObj.findOrCreateObject(mockSession, testPath).getNode();
        assertEquals(mockNode, actual);
    }

    @Test
    public void testGetObject() throws RepositoryException {
        when(mockNodeType.getName()).thenReturn(FEDORA_OBJECT);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.isNew()).thenReturn(false);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getMixinNodeTypes()).thenReturn(
                new NodeType[] {mockNodeType});

        final String testPath = "/foo";
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        final ObjectService testObj = spy(new ObjectServiceImpl());
        doReturn(mockNode).when((JcrTools) testObj).findOrCreateNode(mockSession, "/foo", NT_FOLDER, NT_FOLDER);
        final FedoraObject actual = testObj.findOrCreateObject(mockSession, "/foo");
        assertEquals(mockNode, actual.getNode());
    }

}
