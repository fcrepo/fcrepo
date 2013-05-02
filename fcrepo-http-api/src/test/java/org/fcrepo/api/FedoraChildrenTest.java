package org.fcrepo.api;

import org.fcrepo.FedoraObject;
import org.fcrepo.identifiers.UUIDPidMinter;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class FedoraChildrenTest {


	FedoraChildren testObj;

	ObjectService mockObjects;

	DatastreamService mockDatastreams;

	Repository mockRepo;

	Session mockSession;

	LowLevelStorageService mockLow;

	@Before
	public void setUp() throws LoginException, RepositoryException {
		mockObjects = mock(ObjectService.class);
		mockDatastreams = mock(DatastreamService.class);
		mockLow = mock(LowLevelStorageService.class);
		testObj = new FedoraChildren();
		testObj.setObjectService(mockObjects);
		mockSession = TestHelpers.mockSession(testObj);
		mockRepo = mock(Repository.class);
	}

	@Test
	public void testGetObjects() throws RepositoryException, IOException {
		final String pid = "testObject";
		final String childPid = "testChild";
		final String path = "/" + pid;
		final FedoraObject mockObj = mock(FedoraObject.class);
		when(mockObj.getName()).thenReturn(pid);
		Set<String> mockNames = new HashSet<String>(Arrays.asList(new String[]{childPid}));
		when(mockObjects.getObjectNames(mockSession, path)).thenReturn(mockNames);
		when(mockObjects.getObjectNames(eq(mockSession), eq(path), any(String.class))).thenReturn(mockNames);
		Response actual = testObj.getObjects(createPathList(pid), null);
		assertNotNull(actual);
		String content = (String) actual.getEntity();
		assertTrue(content, content.contains(childPid));
		verify(mockSession, never()).save();
	}

}
