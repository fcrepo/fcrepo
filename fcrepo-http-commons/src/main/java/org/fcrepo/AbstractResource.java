package org.fcrepo;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.identifiers.PidMinter;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Repository;


/**
 * Abstract superclass for Fedora JAX-RS Resources, providing convenience fields
 * and methods.
 * 
 * @author ajs6f
 * 
 */
public abstract class AbstractResource extends Constants {


	/**
	 * Useful for constructing URLs
	 */
	@Context
	protected UriInfo uriInfo;

	/**
	 * The JCR repository at the heart of Fedora.
	 */
	@Inject
	protected Repository repo;

	/**
	 * A resource that can mint new Fedora PIDs.
	 */
	@Inject
	protected PidMinter pidMinter;

	/**
	 * A convenience object provided by ModeShape for acting against the JCR
	 * repository.
	 */
	final static protected JcrTools jcrTools = new JcrTools(true);

	@PostConstruct
	public void initialize() throws LoginException, NoSuchWorkspaceException,
			RepositoryException {

		final Session session = repo.login();
		session.getWorkspace().getNamespaceRegistry()
				.registerNamespace("test", "info:fedora/test");
		Node objects = jcrTools.findOrCreateNode(session, "/objects");
		objects.setProperty("size", 0L);
		session.save();
		session.logout();
	}


	protected Long getRepositorySize(Session session)
			throws ValueFormatException, PathNotFoundException,
			RepositoryException {
		return session.getNode("/objects").getProperty("size").getLong();
	}
}
