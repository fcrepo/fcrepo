package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.Datastream;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraFixityTest {
	FedoraFixity testObj;

	DatastreamService mockDatastreams;

	LowLevelStorageService mockLow;

	Repository mockRepo;

	Session mockSession;

	SecurityContext mockSecurityContext;

	HttpServletRequest mockServletRequest;

	Principal mockPrincipal;

	String mockUser = "testuser";

	@Before
	public void setUp() throws LoginException, RepositoryException {
		mockDatastreams = mock(DatastreamService.class);
		mockLow = mock(LowLevelStorageService.class);

		testObj = new FedoraFixity();
		testObj.setDatastreamService(mockDatastreams);
		testObj.setLlStoreService(mockLow);

		testObj.setUriInfo(TestHelpers.getUriInfoImpl());


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
		final DatastreamFixity actual = testObj.getDatastreamFixity(createPathList("objects", pid, "testDS"));
		assertNotNull(actual);
		verify(mockLow).runFixityAndFixProblems(mockDs);
	}
}
