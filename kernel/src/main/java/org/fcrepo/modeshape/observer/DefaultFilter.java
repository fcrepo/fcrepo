package org.fcrepo.modeshape.observer;

import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Sets.newHashSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import org.modeshape.common.SystemFailureException;
import org.modeshape.jcr.api.Repository;

import com.google.common.base.Predicate;

public class DefaultFilter implements EventFilter {

	@Inject
	private Repository repository;

	// it's safe to keep the session around, because this code does not mutate
	// the state of the repository
	private Session session;

	private Predicate<NodeType> isFedoraNodeType = new Predicate<NodeType>() {
		@Override
		public boolean apply(NodeType type) {
			return type.getName().startsWith("fedora:");
		}
	};

	@Override
	public boolean apply(Event event) {

		try {
			return any(newHashSet(session.getNode(event.getPath())
					.getMixinNodeTypes()), isFedoraNodeType);

		} catch (PathNotFoundException e) {
			return false; // not a node in the fedora workspace
		} catch (LoginException e) {
			throw new SystemFailureException(e);
		} catch (RepositoryException e) {
			throw new SystemFailureException(e);
		}
	}

	@PostConstruct
	public void acquireSession() throws LoginException, RepositoryException {
		session = repository.login();
	}

	@PreDestroy
	public void releaseSession() {
		session.logout();
	}
}