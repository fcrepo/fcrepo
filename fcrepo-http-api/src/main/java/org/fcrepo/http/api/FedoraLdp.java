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
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.RiotException;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static org.apache.commons.lang.StringUtils.isBlank;
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
import static org.fcrepo.kernel.impl.services.TransactionServiceImpl.getCurrentTransactionId;
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

    @PathParam("path") protected String externalPath;

    /**
     * Default JAX-RS entry point
     */
    public FedoraLdp() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath
     */
    @VisibleForTesting
    public FedoraLdp(final String externalPath) {
        this.externalPath = externalPath;
    }

    /**
     * Run these actions after initializing this resource
     */
    @PostConstruct
    public void postConstruct() {
        setUpJMSBaseURIs(uriInfo);
    }

    /**
     * Retrieve the node headers
     * @return response
     * @throws javax.jcr.RepositoryException
     */
    @HEAD
    @Timed
    public Response head() {
        LOGGER.trace("Getting head for: {}", externalPath);

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
                    .topic(translator().reverse().convert(resource().getNode()).asNode());

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

            final String path = toPath(translator(), externalPath);

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
                if ((resource instanceof FedoraObject || resource instanceof Datastream)
                        && isRdfContentType(contentType.toString())) {
                    try {
                        replaceResourceWithStream(resource, requestBodyStream, contentType);
                    } catch (final RiotException e) {
                        throw new BadRequestException("RDF was not parsable", e);
                    }
                } else if (resource instanceof FedoraBinary) {
                    replaceResourceBinaryWithStream((FedoraBinary) resource,
                            requestBodyStream, contentDisposition, requestContentType.toString(), checksum);
                } else {
                    throw new ClientErrorException(UNSUPPORTED_MEDIA_TYPE);
                }

            } else if (!resource.isNew()) {
                throw new ClientErrorException("No RDF provided and the resource already exists!", CONFLICT);
            }

            try {
                session.save();
                versionService.nodeUpdated(resource.getNode());
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

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        if (resource() instanceof FedoraBinary) {
            throw new BadRequestException(resource() + " is not a valid object to receive a PATCH");
        }

        try {
            final String requestBody = IOUtils.toString(requestBodyStream);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            patchResourcewithSparql(resource(), requestBody);

            try {
                session.save();
                versionService.nodeUpdated(resource().getNode());

                if (resource() instanceof Datastream) {
                    versionService.nodeUpdated(((Datastream) resource()).getContentNode());
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

        final String newObjectPath = mintNewPid(slug);

        LOGGER.debug("Attempting to ingest with path: {}", newObjectPath);

        try {

            final MediaType effectiveContentType
                    = requestBodyStream == null || requestContentType == null ? null : contentType;
            final FedoraResource result = createFedoraResource(mixin,
                    effectiveContentType,
                    newObjectPath, contentDisposition);

            if (requestBodyStream == null || requestContentType == null) {
                LOGGER.trace("No request body detected");
            } else {
                LOGGER.trace("Received createObject with a request body and content type \"{}\"", contentTypeString);

                if ((result instanceof FedoraObject || result instanceof Datastream)
                        && isRdfContentType(contentTypeString)) {
                    replaceResourceWithStream(result, requestBodyStream, contentType);
                } else if (result instanceof FedoraBinary) {
                    LOGGER.trace("Created a datastream and have a binary payload.");

                    replaceResourceBinaryWithStream((FedoraBinary) result,
                            requestBodyStream, contentDisposition, contentTypeString, checksum);

                } else if (contentTypeString.equals(contentTypeSPARQLUpdate)) {
                    LOGGER.trace("Found SPARQL-Update content, applying..");
                    patchResourcewithSparql(result, IOUtils.toString(requestBodyStream));
                } else {
                    throw new WebApplicationException(notAcceptable(null)
                            .entity("Invalid Content Type " + contentTypeString).build());
                }
            }

            try {
                session.save();
                versionService.nodeUpdated(result.getNode());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            LOGGER.debug("Finished creating {} with path: {}", mixin, newObjectPath);

            addCacheControlHeaders(servletResponse, result, session);

            final URI location = getUri(result);

            if (result instanceof FedoraBinary) {
                final URI descriptionUri = getUri(((FedoraBinary) result).getDescription());
                servletResponse.addHeader("Link", "<" + descriptionUri + ">;rel=\"describedby\";"
                        + " anchor=\"" + location + "\"");
            }

            return created(location).entity(location.toString()).build();

        } finally {
            session.logout();
        }
    }

    protected void addResourceHttpHeaders(final FedoraResource resource) {
        servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");

        if (resource instanceof Datastream) {
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\"");
        } else if (resource instanceof FedoraBinary) {
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\"");
        } else if (resource instanceof FedoraObject) {
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\"");
        }


        if (getCurrentTransactionId(session) != null) {
            final String canonical = translator().reverse()
                    .convert(resource.getNode())
                    .toString()
                    .replaceFirst("/tx:[^/]+", "");


            servletResponse.addHeader("Link", "<" + canonical + ">;rel=\"canonical\"");

        }

        addOptionsHttpHeaders();
    }

    @Override
    String externalPath() {
        return externalPath;
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

        final FedoraResource resource = resource();


        if (resource instanceof Datastream) {
            final URI binaryUri = getUri(((Datastream) resource).getBinary());
            servletResponse.addHeader("Link", "<" + binaryUri + ">;rel=\"describes\"");
        } else if (resource instanceof FedoraBinary) {
            final URI descriptionUri = getUri(((FedoraBinary) resource).getDescription());
            servletResponse.addHeader("Link", "<" + descriptionUri + ">;rel=\"describedby\"");
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
                if (!s.equals(contentTypeSPARQLUpdate) && !isRdfContentType(s) || s.equals(TEXT_PLAIN)) {
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
    protected Session session() {
        return session;
    }

    private String mintNewPid(final String slug) {
        String pid;

        if (slug != null && !slug.isEmpty()) {
            pid = slug;
        } else {
            pid = pidMinter.mintPid();
        }
        // reverse translate the proffered or created identifier
        LOGGER.trace("Using external identifier {} to create new resource.", pid);
        LOGGER.trace("Using prefixed external identifier {} to create new resource.", uriInfo.getBaseUri() + "/"
                + pid);

        final URI newResourceUri = uriInfo.getAbsolutePathBuilder().clone().path(FedoraLdp.class)
                .resolveTemplate("path", pid, false).build();

        pid = translator().asString(createResource(newResourceUri.toString()));
        try {
            pid = URLDecoder.decode(pid, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // noop
        }
        // remove leading slash left over from translation
        LOGGER.trace("Using internal identifier {} to create new resource.", pid);

        if (nodeService.exists(session, pid)) {
            LOGGER.trace("Resource with path {} already exists; minting new path instead", pid);
            return mintNewPid(null);
        }

        return pid;
    }

}
