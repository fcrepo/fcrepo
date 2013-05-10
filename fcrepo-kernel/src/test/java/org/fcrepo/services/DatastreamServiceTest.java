
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.FedoraTypesUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.value.binary.StrategyHint;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FedoraTypesUtils.class,
        ServiceHelpers.class})
public class DatastreamServiceTest implements FedoraJcrTypes {

    private static final String MOCK_CONTENT_TYPE = "application/test-data";
    
    private static final String JCR_CONTENT = "jcr:content";
    
    private static final String JCR_DATA = "jcr:data";
    
    private Session mockSession;
    
    private Node mockRoot;
    
    private DatastreamService testObj;

    @Before
    public void setUp() throws RepositoryException {
    	testObj = new DatastreamService();
    	mockSession = mock(Session.class);
    	mockRoot = mock(Node.class);
    	when(mockSession.getRootNode()).thenReturn(mockRoot);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCreateDatastreamNode() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);
        Property mockData = mock(Property.class);
        Binary mockBinary = mock(Binary.class);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        when(mockData.getBinary()).thenReturn(mockBinary);
        final InputStream mockIS = mock(InputStream.class);
        when(mockContent.setProperty(JCR_DATA, mockBinary)).thenReturn(mockData);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockData);
		final PolicyDecisionPoint pdp = mock(PolicyDecisionPoint.class);
		when(pdp.evaluatePolicies(mockNode)).thenReturn(null);
		testObj.setStoragePolicyDecisionPoint(pdp);
		PowerMockito.mockStatic(FedoraTypesUtils.class);
		when(FedoraTypesUtils.getBinary(eq(mockNode), eq(mockIS), any(StrategyHint.class))).thenReturn(mockBinary);

        final Node actual =
                testObj.createDatastreamNode(mockSession, testPath,
                        MOCK_CONTENT_TYPE, mockIS);
        assertEquals(mockNode, actual);
        
        verify(mockContent).setProperty(JCR_DATA, mockBinary);
    }

    @Test
    public void testGetDatastreamNode() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);
        when(mockSession.getNode(testPath)).thenReturn(mockNode);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        final DatastreamService testObj = new DatastreamService();
        testObj.getDatastreamNode(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test
    public void testGetDatastream() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
        testObj.getDatastream(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }


	@Test
	public void testGetDatastreamsForPath() throws Exception {
        final String testPath = "/foo/bar";
        final Node mockNode = mock(Node.class);
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockNode);
		final DatastreamService testObj = new DatastreamService();
		testObj.getDatastreamsForPath(mockSession, "/foo/bar");
		verify(mockNode).getNodes();
	}

    @Test
    public void testExists() throws RepositoryException {
        final Session mockSession = mock(Session.class);
        testObj.exists(mockSession, "/foo/bar");
        verify(mockSession).nodeExists("/foo/bar");
    }
}
