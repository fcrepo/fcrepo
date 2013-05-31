/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date May 9, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ServiceHelpers.class})
public class NodeServiceTest {

    private Session mockSession;

    private Node mockRoot;

    private NodeService testObj;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws RepositoryException {
        testObj = new NodeService();
        mockSession = mock(Session.class);
        mockRoot = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRoot);
    }

    /**
     * @todo Add Documentation.
     */
    @After
    public void tearDown() {
    }

    /**
     * @todo Add Documentation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetObjectNames() throws RepositoryException {
        final String objPath = "";
        final Session mockSession = mock(Session.class);
        final Node mockRoot = mock(Node.class);
        final Node mockObj = mock(Node.class);
        when(mockObj.getName()).thenReturn("foo");
        when(mockObj.isNodeType("nt:folder")).thenReturn(true);
        final NodeIterator mockIter = mock(NodeIterator.class);
        when(mockIter.hasNext()).thenReturn(true, false);
        when(mockIter.nextNode()).thenReturn(mockObj)
            .thenThrow(IndexOutOfBoundsException.class);
        when(mockRoot.getNodes()).thenReturn(mockIter);
        when(mockSession.getNode(objPath)).thenReturn(mockRoot);
        final Set<String> actual = testObj.getObjectNames(mockSession, "");
        verify(mockSession).getNode(objPath);
        assertEquals(1, actual.size());
        assertEquals("foo", actual.iterator().next());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testDeleteOBject() throws RepositoryException {
        final String objPath = "foo";
        final Session mockSession = mock(Session.class);
        final Node mockRootNode = mock(Node.class);
        final Node mockObjectsNode = mock(Node.class);
        mock(Property.class);
        final Node mockObjNode = mock(Node.class);
        when(mockSession.getRootNode()).thenReturn(mockRootNode);
        when(mockRootNode.getNode("objects")).thenReturn(mockObjectsNode);
        when(mockSession.getNode(objPath)).thenReturn(mockObjNode);
        PowerMockito.mockStatic(ServiceHelpers.class);
        testObj.deleteObject(mockSession, "foo");
        verify(mockSession).getNode(objPath);
        verify(mockObjNode).remove();
    }
}
