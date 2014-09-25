/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/25/14
 */

@Scope("request")
@Path("/{path: .*}")
public class FedoraLdp extends ContentExposingResource {


    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    @PathParam("path") protected List<PathSegment> pathList;

    protected String path;


    /**
     * Default JAX-RS entry point
     */
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

    @PostConstruct
    private void postConstruct() {
        this.path = toPath(pathList);
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

        addResourceHttpHeaders(resource());

        return ok().build();
    }

    /**
     * Outputs information about the supported HTTP methods, etc.
     */
    @OPTIONS
    @Timed
    public Response options() {
        addOptionsHttpHeaders();
        return ok().build();
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
            TEXT_HTML, APPLICATION_XHTML_XML, "*/*"})
    public Response describe(@HeaderParam("Prefer") final Prefer prefer,
                             @HeaderParam("Range") final String rangeValue) throws IOException {
        checkCacheControlHeaders(request, servletResponse, resource(), session);

        addResourceHttpHeaders(resource());

        final RdfStream rdfStream = new RdfStream().session(session)
                    .topic(translator().getSubject(resource().getPath()).asNode());

        return getContent(prefer, rangeValue, rdfStream);

    }

    /**
     * Deletes an object.
     *
     * @return response
     * @throws RepositoryException
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
     * Create a resource at a specified path, or replace triples with provided RDF.
     * @param requestContentType
     * @param requestBodyStream
     * @return 204
     */
    @PUT
    @Consumes
    @Timed
    public Response createOrReplaceObjectRdf(
            @HeaderParam("Content-Type") final MediaType requestContentType,
            @ContentLocation final InputStream requestBodyStream,
            @QueryParam("checksum") final String checksum,
            @HeaderParam("Content-Disposition") final ContentDisposition contentDisposition)
            throws InvalidChecksumException {

        try {

            final FedoraResource resource;
            final Response.ResponseBuilder response;


            final MediaType contentType = getSimpleContentType(requestContentType);

            if (nodeService.exists(session, path)) {
                resource = resource();
                response = noContent();
            } else {
                final MediaType effectiveContentType
                        = requestBodyStream == null || requestContentType == null ? null : contentType;
                resource = createFedoraResource(null, effectiveContentType, path, contentDisposition);

                final URI location = getUri(resource);

                response = created(location).entity(location.toString());
            }

            evaluateRequestPreconditions(request, servletResponse, resource, session);

            if (requestContentType != null && requestBodyStream != null)  {

                if (resource instanceof FedoraObject) {
                    final Lang format = contentTypeToLang(contentType.toString());

                    if (format == null || contentType.equals(TEXT_PLAIN_TYPE)) {
                        throw new NotSupportedException();
                    }

                    final Model inputModel = createDefaultModel()
                            .read(requestBodyStream, getUri(resource).toString(), format.getName().toUpperCase());

                    resource.replaceProperties(translator(), inputModel,
                            getTriples(resource, PropertiesRdfContext.class));
                } else if (resource instanceof FedoraBinary) {
                    final URI checksumURI = checksumURI(checksum);
                    final String originalFileName
                            = contentDisposition != null ? contentDisposition.getFileName() : null;

                    ((FedoraBinary) resource).setContent(requestBodyStream,
                            requestContentType.toString(),
                            checksumURI,
                            originalFileName,
                            datastreamService.getStoragePolicyDecisionPoint());
                }

            } else if (!resource.isNew()) {
                throw new ClientErrorException("No RDF provided and the resource already exists!", CONFLICT);
            }

            try {
                session.save();
                versionService.nodeUpdated(resource.getNode());

                if (resource instanceof FedoraBinary) {
                    versionService.nodeUpdated(((FedoraBinary) resource).getDescription().getNode());
                }
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            addCacheControlHeaders(servletResponse, resource, session);

            return response.build();
        } finally {
            session.logout();
        }
    }

    /**
     * Update an object using SPARQL-UPDATE
     *
     * @return 201
     * @throws RepositoryException
     * @throws IOException
     */
    @PATCH
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(@ContentLocation final InputStream requestBodyStream) throws IOException {

        if (resource() instanceof FedoraBinary) {
            throw new BadRequestException(resource() + " is not a valid object to receive a PATCH");
        }

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        try {
            final String requestBody = IOUtils.toString(requestBodyStream);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            final Dataset properties = resource().updatePropertiesDataset(translator(), requestBody);


            handleProblems(properties);

            try {
                session.save();
                versionService.nodeUpdated(resource().getNode());

                if (resource() instanceof FedoraBinary) {
                    versionService.nodeUpdated(((FedoraBinary) resource()).getDescription().getNode());
                }
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            addCacheControlHeaders(servletResponse, resource(), session);

            return noContent().build();

        } catch ( final RuntimeException ex ) {
            final Throwable cause = ex.getCause();
            if ( cause != null && cause instanceof PathNotFoundException) {
                // the sparql update referred to a repository resource that doesn't exist
                throw new BadRequestException(cause.getMessage());
            }
            throw ex;
        } finally {
            session.logout();
        }
    }


    /**
     * Creates a new object.
     *
     * application/octet-stream;qs=1001 is a workaround for JERSEY-2636, to ensure
     * requests without a Content-Type get routed here.
     *
     * @return 201
     */
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM + ";qs=1001", MediaType.WILDCARD})
    @Timed
    public Response createObject(@QueryParam("mixin") final String mixin,
                                 @QueryParam("checksum") final String checksum,
                                 @HeaderParam("Content-Disposition") final ContentDisposition contentDisposition,
                                 @HeaderParam("Content-Type") final MediaType requestContentType,
                                 @HeaderParam("Slug") final String slug,
                                 @ContentLocation final InputStream requestBodyStream)
            throws InvalidChecksumException, IOException {

        if (!(resource() instanceof FedoraObject)) {
            throw new ClientErrorException("Object cannot have child nodes", CONFLICT);
        }

        final MediaType contentType = getSimpleContentType(requestContentType);

        final String contentTypeString = contentType.toString();

        final String newObjectPath = mintNewPid(path, slug);

        LOGGER.debug("Attempting to ingest with path: {}", newObjectPath);

        try {

            final MediaType effectiveContentType
                    = requestBodyStream == null || requestContentType == null ? null : contentType;
            final FedoraResource result = createFedoraResource(mixin,
                    effectiveContentType,
                    newObjectPath, contentDisposition);

            final Response.ResponseBuilder response;
            final URI location = getUri(result);

            if (requestBodyStream == null || requestContentType == null) {
                LOGGER.trace("No request body detected");
            } else {
                LOGGER.trace("Received createObject with a request body and content type \"{}\"", contentTypeString);

                if (contentTypeString.equals(contentTypeSPARQLUpdate)) {
                    LOGGER.trace("Found SPARQL-Update content, applying..");
                    result.updatePropertiesDataset(translator(), IOUtils.toString(requestBodyStream));
                } else if (isRdfContentType(contentTypeString)) {
                    LOGGER.trace("Found a RDF syntax, attempting to replace triples");

                    final Lang lang = contentTypeToLang(contentTypeString);

                    final String format = lang.getName().toUpperCase();

                    final Model inputModel =
                            createDefaultModel().read(requestBodyStream, getUri(result).toString(), format);

                    result.replaceProperties(translator(), inputModel,
                            getTriples(result, PropertiesRdfContext.class));
                } else if (result instanceof FedoraBinary) {
                    LOGGER.trace("Created a datastream and have a binary payload.");

                    final URI checksumURI = checksumURI(checksum);
                    final String originalFileName = contentDisposition != null ? contentDisposition.getFileName() : "";

                    ((FedoraBinary)result).setContent(requestBodyStream,
                            contentTypeString,
                            checksumURI,
                            originalFileName,
                            datastreamService.getStoragePolicyDecisionPoint());
                }
            }

            if (result.isNew()) {
                response = created(location).entity(location.toString());
            } else {
                response = noContent();
            }

            try {
                session.save();
                versionService.nodeUpdated(result.getNode());

                if (result instanceof FedoraBinary) {
                    versionService.nodeUpdated(((FedoraBinary) result).getDescription().getNode());
                }
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            LOGGER.debug("Finished creating {} with path: {}", mixin, newObjectPath);

            addCacheControlHeaders(servletResponse, result, session);

            return response.build();

        } finally {
            session.logout();
        }
    }

    protected void addResourceHttpHeaders(final FedoraResource resource) {

        if (resource instanceof Datastream) {
            final URI binaryUri = getUri(((Datastream) resource).getBinary());
            servletResponse.addHeader("Link", "<" + binaryUri + ">;rel=\"describes\"");
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\"");

        } else if (resource instanceof FedoraBinary) {
            final URI descriptionUri = getUri(((FedoraBinary) resource).getDescription());
            servletResponse.addHeader("Link", "<" + descriptionUri + ">;rel=\"describedby\"");
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\"");

        } else if (resource instanceof FedoraObject) {
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\"");
        }

        if (!translator().isCanonical()) {
            final IdentifierTranslator subjectsCanonical = translator().getCanonical(true);

            try {
                servletResponse.addHeader("Link",
                        "<" + subjectsCanonical.getSubject(resource.getPath()) + ">;rel=\"canonical\"");
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }

        addOptionsHttpHeaders();
    }

    @Override
    String path() {
        return path;
    }

    @Override
    List<PathSegment> pathList() {
        return pathList;
    }

    private void addOptionsHttpHeaders() {
        final String options;

        if (resource() instanceof FedoraBinary) {
            options = "DELETE,HEAD,GET,PUT,OPTIONS";

        } else if (resource() instanceof Datastream) {
            options = "MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS";
            servletResponse.addHeader("Accept-Patch", contentTypeSPARQLUpdate);

        } else if (resource() instanceof FedoraObject) {
            options = "MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS";
            servletResponse.addHeader("Accept-Patch", contentTypeSPARQLUpdate);

            final String rdfTypes = TURTLE + "," + N3 + "," + N3_ALT1 + ","
                    + N3_ALT2 + "," + RDF_XML + "," + NTRIPLES;
            servletResponse.addHeader("Accept-Post", rdfTypes + "," + MediaType.MULTIPART_FORM_DATA
                    + "," + contentTypeSPARQLUpdate);
        } else {
            options = "";
        }

        servletResponse.addHeader("Allow", options);
    }

    private String getRequestedObjectType(final String mixin,
                                          final MediaType requestContentType,
                                          final ContentDisposition contentDisposition) {
        String objectType = FEDORA_OBJECT;

        if (mixin != null) {
            objectType = mixin;
        } else {
            if (requestContentType != null) {
                final String s = requestContentType.toString();
                if (!s.equals(contentTypeSPARQLUpdate) && !isRdfContentType(s)) {
                    objectType = FEDORA_DATASTREAM;
                }
            }

            if (contentDisposition != null && contentDisposition.getType().equals("attachment")) {
                objectType = FEDORA_DATASTREAM;
            }
        }
        return objectType;
    }

    private FedoraResource createFedoraResource(final String requestMixin,
                                                final MediaType requestContentType,
                                                final String path,
                                                final ContentDisposition contentDisposition) {
        final String objectType = getRequestedObjectType(requestMixin, requestContentType, contentDisposition);

        final FedoraResource result;

        switch (objectType) {
            case FEDORA_OBJECT:
                result = objectService.findOrCreateObject(session, path);
                break;
            case FEDORA_DATASTREAM:
                result = datastreamService.findOrCreateDatastream(session, path).getBinary();
                break;
            default:
                throw new ClientErrorException("Unknown object type " + objectType, BAD_REQUEST);
        }
        return result;
    }

    @Override
    Session session() {
        return session;
    }

    private void handleProblems(final Dataset properties) {

        final Model problems = properties.getNamedModel(PROBLEMS_MODEL_NAME);

        if (!problems.isEmpty()) {
            LOGGER.info(
                    "Found these problems updating the properties for {}: {}",
                    path, problems);
            final StringBuilder error = new StringBuilder();
            final StmtIterator sit = problems.listStatements();
            while (sit.hasNext()) {
                final String message = getMessage(sit.next());
                if (StringUtils.isNotEmpty(message) && error.indexOf(message) < 0) {
                    error.append(message + " \n");
                }
            }

            throw new ForbiddenException(error.length() > 0 ? error.toString() : problems.toString());
        }
    }

    /*
     * Return the statement's predicate and its literal value if there's any
     * @param stmt
     * @return
     */
    private static String getMessage(final Statement stmt) {
        final Literal literal = stmt.getLiteral();
        if (literal != null) {
            return stmt.getPredicate().getURI() + ": " + literal.getString();
        }
        return null;
    }

    private String mintNewPid(final String base, final String slug) {
        String pid;
        final String newObjectPath;

        assertPathExists(base);

        if (slug != null && !slug.isEmpty()) {
            pid = slug;
        } else {
            pid = pidMinter.mintPid();
        }
        // reverse translate the proffered or created identifier
        LOGGER.trace("Using external identifier {} to create new resource.", pid);
        LOGGER.trace("Using prefixed external identifier {} to create new resource.", uriInfo.getBaseUri() + "/"
                + pid);
        pid = translator().getPathFromSubject(createResource(uriInfo.getBaseUri() + "/" + pid));
        // remove leading slash left over from translation
        pid = pid.substring(1, pid.length());
        LOGGER.trace("Using internal identifier {} to create new resource.", pid);
        newObjectPath = base + "/" + pid;

        assertPathMissing(newObjectPath);
        return newObjectPath;
    }

    private void assertPathMissing(final String path) {
        if (nodeService.exists(session, path)) {
            throw new ClientErrorException(path + " is an existing resource!", CONFLICT);
        }
    }

    private void assertPathExists(final String path) {
        if (!nodeService.exists(session, path)) {
            throw new NotFoundException();
        }
    }

}
