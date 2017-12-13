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

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

/**
 * @author cabeer
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class BinaryServiceImplTest {

    private static final String LOCAL_FILE_RESOURCE = "message/external-body; access-type=LOCAL-FILE; LOCAL-FILE=\"" +
            "file:///path/to/file\"";

    private BinaryServiceImpl testObj;

    private FedoraSession testSession;

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
        testObj = new BinaryServiceImpl();
        testSession = new FedoraSessionImpl(mockSession);
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
        testObj.findOrCreate(testSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test
    public void testFindOrCreateBinary2() throws Exception {
        when(mockDsNode.isNew()).thenReturn(true);
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        testObj.findOrCreate(testSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testFindOrCreateBinary3() throws Exception {
        when(mockDsNode.getNode(JCR_CONTENT)).thenThrow(new RepositoryException());
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockSession.getNode("/")).thenReturn(mockRoot);
        testObj.findOrCreate(testSession, testPath);
    }

    @Test
    public void testFindOrCreateLocalFileBinary() throws Exception {
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);

        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(mockNode.hasProperty(HAS_MIME_TYPE)).thenReturn(true);
        final Property mimeTypeProperty = mock(Property.class);
        when(mockNode.getProperty(HAS_MIME_TYPE)).thenReturn(mimeTypeProperty);

        when(mockSession.getNode("/")).thenReturn(mockRoot);
        testObj.findOrCreate(testSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }
}
