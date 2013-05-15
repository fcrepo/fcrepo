package org.fcrepo.session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.ServletCredentials;

public class AuthenticatedSessionProviderImplTest {
	
	private Repository mockRepo;
	@Before
	public void setUp() {
		mockRepo = mock(Repository.class);
	}
	
	@Test
	public void testCredentialsProvided() throws RepositoryException {
		ServletCredentials mockCreds = mock(ServletCredentials.class);
		AuthenticatedSessionProviderImpl test = new AuthenticatedSessionProviderImpl(mockRepo, mockCreds);
		test.getAuthenticatedSession();
		verify(mockRepo).login(mockCreds);
	}

	@Test
	public void testNoCredentialsProvided() throws RepositoryException {
		AuthenticatedSessionProviderImpl test = new AuthenticatedSessionProviderImpl(mockRepo, null);
		test.getAuthenticatedSession();
		verify(mockRepo).login();
	}
}
