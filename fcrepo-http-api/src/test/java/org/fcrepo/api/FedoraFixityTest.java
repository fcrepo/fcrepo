package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.Datastream;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraFixityTest {
	FedoraFixity testObj;

	DatastreamService mockDatastreams;

	LowLevelStorageService mockLow;

	Session mockSession;

    private UriInfo uriInfo;
    private Request mockRequest;

    @Before
	public void setUp() throws LoginException, RepositoryException {

        mockRequest = mock(Request.class);
		mockDatastreams = mock(DatastreamService.class);
		mockLow = mock(LowLevelStorageService.class);

		testObj = new FedoraFixity();
		testObj.setDatastreamService(mockDatastreams);

        uriInfo = TestHelpers.getUriInfoImpl();
        testObj.setUriInfo(uriInfo);


		mockSession = TestHelpers.mockSession(testObj);
	}

	@After
	public void tearDown() {

	}
	@Test
	public void testGetDatastreamFixity() throws RepositoryException,
														 IOException {
		final String pid = "FedoraDatastreamsTest1";
		final String path = "/objects/" + pid + "/testDS";
		final String dsId = "testDS";
		final Datastream mockDs = TestHelpers.mockDatastream(pid, dsId, null);
		Node mockNode = mock(Node.class);
		when(mockNode.getSession()).thenReturn(mockSession);
		when(mockDs.getNode()).thenReturn(mockNode);
		when(mockDatastreams.getDatastream(mockSession, path)).thenReturn(mockDs);
		testObj.getDatastreamFixity(createPathList("objects", pid, "testDS"), mockRequest, uriInfo);
        verify(mockDatastreams).getFixityResultsModel(any(GraphSubjects.class), eq(mockDs));
	}
}
