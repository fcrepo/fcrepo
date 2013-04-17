
package org.fcrepo;

import static javax.ws.rs.core.Response.noContent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
import org.fcrepo.session.SessionFactory;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    protected SessionFactory sessions;

    /**
     * The fcrepo object service
     */
    @Autowired
    protected ObjectService objectService;

    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Autowired
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

        final Session session = sessions.getSession();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("test",
                "info:fedora/test");
        session.save();
        session.logout();
    }

    protected Session getAuthenticatedSession() {
        return sessions.getSession(securityContext, servletRequest);
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
    public void setSessionFactory(SessionFactory sessions) {
        this.sessions = sessions;
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

    /**
     * A testing convenience setter for otherwise injected resources
     * @param SecurityContext
     */
    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * A testing convenience setter for otherwise injected resources
     * @param HttpServletRequest
     */
    public void setHttpServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

}
