package org.fcrepo.http.api;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/23/14
 */
public class FedoraLdp extends AbstractResource {

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    protected String path;

    protected FedoraResource resource;
    protected HttpIdentifierTranslator identifierTranslator;


    /**
     * Retrieve the node headers
     * @return response
     * @throws javax.jcr.RepositoryException
     */
    @HEAD
    @Timed
    public Response head() {
        LOGGER.trace("Getting head for: {}", path);

        checkCacheControlHeaders(request, servletResponse, resource(), session);

        addResourceHttpHeaders(servletResponse, resource(), translator());

        return ok().build();
    }

    protected FedoraResource resource() {
        if (resource == null) {
            resource = nodeService.getObject(session, path);
        }

        return resource;
    }

    protected HttpIdentifierTranslator translator() {
        if (identifierTranslator == null) {
            identifierTranslator = new HttpIdentifierTranslator(session, this.getClass(), uriInfo);
        }

        return identifierTranslator;
    }

    protected URI getUri(final FedoraResource resource) throws URISyntaxException {
        return new URI(translator().getSubject(resource.getPath()).getURI());
    }

    protected String getPath(final String uri) {
        return translator().getPathFromSubject(ResourceFactory.createResource(uri));
    }

    protected void addResourceHttpHeaders(final HttpServletResponse servletResponse,
                                        final FedoraResource resource,
                                        final HttpIdentifierTranslator subjects) {

        if (resource.hasContent()) {
            try {
                servletResponse.addHeader("Link", "<" + subjects.getSubject(
                        resource.getNode().getNode(JCR_CONTENT).getPath()) + ">;rel=\"describes\"");
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }

        if (!subjects.isCanonical()) {
            final IdentifierTranslator subjectsCanonical = subjects.getCanonical(true);

            try {
                servletResponse.addHeader("Link",
                        "<" + subjectsCanonical.getSubject(resource.getPath()) + ">;rel=\"canonical\"");
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }

        addOptionsHttpHeaders(servletResponse);
        servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");
        servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\"");
    }

    protected void addOptionsHttpHeaders(final HttpServletResponse servletResponse) {
        servletResponse.addHeader("Accept-Patch", contentTypeSPARQLUpdate);

        servletResponse.addHeader("Allow", "MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS");
        final String rdfTypes = TURTLE + "," + N3 + "," + N3_ALT1 + ","
                + N3_ALT2 + "," + RDF_XML + "," + NTRIPLES;
        servletResponse.addHeader("Accept-Post", rdfTypes + "," + MediaType.MULTIPART_FORM_DATA
                + "," + contentTypeSPARQLUpdate);
    }

    /**
     * Outputs information about the supported HTTP methods, etc.
     */
    @OPTIONS
    @Timed
    public Response options() {
        addOptionsHttpHeaders(servletResponse);
        return ok().build();
    }

    /**
     * Deletes an object.
     *
     * @return response
     * @throws javax.jcr.RepositoryException
     */
    @DELETE
    @Timed
    public Response deleteObject() {
        try {

            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            resource().delete();

            try {
                session.save();
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            return noContent().build();
        } finally {
            session.logout();
        }
    }
}
