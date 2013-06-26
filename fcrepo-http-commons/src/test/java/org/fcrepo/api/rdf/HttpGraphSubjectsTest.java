
package org.fcrepo.api.rdf;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.uri.UriBuilderImpl;

public class HttpGraphSubjectsTest {

    private HttpGraphSubjects testObj;

    private String testPath = "/foo/bar";

    private Session mockSession;

    @Before
    public void setUp() {
        UriInfo uris = getUriInfoImpl(testPath);
        mockSession = mock(Session.class);
        testObj =
                new HttpGraphSubjects(MockNodeController.class, uris,
                        mockSession);
    }

    @Test
    public void testGetGraphSubject() throws RepositoryException {
        String expected = "http://localhost:8080/fcrepo/rest" + testPath;
        Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn(testPath);
        Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockNode.getSession()).thenReturn(mockSession);
        Resource actual = testObj.getGraphSubject(mockNode);
        assertEquals(expected, actual.getURI());
        when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
        actual = testObj.getGraphSubject(mockNode);
        assertEquals(expected + "/fcr:content", actual.getURI());
    }

    @Test
    public void testGetNodeFromGraphSubject() throws PathNotFoundException,
        RepositoryException {
        Session mockSession = mock(Session.class);
        Node mockNode = mock(Node.class);
        when(mockSession.nodeExists(testPath)).thenReturn(true);
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        // test a good subject
        Resource mockSubject = mock(Resource.class);
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest" + testPath);
        when(mockSubject.isURIResource()).thenReturn(true);
        Node actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        Mockito.verify(mockSession).getNode(testPath);
        assertEquals(mockNode, actual);
        // test a bad subject
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest2" + testPath + "/bad");
        actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        assertEquals(null, actual);
        // test a non-existent path
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest" + testPath + "/bad");
        actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        assertEquals(null, actual);
        // test a fcr:content path
        when(mockSubject.getURI())
                .thenReturn(
                        "http://localhost:8080/fcrepo/rest" + testPath +
                                "/fcr:content");
        actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
        Mockito.verify(mockSession).getNode(testPath + "/jcr:content");
    }

    @Test
    public void testIsFedoraGraphSubject() {
        Resource mockSubject = mock(Resource.class);
        when(mockSubject.getURI()).thenReturn(
                "http://localhost:8080/fcrepo/rest/foo");
        when(mockSubject.isURIResource()).thenReturn(true);
        boolean actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(true, actual);
        when(mockSubject.getURI()).thenReturn("http://fedora/foo");
        actual = testObj.isFedoraGraphSubject(mockSubject);
        assertEquals(false, actual);
    }

    private static UriInfo getUriInfoImpl(String path) {
        // UriInfo ui = mock(UriInfo.class,withSettings().verboseLogging());
        final UriInfo ui = mock(UriInfo.class);
        final UriBuilder ub = new UriBuilderImpl();
        ub.scheme("http");
        ub.host("localhost");
        ub.port(8080);
        ub.path("/fcrepo");

        final UriBuilder rb = new UriBuilderImpl();
        rb.scheme("http");
        rb.host("localhost");
        rb.port(8080);
        rb.path("/fcrepo/rest" + path);

        when(ui.getRequestUri()).thenReturn(
                URI.create("http://localhost:8080/fcrepo/rest" + path));
        when(ui.getBaseUri()).thenReturn(
                URI.create("http://localhost:8080/fcrepo"));
        when(ui.getBaseUriBuilder()).thenReturn(ub);
        when(ui.getAbsolutePathBuilder()).thenReturn(rb);

        return ui;
    }

    @Path("/rest/{path}")
    private class MockNodeController {

    }
}
