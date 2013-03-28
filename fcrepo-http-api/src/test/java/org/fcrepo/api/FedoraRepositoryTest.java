package org.fcrepo.api;

import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
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
	
    @Test
    public void testDescribeModeshape() throws RepositoryException, IOException {
    	when(mockRepo.getDescriptor("jcr.repository.name")).thenReturn("Mock Repository");
    	String[] mockKeys = { "mock1" };
    	String[] mockPrefix = mockKeys;
    	
    	Workspace mockWorkspace = mock(Workspace.class);
    	NamespaceRegistry mockNameReg = mock(NamespaceRegistry.class);
    	NodeTypeManager mockNTM = mock(NodeTypeManager.class);
    	NodeTypeIterator mockNTI = mock(NodeTypeIterator.class);
    	NodeType mockNodeType = mock(NodeType.class);
    
    	when(mockRepo.getDescriptorKeys()).thenReturn(mockKeys);
    	testFedoraRepo.setRepository(mockRepo);
    	
    	when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
    	
    	when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNameReg);
    	when(mockNameReg.getPrefixes()).thenReturn(mockPrefix);
    	when(mockNameReg.getURI("mock1")).thenReturn("mock.namespace.org");
    	
    	when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNTM);
    	
    	//Next two lines cause out of memory
    	/*when(mockNodeType.getName()).thenReturn("mockName");
    	when(mockNodeType.toString()).thenReturn("mockString");*/
    	when(mockNTM.getAllNodeTypes()).thenReturn(mockNTI);
    	when(mockNTI.hasNext()).thenReturn(true);
    	when(mockNTI.nextNodeType()).thenReturn(mockNodeType);
    	
    	Response actual = testFedoraRepo.describeModeshape();
    	
    	assertNotNull(actual);
    	assertEquals(Status.OK.getStatusCode(), actual.getStatus());
    }
    
	
    @Test
    public void testDescribe() throws LoginException, RepositoryException {
    	DescribeRepository actual = testFedoraRepo.describe();
    	assertNotNull(actual);
    }
    
    @Test
    public void testDescribeHtml() throws LoginException, RepositoryException {
    	String actual = testFedoraRepo.describeHtml();
    	assertNotNull(actual);
    }
}