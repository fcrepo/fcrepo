package org.fcrepo.api;

import static org.mockito.Mockito.mock;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;

public class FedoraVersionsTest {
	
	FedoraVersions testObj;
	
	NodeService mockNodes;

	Session mockSession;

	

    @Before
    public void setUp() throws LoginException, RepositoryException {

	    testObj = new FedoraVersions();

		mockNodes = mock(NodeService.class);
		testObj.setNodeService(mockNodes);

		mockSession = TestHelpers.mockSession(testObj);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
        
    }


    
}
