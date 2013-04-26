
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatastreamService.class, FedoraTypesUtils.class,
        ServiceHelpers.class})
public class DatastreamServiceTest implements FedoraJcrTypes {

    private static final String MOCK_CONTENT_TYPE = "application/test-data";

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCreateDatastreamNode() throws Exception {
        final Node mockNode = mock(Node.class);
        final Session mockSession = mock(Session.class);
        final InputStream mockIS = mock(InputStream.class);
        final String testPath = "/foo/bar";
        final Datastream mockWrapper = mock(Datastream.class);
        when(mockWrapper.getNode()).thenReturn(mockNode);
        whenNew(Datastream.class).withArguments(mockSession, testPath)
                .thenReturn(mockWrapper);
        final DatastreamService testObj = new DatastreamService();
		final PolicyDecisionPoint pdp = mock(PolicyDecisionPoint.class);
		when(pdp.evaluatePolicies(mockNode)).thenReturn(null);
		testObj.setStoragePolicyDecisionPoint(pdp);
		PowerMockito.mockStatic(FedoraTypesUtils.class);
		when(FedoraTypesUtils.getBinary(mockNode, mockIS)).thenReturn(mock(Binary.class));
        final Node actual =
                testObj.createDatastreamNode(mockSession, testPath,
                        MOCK_CONTENT_TYPE, mockIS);
        assertEquals(mockNode, actual);
        verifyNew(Datastream.class).withArguments(mockSession, testPath);
        verify(mockWrapper).setContent(any(Binary.class), any(String.class),
                any(String.class), any(String.class));
    }

    @Test
    public void testGetDatastreamNode() throws Exception {
        final Session mockSession = mock(Session.class);
        final Node mockNode = mock(Node.class);
        final Datastream mockWrapper = mock(Datastream.class);
        when(mockWrapper.getNode()).thenReturn(mockNode);
        whenNew(Datastream.class).withArguments(mockSession, "/foo/bar")
                .thenReturn(mockWrapper);
        final DatastreamService testObj = new DatastreamService();
        testObj.readOnlySession = mockSession;
        testObj.getDatastreamNode("/foo/bar");
        verifyNew(Datastream.class).withArguments(mockSession, "/foo/bar");
        verify(mockWrapper).getNode();
    }

    @Test
    public void testGetDatastream() throws Exception {
        final Session mockSession = mock(Session.class);
        final Node mockNode = mock(Node.class);
        final Datastream mockWrapper = mock(Datastream.class);
        when(mockWrapper.getNode()).thenReturn(mockNode);
        whenNew(Datastream.class).withArguments(mockSession, "/foo/bar")
                .thenReturn(mockWrapper);
        final DatastreamService testObj = new DatastreamService();
        testObj.readOnlySession = mockSession;
        testObj.getDatastream("/foo/bar");
        verifyNew(Datastream.class).withArguments(mockSession, "/foo/bar");
    }


	@Test
	public void testGetDatastreamFromPath() throws Exception {
		final Session mockSession = mock(Session.class);
		final Node mockNode = mock(Node.class);
		final Datastream mockWrapper = mock(Datastream.class);
		when(mockWrapper.getNode()).thenReturn(mockNode);
		whenNew(Datastream.class).withArguments(mockSession, "/foo/bar")
				.thenReturn(mockWrapper);
		final DatastreamService testObj = new DatastreamService();
		testObj.readOnlySession = mockSession;
		testObj.getDatastream("/foo/bar");
		verifyNew(Datastream.class).withArguments(mockSession, "/foo/bar");
	}

    @Test
    public void testPurgeDatastream() throws Exception {
        final Session mockSession = mock(Session.class);
        final Node mockNode = mock(Node.class);
        final Datastream mockWrapper = mock(Datastream.class);
        when(mockWrapper.getNode()).thenReturn(mockNode);
        whenNew(Datastream.class).withArguments(mockSession, "/foo/bar")
                .thenReturn(mockWrapper);
        final DatastreamService testObj = new DatastreamService();
        testObj.purgeDatastream(mockSession, "/foo/bar");
        verifyNew(Datastream.class).withArguments(mockSession, "/foo/bar");
        verify(mockWrapper).purge();
    }


    @Test
    public void testExists() throws RepositoryException {
        final Session mockSession = mock(Session.class);
        final DatastreamService testObj = new DatastreamService();
        testObj.readOnlySession = mockSession;
        testObj.exists("/foo/bar");
        verify(mockSession).nodeExists("/foo/bar");
    }
}
