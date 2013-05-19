package org.fcrepo.api.rdf;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
	
	@Before
	public void setUp(){
		UriInfo uris = getUriInfoImpl(testPath);
		testObj = new HttpGraphSubjects(MockNodeController.class, uris);
	}
	
	@Test
	public void testGetGraphSubject() throws RepositoryException {
		String expected = "http://localhost:8080/fcrepo/rest" + testPath;
		Node mockNode = Mockito.mock(Node.class);
		Mockito.when(mockNode.getPath()).thenReturn(testPath);
		Resource actual = testObj.getGraphSubject(mockNode);
		assertEquals(expected, actual.getURI());
		Mockito.when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
		actual = testObj.getGraphSubject(mockNode);
		assertEquals(expected + "/fcr:content", actual.getURI());
	}

	@Test
	public void testGetNodeFromGraphSubject() throws PathNotFoundException, RepositoryException {
		Session mockSession = Mockito.mock(Session.class);
		Node mockNode = Mockito.mock(Node.class);
		Mockito.when(mockSession.nodeExists(testPath)).thenReturn(true);
		Mockito.when(mockSession.getNode(testPath)).thenReturn(mockNode);
		// test a good subject
		Resource mockSubject = Mockito.mock(Resource.class);
		Mockito.when(mockSubject.getURI()).thenReturn("http://localhost:8080/fcrepo/rest" + testPath);
		Mockito.when(mockSubject.isURIResource()).thenReturn(true);
		Node actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		Mockito.verify(mockSession).getNode(testPath);
		assertEquals(mockNode, actual);
		// test a bad subject
		Mockito.when(mockSubject.getURI()).thenReturn("http://localhost:8080/fcrepo/rest2" + testPath + "/bad");
		actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		assertEquals(null, actual);
		// test a non-existent path
		Mockito.when(mockSubject.getURI()).thenReturn("http://localhost:8080/fcrepo/rest" + testPath + "/bad");
		actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		assertEquals(null, actual);
		// test a fcr:content path
		Mockito.when(mockSubject.getURI()).thenReturn("http://localhost:8080/fcrepo/rest" + testPath + "/fcr:content");
		actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		Mockito.verify(mockSession).getNode(testPath + "/jcr:content");
	}

	@Test
	public void testIsFedoraGraphSubject() {
		Resource mockSubject = Mockito.mock(Resource.class);
		Mockito.when(mockSubject.getURI()).thenReturn("http://localhost:8080/fcrepo/rest/foo");
		Mockito.when(mockSubject.isURIResource()).thenReturn(true);
		boolean actual = testObj.isFedoraGraphSubject(mockSubject);
		assertEquals(true, actual);
		Mockito.when(mockSubject.getURI()).thenReturn("http://fedora/foo");
		actual = testObj.isFedoraGraphSubject(mockSubject);
		assertEquals(false, actual);
	}

	private static UriInfo getUriInfoImpl(String path) {
		// UriInfo ui = mock(UriInfo.class,withSettings().verboseLogging());
		final UriInfo ui = Mockito.mock(UriInfo.class);
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

		Mockito.when(ui.getRequestUri()).thenReturn(
				URI.create("http://localhost:8080/fcrepo/rest" + path));
		Mockito.when(ui.getBaseUri()).thenReturn(URI.create("http://localhost:8080/fcrepo"));
		Mockito.when(ui.getBaseUriBuilder()).thenReturn(ub);
		Mockito.when(ui.getAbsolutePathBuilder()).thenReturn(rb);

		return ui;
	}

    @Path("/rest/{path}")
    private class MockNodeController {

    }
}
