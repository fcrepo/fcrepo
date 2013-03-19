
package org.fcrepo;

import static javax.ws.rs.core.Response.noContent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.services.ObjectService;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for Fedora JAX-RS Resources, providing convenience fields
 * and methods.
 * 
 * @author ajs6f
 * 
 */
public abstract class AbstractResource {

    final private Logger logger = LoggerFactory
            .getLogger(AbstractResource.class);

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
     * The fcrepo object service
     */
    @Inject
    protected ObjectService objectService;


    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Inject
    protected PidMinter pidMinter;
    
    @Context 
    private HttpServletRequest servletRequest;
    
    @Context 
    private SecurityContext securityContext;

    /**
     * A convenience object provided by ModeShape for acting against the JCR
     * repository.
     */
    final static protected JcrTools jcrTools = new JcrTools(true);

    @PostConstruct
    public void initialize() throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        final Session session = repo.login();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("test",
                "info:fedora/test");
        session.save();
        session.logout();
    }
    
    protected Session getAuthenticatedSession() throws RepositoryException {

        final Session session;
        if(securityContext.getUserPrincipal() != null) {
        	logger.debug("Authenticated user: " + securityContext.getUserPrincipal().getName());
            ServletCredentials credentials = new ServletCredentials(servletRequest);
            session = repo.login(credentials);
        } else {
        	logger.debug("No authenticated user found!");
            session = repo.login();
        }

        return session;
    }

    protected synchronized Response deleteResource(final Node resource)
            throws RepositoryException {

        logger.debug("Attempting to delete resource at path: " +
                resource.getPath());
        final Session session = resource.getSession();

        try {
            resource.remove();
            session.save();
        } finally {
            session.logout();
        }
        return noContent().build();

    }
    
    /**
     * A testing convenience setter for otherwise injected resources
     * @param repo
     */
    public void setRepository(Repository repo) {
    	this.repo = repo;
    }
    
    /**
     * A testing convenience setter for otherwise injected resources
     * @param uriInfo
     */
    public void setUriInfo(UriInfo uriInfo) {
    	this.uriInfo = uriInfo;
    }
    
    /**
     * A testing convenience setter for otherwise injected resources
     * @param pidMinter
     */
    public void setPidMinter(PidMinter pidMinter) {
    	this.pidMinter = pidMinter;
    }

}
