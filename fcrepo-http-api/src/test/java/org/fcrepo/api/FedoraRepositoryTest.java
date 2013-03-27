package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.services.ObjectService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraRepositoryTest {
	
	FedoraRepository testFedoraRepo;
	
	Repository mockRepo;
	
	ObjectService mockObjects;
	
	Session mockSession;
	
	@Before
	public void setUp() throws LoginException, RepositoryException {
		testFedoraRepo = new FedoraRepository();
		mockObjects = mock(ObjectService.class);
		mockRepo = mock(Repository.class);
		mockSession = mock(Session.class);
		when(mockRepo.login()).thenReturn(mockSession);
		testFedoraRepo.setRepository(mockRepo);
		testFedoraRepo.setPidMinter(new UUIDPidMinter());
		testFedoraRepo.objectService = mockObjects;
		testFedoraRepo.setUriInfo(getUriInfoImpl());
	}
	
	@After
	public void tearDown() {
		
	}
	
   /* @Test
    public void testDescribeModeshape() throws RepositoryException, IOException {
    	Response actual = testFedoraRepo.describeModeshape();
    	assertNotNull(actual);
    	assertEquals(Status.OK.getStatusCode(), actual.getStatus());
    }*/
    
	
    @Test
    public void testDescribe() throws LoginException, RepositoryException {
    	DescribeRepository actual = testFedoraRepo.describe();
    	assertNotNull(actual);
    	assertEquals("4.0-modeshape-candidate", actual.getRepositoryVersion());
    }
    
    @Test
    public void testDescribeHtml() throws LoginException, RepositoryException {
    	String actual = testFedoraRepo.describeHtml();
    	assertNotNull(actual);
    	assertEquals(true, actual.contains("4.0-modeshape-candidate"));
    }
}