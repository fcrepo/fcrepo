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
package org.fcrepo.kernel.modeshape.services;

import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.api.JcrTools;

/**
 * <p>ObjectServiceImplTest class.</p>
 *
 * @author ksclarke
 */
@RunWith(MockitoJUnitRunner.class)
public class ContainerServiceImplTest implements FedoraTypes {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRoot, mockNode, mockParent;

    @Mock
    private NodeType mockNodeType;

    private ContainerService testObj;

    @Mock
    private JcrTools mockJcrTools;

    private final String testPath = "/foo";

    private FedoraSession testSession;

    @Before
    public void setUp() throws RepositoryException {
        testObj = new ContainerServiceImpl();
        testSession = new FedoraSessionImpl(mockSession);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockSession.nodeExists("/")).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getParent()).thenReturn(mockRoot);
        when(mockRoot.isNew()).thenReturn(false);
    }

    @Test
    public void testCreateObject() {
        final Node actual = getJcrNode(testObj.findOrCreate(testSession, testPath));
        assertEquals(mockNode, actual);
    }

    @Test
    public void testCreateObjectWithHierarchy() throws Exception {
        when(mockNode.getParent()).thenReturn(mockParent);
        when(mockParent.getParent()).thenReturn(mockRoot);
        when(mockParent.isNew()).thenReturn(true);
        when(mockRoot.getNode("foo/bar")).thenReturn(mockNode);
        when(mockNode.getDepth()).thenReturn(1);
        when(mockNode.isNew()).thenReturn(true);

        final Node actual = getJcrNode(testObj.findOrCreate(testSession, "/foo/bar"));
        assertEquals(mockNode, actual);
        verify(mockParent).addMixin(FedoraTypes.FEDORA_PAIRTREE);
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
        when(mockNode.getDepth()).thenReturn(1);

        final Node actual = getJcrNode(testObj.findOrCreate(testSession, "/foo/bar"));
        assertEquals(mockNode, actual);
        verify(mockParent, never()).addMixin(FedoraTypes.FEDORA_PAIRTREE);
    }


    @Test
    public void testGetObject() throws RepositoryException {
        final String testPath = "/foo";
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockJcrTools.findOrCreateNode(mockSession, "/foo", NT_FOLDER, NT_FOLDER)).thenReturn(mockNode);
        final Container actual = testObj.findOrCreate(testSession, "/foo");
        assertEquals(mockNode, getJcrNode(actual));
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

        testObj.findOrCreate(testSession, "/foo/bar");

    }


}
