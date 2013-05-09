package org.fcrepo.api;

import static junit.framework.Assert.assertNotNull;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.api.FedoraVersions.Version;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraVersionsTest {
	
	FedoraVersions testObj;
	
	NodeService mockNodes;

	Session mockSession;
	
	FedoraObject mockObject;
	FedoraNodes mockObjects;


	

    @Before
    public void setUp() throws LoginException, RepositoryException {

		mockObjects = mock(FedoraNodes.class);

        testObj = new FedoraVersions();
		testObj.objectsResource = mockObjects;

		mockNodes = mock(NodeService.class);
		testObj.setNodeService(mockNodes);

		mockSession = TestHelpers.mockSession(testObj);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
        
        mockObject = mock(FedoraObject.class);
    }

    @Test
    public void testGetObjectVersion() throws Exception {
    	String path = "objects/fedoradatastreamtest1";


		Node mockNode = mock(Node.class);
		when(mockObject.getNode()).thenReturn(mockNode);
		when(mockNodes.getObject(mockSession, "/" + path)).thenReturn(mockObject);

    	testObj.getVersion(createPathList(path), path);

		verify(mockObjects).getObjectProfile(mockNode);
    }
    
    @Test
    public void testGetDatastreamVersion() throws Exception {
    	String path = "objects/fedoradatastreamtest1/ds1";
    	String pid = "testobj";
    	String dsid = "ds1";
    	String content ="emptem";
    	Datastream mockds = TestHelpers.mockDatastream(pid, dsid, content);

		Node mockNode = mock(Node.class);
		when(mockds.hasContent()).thenReturn(true);
		when(mockds.getNode()).thenReturn(mockNode);
		when(mockNodes.getObject(mockSession, "/" + path)).thenReturn(mockds);

		testObj.getVersion(createPathList(path), path);

		verify(mockObjects).getDatastreamProfile(mockNode);
    }
    
    @Test
    public void testGetDatastreamVersionProfile() throws Exception{
    	String path = "objects/fedoradatastreamtest1/ds1";
    	String pid = "testobj";
    	String dsid = "ds1";
    	String content ="emptem";
    	Datastream mockds = TestHelpers.mockDatastream(pid, dsid, content);


		Node mockNode = mock(Node.class);
		when(mockNode.getSession()).thenReturn(mockSession);
		when(mockNode.isNodeType("nt:file")).thenReturn(true);
		when(mockSession.getNode("/" + path)).thenReturn(mockNode);


		when(mockNodes.getObject(mockSession, "/" + path)).thenReturn(mockds);

    	List<Version> versions = testObj.getVersionProfile(createPathList(path));
    	
    	verify(mockNodes).getObject(mockSession, "/" + path);
    	assertTrue(versions.size() == 1);
    	Version v = versions.get(0);
    	assertNotNull(v.getCreated());
    	assertEquals("/" + path, v.getPath());
    }
    
}
