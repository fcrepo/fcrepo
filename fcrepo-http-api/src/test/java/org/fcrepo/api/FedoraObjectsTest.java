package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.FedoraObject;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraObjectsTest {

	FedoraObjects testObj;
	
	ObjectService mockObjects;
	
	Repository mockRepo;
	
	Session mockSession;
	
	SecurityContext mockSecurityContext;
	
	HttpServletRequest mockServletRequest;
	
	Principal mockPrincipal;
	
	String mockUser = "testuser";

	@Before
	public void setUp() throws LoginException, RepositoryException{
		mockSecurityContext = mock(SecurityContext.class);
		mockServletRequest = mock(HttpServletRequest.class);
		mockPrincipal = mock(Principal.class);
		mockObjects = mock(ObjectService.class);
		testObj = new FedoraObjects();
		testObj.objectService = mockObjects;
		mockRepo = mock(Repository.class);
		mockSession = mock(Session.class);
		when(mockSession.getUserID()).thenReturn(mockUser);
		when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
		when(mockPrincipal.getName()).thenReturn(mockUser);
    	SessionFactory mockSessions = mock(SessionFactory.class);
    	when(mockSessions.getSession()).thenReturn(mockSession);
    	when(mockSessions.getSession(any(SecurityContext.class), any(HttpServletRequest.class))).thenReturn(mockSession);
        testObj.setSessionFactory(mockSessions);
		testObj.setUriInfo(getUriInfoImpl());
		testObj.setPidMinter(new UUIDPidMinter());
		testObj.setHttpServletRequest(mockServletRequest);
		testObj.setSecurityContext(mockSecurityContext);
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void testGetObjects() throws RepositoryException {
		Response actual = testObj.getObjects();
		assertNotNull(actual);
		assertEquals(Status.OK.getStatusCode(), actual.getStatus());
		verify(mockObjects).getObjectNames();
		verify(mockSession, never()).save();
	}
	
	@Test
	public void testIngestAndMint() throws RepositoryException {
		Response actual = testObj.ingestAndMint();
		assertNotNull(actual);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
		verify(mockSession).save();
	}
	
	@Test
	public void testModify() throws RepositoryException {
		String pid = "testObject";
		Response actual = testObj.modify(pid);
		assertNotNull(actual);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
    	// this verify will fail when modify is actually implemented, thus encouraging the unit test to be updated appropriately.
    	verifyNoMoreInteractions(mockObjects);
		verify(mockSession).save();
	}
	
	@Test
	public void testIngest() throws RepositoryException {
		String pid = "testObject";
		Response actual = testObj.ingest(pid, null);
		assertNotNull(actual);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
    	assertTrue(actual.getEntity().toString().endsWith(pid));
    	verify(mockObjects).createObject(mockSession, pid);
		verify(mockSession).save();
	}
	
	@Test
	public void testGetObject() throws RepositoryException, IOException {
		String pid = "testObject";
		FedoraObject mockObj = mock(FedoraObject.class);
		when(mockObjects.getObject(pid)).thenReturn(mockObj);
		ObjectProfile actual = testObj.getObject(pid);
		assertNotNull(actual);
		assertEquals(pid, actual.pid);
    	verify(mockObjects).getObject(pid);
		verify(mockSession, never()).save();
	}
	
	@Test
	public void testDeleteObject() throws RepositoryException {
		String pid = "testObject";
		Response actual = testObj.deleteObject(pid);
		assertNotNull(actual);
    	assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
    	verify(mockObjects).deleteObject(pid, mockSession);
		verify(mockSession).save();
	}
}
