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

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_BINARY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

/**
 * @author cabeer
 * @author ajs6f
 */
public class BinaryServiceImplTest {

    private BinaryServiceImpl testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockDsNode;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockRoot;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new BinaryServiceImpl();
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockNode);
        when(mockDsNode.getParent()).thenReturn(mockRoot);
        when(mockRoot.isNew()).thenReturn(false);
    }

    @Test
    public void testFindOrCreateBinary() throws Exception {
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        testObj.findOrCreate(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test
    public void testFindOrCreateBinary2() throws Exception {
        when(mockDsNode.isNew()).thenReturn(true);
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        testObj.findOrCreate(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testFindOrCreateBinary3() throws Exception {
        when(mockDsNode.getNode(JCR_CONTENT)).thenThrow(new RepositoryException());
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        testObj.findOrCreate(mockSession, testPath);
    }

    @Test
    public void testAsBinary() throws Exception {
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        testObj.cast(mockNode);
    }

    @Test(expected = ResourceTypeException.class)
    public void testAsBinaryWithNonbinary() throws Exception {
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(false);
        testObj.cast(mockNode);
    }

}
