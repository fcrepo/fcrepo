/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.services.FixityService;

import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.google.common.annotations.VisibleForTesting;

import io.micrometer.core.annotation.Timed;

/**
 * Run a fixity check on a path
 *
 * @author ajs6f
 * @since Jun 12, 2013
 */
@Timed
@Scope("request")
@Path("/{path: .*}/fcr:fixity")
public class FedoraFixity extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraFixity.class);

    private static final String OPTIONS_VALUES = "OPTIONS, GET";

    @PathParam("path") protected String externalPath;

    @Inject private FixityService fixityService;

    /**
     * Default JAX-RS entry point
     */
    public FedoraFixity() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraFixity(final String externalPath) {
        this.externalPath = externalPath;
    }

    /**
     * Get the results of a fixity check for a path
     *
     * GET /path/to/some/datastream/fcr:fixity
     *
     * @return datastream fixity in the given format
     */
    @GET
    @HtmlTemplate(value = "fcr:fixity")
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8", N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET,
            RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET, TEXT_HTML_WITH_CHARSET, "*/*"})
    public RdfNamespacedStream getDatastreamFixity() {

        if (!(resource() instanceof Binary)) {
            throw new NotFoundException("Error: Resource at " + resource().getFedoraId().getFullIdPath() + " is not a" +
                    " binary");
        }

        final Link.Builder resourceLink = Link.fromUri(RESOURCE.getURI()).rel("type");
        servletResponse.addHeader(LINK, resourceLink.build().toString());
        final Link.Builder rdfSourceLink = Link.fromUri(RDF_SOURCE.getURI()).rel("type");
        servletResponse.addHeader(LINK, rdfSourceLink.build().toString());

        final Binary binaryResource = (Binary) resource();
        LOGGER.info("Get fixity for '{}'", externalPath);

        final RdfStream rdfStream = httpRdfService.bodyToExternalStream(getUri(binaryResource).toString(),
                fixityService.checkFixity(binaryResource), identifierConverter());
        return new RdfNamespacedStream(rdfStream, namespaceRegistry.getNamespaces());
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }

    @OPTIONS
    public Response options() {
        return Response.ok().header(ALLOW, OPTIONS_VALUES).build();
    }
    /*
     * These methods are disallowed, but need to exist here or the path gets caught by the FedoraLdp path matcher.
     */
    @HEAD
    public Response get() {
        return methodNotAllowed();
    }
    @POST
    public Response post() {
        return methodNotAllowed();
    }
    @PUT
    public Response put() {
        return methodNotAllowed();
    }
    @PATCH
    public Response patch() {
        return methodNotAllowed();
    }
    @DELETE
    public Response delete() {
        return methodNotAllowed();
    }

    private Response methodNotAllowed() {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).header(ALLOW, OPTIONS_VALUES).build();
    }
}
