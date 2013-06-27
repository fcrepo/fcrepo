
package org.fcrepo.api;


import static org.junit.Assert.*;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import static org.fcrepo.http.RDFMediaType.POSSIBLE_RDF_VARIANTS;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;

import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
    
    @Test
    public void testGetVersionList () throws RepositoryException {
        String pid = "FedoraVersioningTest";
        
        final FedoraResource mockResource = mock(FedoraResource.class);
        final Request mockRequest = mock(Request.class);
        final Variant mockVariant = mock(Variant.class);        
        final Dataset mockDataset = mock(Dataset.class);

        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(mockVariant);        
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(mockResource);
        when(mockResource.getVersionDataset(any(HttpGraphSubjects.class))).thenReturn(mockDataset);
        when(mockVariant.getMediaType()).thenReturn(new MediaType("text","turtle"));
        
        final Response response= testObj.getVersionList(createPathList(pid), mockRequest, TestHelpers.getUriInfoImpl());  
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }
    
    @Test    
    public void testAddVersionLabel () throws RepositoryException {
    	String pid = "FedoraVersioningTest";
    	String versionLabel = "FedoraVersioningTest1/fcr:versions/v0.0.1";
    	
    	final FedoraResource mockResource = mock(FedoraResource.class);
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(mockResource);
        
    	final Response response = testObj.addVersionLabel(createPathList(pid), versionLabel);
    	verify(mockResource).addVersionLabel(anyString());
    	assertNotNull(response);
    }
    
    @Test    
    public void testGetVersion () throws RepositoryException, IOException {
    	String pid = "FedoraVersioningTest";
    	String versionLabel = "v0.0.1";
    	
    	final FedoraResource mockResource = mock(FedoraResource.class);    	
    	when(mockNodes.getObject(any(Session.class),any(String.class), any(String.class))).thenReturn(mockResource);
    	Dataset ds = testObj.getVersion(createPathList(pid), versionLabel, TestHelpers.getUriInfoImpl());
    	verify(mockResource).getPropertiesDataset(any(HttpGraphSubjects.class), anyLong(), anyInt());    	
    }

}
