package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.createMockRepo;
import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.services.ObjectService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraRepositoryTest {
	
	FedoraRepository testObj;
	
	Repository mockRepo;
	
	ObjectService mockObjects;
	
	@Before
	public void setUp() throws LoginException, RepositoryException {
		testObj = new FedoraRepository();
		mockObjects = mock(ObjectService.class);
		mockRepo = createMockRepo();
		testObj.setRepository(mockRepo);
		testObj.setPidMinter(new UUIDPidMinter());
		testObj.objectService = mockObjects;
		testObj.setUriInfo(getUriInfoImpl());
	}
	
	@After
	public void tearDown() {
		
	}
	
    @Test
    public void testDescribeModeshape() throws RepositoryException, IOException {   	
    	testObj.setRepository(mockRepo);
    	Response actual = testObj.describeModeshape();
    	assertNotNull(actual);
    	assertEquals(Status.OK.getStatusCode(), actual.getStatus());
    }
    
	
    @Test
    public void testDescribe() throws LoginException, RepositoryException {
    	DescribeRepository actual = testObj.describe();
    	assertNotNull(actual);
    }
    
    @Test
    public void testDescribeHtml() throws LoginException, RepositoryException {
    	String actual = testObj.describeHtml();
    	assertNotNull(actual);
    }
}