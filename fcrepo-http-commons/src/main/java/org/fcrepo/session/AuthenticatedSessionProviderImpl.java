package org.fcrepo.session;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.modeshape.jcr.api.ServletCredentials;


public class AuthenticatedSessionProviderImpl implements
		AuthenticatedSessionProvider {
	private final Repository repository;
	private final ServletCredentials credentials;
	public AuthenticatedSessionProviderImpl(Repository repo, ServletCredentials creds) {
		repository = repo;
		credentials = creds;
	}
	@Override
	public Session getAuthenticatedSession(){
		try {
			return (credentials != null) ? repository.login(credentials) : repository.login();
		} catch (RepositoryException e) {
            throw new IllegalStateException(e);
		}
	}

}
