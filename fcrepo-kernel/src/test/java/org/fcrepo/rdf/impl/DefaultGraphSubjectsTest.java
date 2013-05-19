package org.fcrepo.rdf.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;

public class DefaultGraphSubjectsTest {

	private DefaultGraphSubjects testObj;
	
	@Before
	public void setUp(){
		testObj = new DefaultGraphSubjects();
	}
	
	@Test
	public void testGetGraphSubject() throws RepositoryException {
		String testPath = "/foo/bar";
		String expected = "info:fedora" + testPath;
		Node mockNode = mock(Node.class); 
		when(mockNode.getPath()).thenReturn(testPath);
		Resource actual = testObj.getGraphSubject(mockNode);
		assertEquals(expected, actual.getURI());
		when(mockNode.getPath()).thenReturn(testPath + "/jcr:content");
		actual = testObj.getGraphSubject(mockNode);
		assertEquals(expected + "/fcr:content", actual.getURI());
	}

	@Test
	public void testGetNodeFromGraphSubject() throws PathNotFoundException, RepositoryException {
		String expected = "/foo/bar";
		Session mockSession = mock(Session.class);
		Node mockNode = mock(Node.class);
		when(mockSession.nodeExists(expected)).thenReturn(true);
		when(mockSession.getNode(expected)).thenReturn(mockNode);
		// test a good subject
		Resource mockSubject = mock(Resource.class);
		when(mockSubject.getURI()).thenReturn("info:fedora" + expected);
		when(mockSubject.isURIResource()).thenReturn(true);		
		Node actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		assertEquals(mockNode, actual);
		// test a bad subject
		when(mockSubject.getURI()).thenReturn("info:fedora2" + expected + "/bad");
		actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		assertEquals(null, actual);
		// test a non-existent path
		when(mockSubject.getURI()).thenReturn("info:fedora" + expected + "/bad");
		actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		assertEquals(null, actual);
		// test a fcr:content path
		when(mockSubject.getURI()).thenReturn("info:fedora" + expected + "/fcr:content");
		actual = testObj.getNodeFromGraphSubject(mockSession, mockSubject);
		verify(mockSession).getNode(expected + "/jcr:content");
	}

	@Test
	public void testIsFedoraGraphSubject() {
		Resource mockSubject = mock(Resource.class);
		when(mockSubject.getURI()).thenReturn("info:fedora/foo");
		when(mockSubject.isURIResource()).thenReturn(true);		
		boolean actual = testObj.isFedoraGraphSubject(mockSubject);
		assertEquals(true, actual);
		when(mockSubject.getURI()).thenReturn("http://fedora/foo");
		actual = testObj.isFedoraGraphSubject(mockSubject);
		assertEquals(false, actual);
	}

}
