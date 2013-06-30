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
package org.fcrepo.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.junit.Test;

public class NamespaceToolsTest {

    @Test
    public void testGetNamespaceRegistry() throws RepositoryException {
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Workspace mockWork = mock(Workspace.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWork);
        NamespaceTools.getNamespaceRegistry(mockNode);
    }

    @Test
    public void testFunction() throws RepositoryException {
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Workspace mockWork = mock(Workspace.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWork);
        NamespaceTools.getNamespaceRegistry.apply(mockNode);
    }
}
