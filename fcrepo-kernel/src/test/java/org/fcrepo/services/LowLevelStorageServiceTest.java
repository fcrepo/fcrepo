package org.fcrepo.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.BinaryStorage;

public class LowLevelStorageServiceTest {
	
	@Test
	public void testGetFixity() {
		
	}
	
	@Test
	public void testTransformBinaryBlobs() {
		
	}
	
	@Test
	public void testGetBinaryBlobs() {
		
	}

	@Test
	public void testGetBinaryStore() throws LoginException, RepositoryException {
		Repository mockRepo = mock(Repository.class);
		Session mockSession = mock(Session.class);
		JcrRepository mockJcr = mock(JcrRepository.class);
		RepositoryConfiguration mockConfig = mock(RepositoryConfiguration.class);
		BinaryStorage mockBinStorage = mock(BinaryStorage.class);
		when(mockConfig.getBinaryStorage()).thenReturn(mockBinStorage);
		when(mockJcr.getConfiguration()).thenReturn(mockConfig);
		when(mockSession.getRepository()).thenReturn(mockJcr);
		when(mockRepo.login()).thenReturn(mockSession);
		LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.setRepository(mockRepo);
		testObj.getBinaryStore();
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
