package org.fcrepo.session;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.modeshape.jcr.api.ServletCredentials;


public class AuthenticatedSessionProviderImpl implements
		AuthenticatedSessionProvider {
	private final Repository m_repo;
	private final ServletCredentials m_creds;
	public AuthenticatedSessionProviderImpl(Repository repo, ServletCredentials creds) {
		m_repo = repo;
		m_creds = creds;
	}
	@Override
	public Session getAuthenticatedSession(){
		try {
			return (m_creds != null) ? m_repo.login(m_creds) : m_repo.login();
		} catch (RepositoryException e) {
            throw new IllegalStateException(e);
		}
	}

}
