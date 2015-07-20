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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.FedoraJcrTypes;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.services.ContainerService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.JcrTools;

/**
 * <p>ObjectServiceImplTest class.</p>
 *
 * @author ksclarke
 */
public class ContainerServiceImplTest implements FedoraJcrTypes {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockParent;

    @Mock
    private NodeType mockNodeType;

    private ContainerService testObj;

    @Mock
    private JcrTools mockJcrTools;

    private final String testPath = "/foo";

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new ContainerServiceImpl();
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockSession.nodeExists("/")).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getParent()).thenReturn(mockRoot);
        when(mockRoot.isNew()).thenReturn(false);
    }

    @Test
    public void testCreateObject() {
        final Node actual = testObj.findOrCreate(mockSession, testPath).getNode();
        assertEquals(mockNode, actual);
    }

    @Test
    public void testCreateObjectWithHierarchy() throws Exception {
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockParent.getParent()).thenReturn(mockRoot);
        when(mockParent.isNew()).thenReturn(true);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);

        final Node actual =
                testObj.findOrCreate(mockSession, "/foo/bar").getNode();
        assertEquals(mockNode, actual);
        verify(mockParent).addMixin(FedoraJcrTypes.FEDORA_PAIRTREE);
    }

    @Test
    public void testCreateObjectWithExistingHierarchy() throws Exception {
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockParent.getParent()).thenReturn(mockRoot);
        when(mockParent.isNew()).thenReturn(false);
        when(mockRoot.hasNode("foo")).thenReturn(true);
        when(mockRoot.getNode("foo")).thenReturn(mockParent);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);

        final Node actual = testObj.findOrCreate(mockSession, "/foo/bar").getNode();
        assertEquals(mockNode, actual);
        verify(mockParent, never()).addMixin(FedoraJcrTypes.FEDORA_PAIRTREE);
    }


    @Test
    public void testGetObject() throws RepositoryException {
        final String testPath = "/foo";
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockJcrTools.findOrCreateNode(mockSession, "/foo", NT_FOLDER, NT_FOLDER)).thenReturn(mockNode);
        final Container actual = testObj.findOrCreate(mockSession, "/foo");
        assertEquals(mockNode, actual.getNode());
    }

    @Test(expected = TombstoneException.class)
    public void testThrowsTombstoneExceptionOnCreateOnTombstone() throws RepositoryException {

        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockParent.getParent()).thenReturn(mockRoot);
        when(mockParent.isNew()).thenReturn(false);
        when(mockParent.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);
        when(mockSession.nodeExists("/foo")).thenReturn(true);
        when(mockSession.getNode("/foo")).thenReturn(mockParent);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.isNew()).thenReturn(true);

        testObj.findOrCreate(mockSession, "/foo/bar");

    }


}
