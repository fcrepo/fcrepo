package org.fcrepo.http.api;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.transform;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
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

    public FedoraLdp() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param path
     */
    @VisibleForTesting
    public FedoraLdp(final String path) {
        this.path = path;
    }


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

    /**
     * Retrieve the node profile
     *
     * @return triples for the specified node
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE + ";qs=10", JSON_LD + ";qs=8",
            N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML})
    public RdfStream describe(@HeaderParam("Prefer") final Prefer prefer) {
        LOGGER.trace("Getting profile for: {}", path);

        checkCacheControlHeaders(request, servletResponse, resource(), session);

        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else {
            returnPreference = new PreferTag("");
        }

        final RdfStream rdfStream = getObjectDescriptionStream(returnPreference);

        returnPreference.addResponseHeaders(servletResponse);

        addResourceHttpHeaders(servletResponse, resource(), translator());

        addResponseInformationToStream(resource(), rdfStream, uriInfo,
                translator());

        return rdfStream;


    }

    private RdfStream getObjectDescriptionStream(final PreferTag returnPreference) {
        final RdfStream rdfStream = getTriples(PropertiesRdfContext.class).session(session)
                .topic(translator().getSubject(resource().getPath()).asNode());

        if (!returnPreference.isMinimal()) {
            final LdpPreferTag ldpPreferences = new LdpPreferTag(returnPreference);

            if (ldpPreferences.prefersReferences()) {
                rdfStream.concat(getTriples(ReferencesRdfContext.class));
            }

            rdfStream.concat(getTriples(ParentRdfContext.class));

            if (ldpPreferences.prefersContainment() || ldpPreferences.prefersMembership()) {
                rdfStream.concat(getTriples(ChildrenRdfContext.class));
            }

            if (ldpPreferences.prefersContainment()) {

                final Iterator<FedoraResource> children = resource().getChildren();

                rdfStream.concat(concat(transform(children,
                        new Function<FedoraResource, RdfStream>() {

                            @Override
                            public RdfStream apply(final FedoraResource child) {
                                return child.getTriples(translator(), PropertiesRdfContext.class);
                            }
                        })));

            }

            rdfStream.concat(getTriples(ContainerRdfContext.class));
        }
        return rdfStream;
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

    protected RdfStream getTriples(final Class<? extends RdfStream> x) {
        return getTriples(resource(), x);
    }

    protected RdfStream getTriples(final FedoraResource resource, final Class<? extends RdfStream> x) {
        return resource.getTriples(translator(), x);
    }


}
