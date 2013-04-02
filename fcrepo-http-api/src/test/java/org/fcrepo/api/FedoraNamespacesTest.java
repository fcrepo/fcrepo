package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.MOCK_PREFIX;
import static org.fcrepo.api.TestHelpers.MOCK_URI_STRING;
import static org.fcrepo.api.TestHelpers.getSessionMock;
import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.jaxb.responses.management.NamespaceListing;
import org.fcrepo.jaxb.responses.management.NamespaceListing.Namespace;
import org.fcrepo.session.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraNamespacesTest {

	FedoraNamespaces testObj;
		
	Namespace mockNs;
	
	@Before
	public void setUp() throws LoginException, RepositoryException, URISyntaxException {
		mockNs = new Namespace();
		mockNs.prefix = MOCK_PREFIX;
		mockNs.uri = new URI(MOCK_URI_STRING);
    	
		testObj = new FedoraNamespaces();
		testObj.setUriInfo(getUriInfoImpl());
		SessionFactory mockSessions = mock(SessionFactory.class);
		Session mockSession = getSessionMock();
    	when(mockSessions.getSession()).thenReturn(mockSession);
    	when(mockSessions.getSession(any(SecurityContext.class), any(HttpServletRequest.class))).thenReturn(mockSession);
        testObj.setSessionFactory(mockSessions);
		testObj.setPidMinter(new UUIDPidMinter());
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void testRegisterObjectNamespace() throws RepositoryException {
		Response actual = testObj.registerObjectNamespace(MOCK_PREFIX, MOCK_URI_STRING);
		assertNotNull(actual);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
	}
	
	@Test
	public void testRegisterObjectNamespaces() throws RepositoryException {
		Set<Namespace> mockNses = new HashSet<Namespace>();
		mockNses.add(mockNs);
		NamespaceListing nses = new NamespaceListing();
		nses.namespaces = mockNses;
		Response actual = testObj.registerObjectNamespaces(nses);
		assertNotNull(actual);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
	}

	@Test
	public void testRetrieveObjectNamespace() throws RepositoryException {
		testObj.registerObjectNamespace(MOCK_PREFIX, MOCK_URI_STRING);
		Namespace actual = testObj.retrieveObjectNamespace(MOCK_PREFIX);
		assertNotNull(actual);
		assertEquals(actual.uri,mockNs.uri);
	}
	
	@Test
	public void testGetNamespaces() throws RepositoryException, IOException {
		NamespaceListing actual = testObj.getNamespaces();
		assertNotNull(actual);
	}
}
