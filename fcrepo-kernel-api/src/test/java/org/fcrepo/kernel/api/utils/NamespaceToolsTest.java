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
package org.fcrepo.kernel.api.utils;

import static org.fcrepo.kernel.api.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.api.utils.NamespaceTools.validatePath;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

/**
 * <p>NamespaceToolsTest class.</p>
 *
 * @author ksclarke
 */
public class NamespaceToolsTest {

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWork;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWork);
    }

    @Test
    public void testFunction() {
        getNamespaceRegistry.apply(mockNode);
    }

    @Test (expected = NullPointerException.class)
    public void testNullNamespaceRegistry() {
        validatePath(mockSession, "irrelevant");
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testWrapsRepositoryException() {
        when(mockSession.getWorkspace()).thenThrow(new RepositoryRuntimeException(""));
        validatePath(mockSession, "irrelevant");
    }

    @Test
    public void testValidatePathWithValidNamespace() throws RepositoryException {
        when(mockWork.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        validatePath(mockSession, "easy/valid:test");
    }

    @Test (expected = FedoraInvalidNamespaceException.class)
    public void testValidatePathWithUnregisteredNamespace() throws RepositoryException {
        when(mockWork.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("invalid")).thenThrow(new NamespaceException());
        validatePath(mockSession, "easy/invalid:test");
    }

    @Test (expected = FedoraInvalidNamespaceException.class)
    public void testValidatePathWithNoNamespace() throws RepositoryException {
        when(mockWork.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        validatePath(mockSession, "easy/:test");
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testWrapsRepositoryExceptionFromNamespaceRegistry() throws RepositoryException {
        when(mockWork.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockNamespaceRegistry.getURI("broken")).thenThrow(new RepositoryException());
        validatePath(mockSession, "test/a/broken:namespace-registry");
    }

}
