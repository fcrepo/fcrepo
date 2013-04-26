
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ObjectService.class, ServiceHelpers.class})
public class ObjectServiceTest implements FedoraJcrTypes {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCreateObjectNode() throws Exception {
        final Node mockNode = mock(Node.class);
        final Session mockSession = mock(Session.class);
        final String testPath = "/foo";
        final FedoraObject mockWrapper = new FedoraObject(mockNode);
        whenNew(FedoraObject.class).withArguments(mockSession, testPath)
                .thenReturn(mockWrapper);
        final ObjectService testObj = new ObjectService();
        final Node actual = testObj.createObject(mockSession, "/foo").getNode();
        assertEquals(mockNode, actual);
        verifyNew(FedoraObject.class).withArguments(mockSession, testPath);
    }

    @Test
    public void testCreateObject() throws Exception {
        final Node mockNode = mock(Node.class);
        final Session mockSession = mock(Session.class);
        final String testPath = "/foo";
        final FedoraObject mockWrapper = new FedoraObject(mockNode);
        whenNew(FedoraObject.class).withArguments(any(Session.class),
                any(String.class)).thenReturn(mockWrapper);
        final ObjectService testObj = new ObjectService();
        testObj.createObject(mockSession, "/foo");
        verifyNew(FedoraObject.class).withArguments(mockSession, testPath);
    }

    @Test
    public void testGetObject() throws RepositoryException {
        final Session mockSession = mock(Session.class);
        final Session mockROSession = mock(Session.class);
        final Node mockNode = mock(Node.class);
		final String testPath = "/foo";
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockROSession.getNode(testPath)).thenReturn(mockNode);
        final ObjectService testObj = new ObjectService();
        testObj.readOnlySession = mockROSession;
        testObj.getObject("/foo");
        testObj.getObject(mockSession, "/foo");
        verify(mockROSession).getNode(testPath);
        verify(mockSession).getNode(testPath);
    }

    @Test
    public void testGetObjectNode() throws RepositoryException {
        final Session mockSession = mock(Session.class);
        final Session mockROSession = mock(Session.class);
		final String testPath = "/foo";
        final ObjectService testObj = new ObjectService();
        testObj.readOnlySession = mockROSession;
        testObj.getObjectNode("/foo");
        testObj.getObjectNode(mockSession, "/foo");
        verify(mockROSession).getNode(testPath);
        verify(mockSession).getNode(testPath);
    }

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
        when(mockIter.nextNode()).thenReturn(mockObj).thenThrow(
                IndexOutOfBoundsException.class);
        when(mockRoot.getNodes()).thenReturn(mockIter);
        when(mockSession.getNode(objPath)).thenReturn(mockRoot);
        final ObjectService testObj = new ObjectService();
        testObj.readOnlySession = mockSession;
        final Set<String> actual = testObj.getObjectNames("");
        verify(mockSession).getNode(objPath);
        assertEquals(1, actual.size());
        assertEquals("foo", actual.iterator().next());
    }

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
        final ObjectService testObj = new ObjectService();
        testObj.deleteObject(mockSession, "foo");
        verify(mockSession).getNode(objPath);
        verify(mockObjNode).remove();
    }
}
