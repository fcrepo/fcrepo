/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 13, 2013
 */
public class NamespaceToolsTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetNamespaceRegistry() throws RepositoryException {
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Workspace mockWork = mock(Workspace.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWork);
        NamespaceTools.getNamespaceRegistry(mockNode);
    }

    /**
     * @todo Add Documentation.
     */
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
