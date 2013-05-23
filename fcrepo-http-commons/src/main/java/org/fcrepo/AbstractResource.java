
package org.fcrepo;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.WebContent;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.AuthenticatedSessionProvider;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.NamespaceTools;
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

    private static final Logger LOGGER = getLogger(AbstractResource.class);

    public static final String TEST_NS_PREFIX = "test";

    public static final String TEST_NS_URI = "info:fedora/test/";

    /**
     * Useful for constructing URLs
     */
    @Context
    protected UriInfo uriInfo;

    @Autowired
    protected SessionFactory sessions;

    /**
     * The fcrepo node service
     */
    @Autowired
    protected NodeService nodeService;

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
        NamespaceTools.getNamespaceRegistry(session).registerNamespace(
                TEST_NS_PREFIX, TEST_NS_URI);
        session.save();
        session.logout();
    }

    protected Session getAuthenticatedSession() {
        return sessions.getSession(securityContext, servletRequest);
    }

    protected AuthenticatedSessionProvider getAuthenticatedSessionProvider() {
        return sessions.getSessionProvider(securityContext, servletRequest);
    }

    protected FedoraResource createObjectOrDatastreamFromRequestContent(
            final Class<?> pathsRelativeTo, final Session session,
            final String path, final String mixin, final UriInfo uriInfo,
            final InputStream requestBodyStream,
            final MediaType requestContentType, final String checksumType,
            final String checksum) throws RepositoryException,
            InvalidChecksumException, IOException {

        final FedoraResource result;

        if (FedoraJcrTypes.FEDORA_OBJECT.equals(mixin)) {
            result = objectService.createObject(session, path);

            if (requestBodyStream != null &&
                    requestContentType != null &&
                    requestContentType.toString().equals(
                            WebContent.contentTypeSPARQLUpdate)) {
                result.updatePropertiesDataset(new HttpGraphSubjects(pathsRelativeTo,
                                                                            uriInfo), IOUtils.toString(requestBodyStream));
            }

        } else if (FedoraJcrTypes.FEDORA_DATASTREAM.equals(mixin)) {
            final MediaType contentType =
                    requestContentType != null ? requestContentType
                            : APPLICATION_OCTET_STREAM_TYPE;

            final Node node =
                    datastreamService.createDatastreamNode(session, path,
                            contentType.toString(), requestBodyStream,
                            checksumType, checksum);
            result = new Datastream(node);
        } else {
            result = null;
        }

        return result;
    }

    protected synchronized Response deleteResource(final Node resource)
            throws RepositoryException {

        LOGGER.debug("Attempting to delete resource at path: " +
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

    public static final String toPath(final List<PathSegment> paths) {
        final StringBuffer result = new StringBuffer();

        for (final PathSegment path : paths) {
            final String p = path.getPath();

            if (!p.equals("")) {
                result.append('/');
                result.append(p);
            }
        }

        final String path = result.toString();

        if (path.isEmpty()) {
            return "/";
        } else {
            return path;
        }
    }

    public void setNodeService(final NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setObjectService(final ObjectService objectService) {
        this.objectService = objectService;
    }

    public void setDatastreamService(final DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }
}
