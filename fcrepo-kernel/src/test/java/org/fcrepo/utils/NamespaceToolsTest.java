package org.fcrepo.utils;

import static org.mockito.Mockito.*;

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
