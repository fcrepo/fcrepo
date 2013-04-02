package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.services.functions.GetBinaryStore;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.BinaryStorage;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;

public class LowLevelStorageServiceTest {
	
	@Test
	public void testGetFixity() {
		
	}
	
	@Test
	public void testTransformBinaryBlobs() {
		
	}
	
	@Test
	public void testGetBinaryBlobs() throws RepositoryException {
		GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
		GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
		Node mockNode = mock(Node.class);
		Repository mockRepo = mock(Repository.class);
		BinaryKey mockKey = mock(BinaryKey.class);
		BinaryStore mockStore = mock(BinaryStore.class);
		when(mockStore.toString()).thenReturn("foo");
		when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
		when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
		LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.getBinaryStore = mockStoreFunc;
		testObj.getBinaryKey = mockKeyFunc;
		testObj.setRepository(mockRepo);
		Set<LowLevelCacheEntry> actual = testObj.getBinaryBlobs(mockNode);
		assertEquals("foo", actual.iterator().next().getExternalIdentifier());
	}

	@Test
	public void testGetBinaryStore() throws LoginException, RepositoryException {
		//TODO Now that this has been refactored into a Function, break the test case out
		JcrRepository mockRepo = mock(JcrRepository.class);
		JcrSession mockSession = mock(JcrSession.class);
		RepositoryConfiguration mockConfig = mock(RepositoryConfiguration.class);
		BinaryStorage mockBinStorage = mock(BinaryStorage.class);
		when(mockConfig.getBinaryStorage()).thenReturn(mockBinStorage);
		when(mockRepo.getConfiguration()).thenReturn(mockConfig);
		when(mockSession.getRepository()).thenReturn(mockRepo);
		when(mockRepo.login()).thenReturn(mockSession);
		GetBinaryStore testObj = new GetBinaryStore();
		testObj.apply(mockRepo);
	}

	@Test
	public void testGetSession() throws LoginException, RepositoryException {
		Repository mockRepo = mock(Repository.class);
		Session mockSession = mock(Session.class);
		Session mockAnotherSession = mock(Session.class);
		when(mockRepo.login()).thenReturn(mockSession, mockAnotherSession);
		LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.setRepository(mockRepo);
		testObj.getSession();
//		verify(mockSession).logout();
		verify(mockRepo,times(2)).login();
	}
	
	@Test
	public void testLogoutSession() throws LoginException, RepositoryException {
		Repository mockRepo = mock(Repository.class);
		Session mockSession = mock(Session.class);
		when(mockRepo.login()).thenReturn(mockSession);
		LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.setRepository(mockRepo);
		testObj.logoutSession();
		verify(mockSession).logout();
		verify(mockRepo).login();
	}
	
	@Test
	public void testSetRepository() throws LoginException, RepositoryException {
		Repository mockRepo = mock(Repository.class);
		Repository mockAnotherRepo = mock(Repository.class);
		Session mockSession = mock(Session.class);
		Session mockAnotherSession = mock(Session.class);
		when(mockRepo.login()).thenReturn(mockSession);
		when(mockAnotherRepo.login()).thenReturn(mockAnotherSession);
		LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.setRepository(mockRepo);
		testObj.setRepository(mockAnotherRepo);
		verify(mockSession).logout();
		verify(mockAnotherRepo).login();
	}
	
	@Test
	public void testRunFixityAndFixProblems() {
		
	}
	
}
