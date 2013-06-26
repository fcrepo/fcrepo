
package org.fcrepo.api;


import static org.junit.Assert.*;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;

import java.io.IOException;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import static org.fcrepo.http.RDFMediaType.POSSIBLE_RDF_VARIANTS;


import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.responses.GraphStoreStreamingOutput;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.hp.hpl.jena.query.Dataset;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
        testObj.setSession(mockSession);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());        
    }
    
    @SuppressWarnings("static-access")
	@Test
    public void testGetVersionList () throws RepositoryException {
        String pid = "FedoraDatastreamsTest1";
        
        final FedoraResource mockResource = mock(FedoraResource.class);
        
        final Request mockRequest = mock(Request.class);
        final Variant mockVariant = mock(Variant.class);        
        final Dataset mockDataset = mock(Dataset.class);
        final Response mockResponse = mock(Response.class);
        final Response mockResponseReturned = mock(Response.class);

        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(mockVariant);        
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(mockResource);
        
        when(mockResource.getVersionDataset(any(HttpGraphSubjects.class))).thenReturn(mockDataset);
        final ResponseBuilder mockResponseBuilder = mock(ResponseBuilder.class);                
//        when(mockResponse.ok(Mockito.anyObject())).thenReturn(mockResponseBuilder);
//        when(mockResponseBuilder.build()).thenReturn(mockResponseReturned);
//        
//        final Response respReturned= testObj.getVersionList(createPathList(pid), mockRequest, TestHelpers.getUriInfoImpl());  
//        
//        assertNotNull(respReturned);
//        assertEquals(mockResponseReturned.getStatus(), respReturned.getStatus());
    }
    
    @Test    
    public void testAddVersionLabel () throws RepositoryException {
    	String pid = "FedoraVersioningTest2";
    	String versionLabel = "FedoraVersioningTest2/fcr:versions/v0.0.1";
    	
    	final FedoraResource mockResource = mock(FedoraResource.class);
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(mockResource);
        
    	final Response resp = testObj.addVersionLabel(createPathList(pid), versionLabel);
    	verify(mockResource).addVersionLabel(anyString());
    	assertNotNull(resp);
    }
    
    @Test    
    public void testGetVersionLabel () throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String versionLabel = "v0.0.1";
    	
    	final FedoraResource mockResource = mock(FedoraResource.class);    	
    	when(mockNodes.getObject(any(Session.class),any(String.class), any(String.class))).thenReturn(mockResource);
    	
    	Dataset ds = testObj.getVersion(createPathList(pid), versionLabel, TestHelpers.getUriInfoImpl());
    	verify(mockResource).getPropertiesDataset(any(HttpGraphSubjects.class), anyLong(), anyInt());
    	
    }

}
