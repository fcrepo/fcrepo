
package org.fcrepo;

import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.AuthenticatedSessionProvider;
import org.fcrepo.session.SessionFactory;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract superclass for Fedora JAX-RS Resources, providing convenience fields
 * and methods.
 * 
 * @author ajs6f
 * 
 */
public abstract class AbstractResource {

    private static final Logger logger = getLogger(AbstractResource.class);

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
     * The fcrepo datastream service
     */
    @Autowired
    protected DatastreamService datastreamService;

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
    protected static final JcrTools jcrTools = new JcrTools(true);

    @PostConstruct
    public void initialize() throws RepositoryException {

        final Session session = sessions.getSession();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("test",
                "info:fedora/test");
        session.save();
        session.logout();
    }

    protected Session getAuthenticatedSession() {
        return sessions.getSession(securityContext, servletRequest);
    }
    
    protected AuthenticatedSessionProvider getAuthenticatedSessionProvider() {
    	return sessions.getSessionProvider(securityContext, servletRequest);
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
    public void setSessionFactory(final SessionFactory sessions) {
        this.sessions = sessions;
    }

    /**
     * A testing convenience setter for otherwise injected resources
     * @param uriInfo
     */
    public void setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    /**
     * A testing convenience setter for otherwise injected resources
     * @param pidMinter
     */
    public void setPidMinter(final PidMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    /**
     * A testing convenience setter for otherwise injected resources
     * @param SecurityContext
     */
    public void setSecurityContext(final SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * A testing convenience setter for otherwise injected resources
     * @param HttpServletRequest
     */
    public void setHttpServletRequest(final HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    public static final String toPath(List<PathSegment> paths) {
        StringBuffer result = new StringBuffer();
        for (PathSegment path: paths) {
			final String p = path.getPath();

			if(!p.equals("")) {
            	result.append('/');
            	result.append(p);
			}
        }
        return result.toString();
    }

}
