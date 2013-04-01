package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.MOCK_PREFIX;
import static org.fcrepo.api.TestHelpers.MOCK_URI_STRING;
import static org.fcrepo.api.TestHelpers.createMockRepo;
import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.jaxb.responses.management.NamespaceListing;
import org.fcrepo.jaxb.responses.management.NamespaceListing.Namespace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraNamespacesTest {

	FedoraNamespaces testObj;
	
	Repository mockRepo;
	
	Namespace mockNs;
	
	@Before
	public void setUp() throws LoginException, RepositoryException, URISyntaxException {
		mockNs = new Namespace();
		mockNs.prefix = MOCK_PREFIX;
		mockNs.uri = new URI(MOCK_URI_STRING);
		mockRepo = createMockRepo();
    	
		testObj = new FedoraNamespaces();
		testObj.setUriInfo(getUriInfoImpl());
		testObj.setRepository(mockRepo);
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
