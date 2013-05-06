package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

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
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraVersionsTest {
	
	FedoraVersions testObj;
	
	ObjectService mockObjects;
	
	DatastreamService mockDatastreams;
	
	Session mockSession;
	
	FedoraObject mockObject;
	

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockObjects = mock(ObjectService.class);
        mockDatastreams = mock(DatastreamService.class);
        testObj = new FedoraVersions();

		mockSession = TestHelpers.mockSession(testObj);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
        testObj.setObjectService(mockObjects);
        testObj.setDatastreamService(mockDatastreams);
        
        mockObject = mock(FedoraObject.class);
    }

    @Test
    public void testGetObjectVersion() throws Exception {
    	String path = "objects/fedoradatastreamtest1";


		Node mockNode = mock(Node.class);
		when(mockNode.getSession()).thenReturn(mockSession);
		when(mockNode.isNodeType("nt:folder")).thenReturn(true);
		when(mockSession.getNode("/" + path)).thenReturn(mockNode);

    	when(mockObject.getCreated()).thenReturn("2013-05-06T15:21:27.480+02:00");
    	when(mockObject.getName()).thenReturn(path);
    	when(mockObject.getLabel()).thenReturn(path);
    	when(mockObject.getLastModified()).thenReturn("2013-05-06T15:22:27.480+02:00");
    	when(mockObject.getOwnerId()).thenReturn("testOwner");
    	when(mockObject.getModels()).thenReturn(null);
    	when(mockObject.getSize()).thenReturn(1291l);
    	when(mockObjects.getObject(mockSession, "/" + path)).thenReturn(mockObject);

    	testObj.getVersion(createPathList(path), path);
    	
    	verify(mockObjects).getObject(mockSession, "/" + path);
    }
    
    @Test
    public void testGetDatastreamVersion() throws Exception {
    	String path = "objects/fedoradatastreamtest1/ds1";
    	String pid = "testobj";
    	String dsid = "ds1";
    	String content ="emptem";
    	Datastream mockds = TestHelpers.mockDatastream(pid, dsid, content);

		Node mockNode = mock(Node.class);
		when(mockNode.getSession()).thenReturn(mockSession);
		when(mockNode.isNodeType("nt:file")).thenReturn(true);
		when(mockSession.getNode("/" + path)).thenReturn(mockNode);



		when(mockDatastreams.getDatastream(mockSession, "/" + path)).thenReturn(mockds);
    	
    	Response resp = testObj.getVersion(createPathList(path),path);
    	
    	verify(mockDatastreams).getDatastream(mockSession, "/" + path);
    	assertTrue(resp.getStatus() == 200);
    	assertTrue(resp.getEntity() instanceof DatastreamProfile);
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


		when(mockDatastreams.getDatastream(mockSession, "/" + path)).thenReturn(mockds);

    	List<Version> versions = testObj.getVersionProfile(createPathList(path));
    	
    	verify(mockDatastreams).getDatastream(mockSession, "/" + path);
    	assertTrue(versions.size() == 1);
    	Version v = versions.get(0);
    	assertTrue(v.getCreated() != null);
    	assertTrue(v.getId() == dsid);
    }
    
}
