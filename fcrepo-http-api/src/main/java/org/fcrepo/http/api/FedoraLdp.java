/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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


import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.core.Variant.mediaTypes;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.jena.atlas.web.ContentType.create;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.ctSPARQLUpdate;
import static org.apache.jena.riot.WebContent.ctTextCSV;
import static org.apache.jena.riot.WebContent.ctTextPlain;
import static org.apache.jena.riot.WebContent.matchContentType;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.Variant.VariantListBuilder;

import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.utils.ContentDigest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.rdf.model.Resource;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */

@Scope("request")
@Path("/{path: .*}")
public class FedoraLdp extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    private static final Splitter.MapSplitter RFC3230_SPLITTER =
        Splitter.on(',').omitEmptyStrings().trimResults().
        withKeyValueSeparator(Splitter.on('=').limit(2));

    private static final String RFC_1123_DATE_TIME = "EEE, dd MMM yyyy HH:mm:ss z";

    static final String INSUFFICIENT_SPACE_IDENTIFYING_MESSAGE = "No space left on device";

    static final String HTTP_HEADER_ACCEPT_PATCH = "Accept-Patch";

    static final String WANT_DIGEST = "Want-Digest";

    static final String DIGEST = "Digest";

    @PathParam("path") protected String externalPath;

    @Inject private FedoraHttpConfiguration httpConfiguration;

    /**
     * Default JAX-RS entry point
     */
    public FedoraLdp() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
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
        setUpJMSInfo(uriInfo, headers);
    }

    /**
     * Retrieve the node headers
     *
     * @return response
     */
    @HEAD
    @Timed
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
        N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
        TURTLE_X, TEXT_HTML_WITH_CHARSET })
    public Response head() throws UnsupportedAlgorithmException {
        LOGGER.info("HEAD for: {}", externalPath);

        checkCacheControlHeaders(request, servletResponse, resource(), session);

        addResourceHttpHeaders(resource());

        Response.ResponseBuilder builder = ok();

        if (resource() instanceof FedoraBinary) {
            final MediaType mediaType = MediaType.valueOf(((FedoraBinary) resource()).getMimeType());

            if (isExternalBody(mediaType)) {
                builder = externalBodyRedirect(URI.create(mediaType.getParameters().get("URL")));
            }

            // we set the content-type explicitly to avoid content-negotiation from getting in the way
            builder.type(mediaType.toString());

            // Respect the Want-Digest header with fixity check
            final String wantDigest = headers.getHeaderString(WANT_DIGEST);
            if (!isNullOrEmpty(wantDigest)) {
                builder.header(DIGEST, handleWantDigestHeader((FedoraBinary)resource(), wantDigest));
            }
        } else {
            final String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
            if (accept == null || "*/*".equals(accept)) {
                builder.type(TURTLE_WITH_CHARSET);
            }
        }

        return builder.build();
    }

    /**
     * Outputs information about the supported HTTP methods, etc.
     * @return the outputs information about the supported HTTP methods, etc.
     */
    @OPTIONS
    @Timed
    public Response options() {
        LOGGER.info("OPTIONS for '{}'", externalPath);
        addLinkAndOptionsHttpHeaders();
        return ok().build();
    }


    /**
     * Retrieve the node profile
     *
     * @param rangeValue the range value
     * @return a binary or the triples for the specified node
     * @throws IOException if IO exception occurred
     */
    @GET
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
            TURTLE_X, TEXT_HTML_WITH_CHARSET})
    public Response getResource(@HeaderParam("Range") final String rangeValue)
            throws IOException, UnsupportedAlgorithmException {
        checkCacheControlHeaders(request, servletResponse, resource(), session);

        LOGGER.info("GET resource '{}'", externalPath);
        final AcquiredLock readLock = lockManager.lockForRead(resource().getPath());
        try (final RdfStream rdfStream = new DefaultRdfStream(asNode(resource()))) {

            // If requesting a binary, check the mime-type if "Accept:" header is present.
            // (This needs to be done before setting up response headers, as getContent
            // returns a response - so changing headers after that won't work so nicely.)
            final ImmutableList<MediaType> acceptableMediaTypes = ImmutableList.copyOf(headers
                    .getAcceptableMediaTypes());

            if (resource() instanceof FedoraBinary && acceptableMediaTypes.size() > 0) {
                final MediaType mediaType = MediaType.valueOf(((FedoraBinary) resource()).getMimeType());

                // Respect the Want-Digest header for fixity check
                final String wantDigest = headers.getHeaderString(WANT_DIGEST);
                if (!isNullOrEmpty(wantDigest)) {
                    servletResponse.addHeader(DIGEST, handleWantDigestHeader((FedoraBinary)resource(), wantDigest));
                }

                if (!acceptableMediaTypes.stream().anyMatch(t -> t.isCompatible(mediaType))) {
                    return notAcceptable(VariantListBuilder.newInstance().mediaTypes(mediaType).build()).build();
                }
            }

            addResourceHttpHeaders(resource());
            return getContent(rangeValue, getChildrenLimit(), rdfStream);
        } finally {
            readLock.release();
        }
    }

    private int getChildrenLimit() {
        final List<String> acceptHeaders = headers.getRequestHeader(ACCEPT);
        if (acceptHeaders != null && acceptHeaders.size() > 0) {
            final List<String> accept = Arrays.asList(acceptHeaders.get(0).split(","));
            if (accept.contains(TEXT_HTML)) {
                // Magic number '100' is tied to common-metadata.vsl display of ellipses
                return 100;
            }
        }

        final List<String> limits = headers.getRequestHeader("Limit");
        if (null != limits && limits.size() > 0) {
            try {
                return Integer.parseInt(limits.get(0));

            } catch (final NumberFormatException e) {
                LOGGER.warn("Invalid 'Limit' header value: {}", limits.get(0));
                throw new ClientErrorException("Invalid 'Limit' header value: " + limits.get(0), SC_BAD_REQUEST, e);
            }
        }
        return -1;
    }

    /**
     * Deletes an object.
     *
     * @return response
     */
    @DELETE
    @Timed
    public Response deleteObject() {
        if (resource() instanceof Container) {
            final String depth = headers.getHeaderString("Depth");
            LOGGER.debug("Depth header value is: {}", depth);
            if (depth != null && !depth.equalsIgnoreCase("infinity")) {
                throw new ClientErrorException("Depth header, if present, must be set to 'infinity' for containers",
                        SC_BAD_REQUEST);
            }
        }

        evaluateRequestPreconditions(request, servletResponse, resource(), session);

        LOGGER.info("Delete resource '{}'", externalPath);

        final AcquiredLock lock = lockManager.lockForDelete(resource().getPath());

        try {
            resource().delete();
            session.commit();
            return noContent().build();
        } finally {
            lock.release();
        }
    }

    /**
     * Create a resource at a specified path, or replace triples with provided RDF.
     *
     * @param requestContentType the request content type
     * @param requestBodyStream the request body stream
     * @param contentDisposition the content disposition value
     * @param ifMatch the if-match value
     * @param links the link values
     * @param digest the digest header
     * @return 204
     * @throws InvalidChecksumException if invalid checksum exception occurred
     * @throws MalformedRdfException if malformed rdf exception occurred
     * @throws UnsupportedAlgorithmException
     */
    @PUT
    @Consumes
    @Timed
    public Response createOrReplaceObjectRdf(
            @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
            @ContentLocation final InputStream requestBodyStream,
            @HeaderParam(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
            @HeaderParam("If-Match") final String ifMatch,
            @HeaderParam(LINK) final List<String> links,
            @HeaderParam("Digest") final String digest)
            throws InvalidChecksumException, MalformedRdfException, UnsupportedAlgorithmException {

        final String interactionModel = checkInteractionModel(links);

        final FedoraResource resource;

        final String path = toPath(translator(), externalPath);

        final AcquiredLock lock = lockManager.lockForWrite(path, session.getFedoraSession(), nodeService);

        try {

            final Collection<String> checksums = parseDigestHeader(digest);

            final MediaType contentType = getSimpleContentType(requestContentType);

            if (nodeService.exists(session.getFedoraSession(), path)) {
                resource = resource();
            } else {
                final MediaType effectiveContentType
                        = requestBodyStream == null || requestContentType == null ? null : contentType;
                resource = createFedoraResource(path, interactionModel, effectiveContentType,
                        !(requestBodyStream == null || requestContentType == null), null);
            }

            if (httpConfiguration.putRequiresIfMatch() && StringUtils.isBlank(ifMatch) && !resource.isNew()) {
                throw new ClientErrorException("An If-Match header is required", 428);
            }

            evaluateRequestPreconditions(request, servletResponse, resource, session);
            final boolean created = resource.isNew();

            try (final RdfStream resourceTriples =
                    created ? new DefaultRdfStream(asNode(resource())) : getResourceTriples()) {
                LOGGER.info("PUT resource '{}'", externalPath);
                if (resource instanceof FedoraBinary) {
                    replaceResourceBinaryWithStream((FedoraBinary) resource,
                            requestBodyStream, contentDisposition, requestContentType, checksums);
                } else if (isRdfContentType(contentType.toString())) {
                    replaceResourceWithStream(resource, requestBodyStream, contentType, resourceTriples);
                } else if (!created) {
                    boolean emptyRequest = true;
                    try {
                        emptyRequest = requestBodyStream.read() == -1;
                    } catch (final IOException ex) {
                        LOGGER.debug("Error checking for request body content", ex);
                    }

                    if (requestContentType == null && emptyRequest) {
                        throw new ClientErrorException("Resource Already Exists", CONFLICT);
                    }
                    throw new ClientErrorException("Invalid Content Type " + requestContentType,
                            UNSUPPORTED_MEDIA_TYPE);
                }
            } catch (final Exception e) {
                checkForInsufficientStorageException(e, e);
            }

            if (hasVersionedResourceLink(links)) {
                resource.enableVersioning();
            }

            ensureInteractionType(resource, interactionModel,
                    (requestBodyStream == null || requestContentType == null));

            session.commit();
            return createUpdateResponse(resource, created);

        } finally {
            lock.release();
        }
    }

    /**
     * Make sure the resource has the specified interaction model
     */
    private static void ensureInteractionType(final FedoraResource resource, final String interactionModel,
            final boolean defaultContent) {
        if (interactionModel != null) {
            if (!interactionModel.equals("ldp:NonRDFSource") && !resource.hasType(interactionModel)) {
                resource.addType(interactionModel);
            }
        } else if (defaultContent) {
            resource.addType("ldp:BasicContainer");
        }
    }

    /**
     * Update an object using SPARQL-UPDATE
     *
     * @param requestBodyStream the request body stream
     * @return 201
     * @throws IOException if IO exception occurred
     */
    @PATCH
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(@ContentLocation final InputStream requestBodyStream)
            throws IOException {

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        if (resource() instanceof FedoraBinary) {
            throw new BadRequestException(resource().getPath() + " is not a valid object to receive a PATCH");
        }

        final AcquiredLock lock = lockManager.lockForWrite(resource().getPath(), session.getFedoraSession(),
                nodeService);

        try {
            final String requestBody = IOUtils.toString(requestBodyStream, UTF_8);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), session);

            try (final RdfStream resourceTriples =
                    resource().isNew() ? new DefaultRdfStream(asNode(resource())) : getResourceTriples()) {
                LOGGER.info("PATCH for '{}'", externalPath);
                patchResourcewithSparql(resource(), requestBody, resourceTriples);
            }
            session.commit();

            addCacheControlHeaders(servletResponse, resource().getDescription(), session);

            return noContent().build();
        } catch (final IllegalArgumentException iae) {
            throw new BadRequestException(iae.getMessage());
        } catch (final AccessDeniedException e) {
            throw e;
        } catch ( final RuntimeException ex ) {
            final Throwable cause = ex.getCause();
            if (cause instanceof PathNotFoundRuntimeException) {
                // the sparql update referred to a repository resource that doesn't exist
                throw new BadRequestException(cause.getMessage());
            }
            throw ex;
        } finally {
            lock.release();
        }
    }

    /**
     * Creates a new object.
     *
     * This originally used application/octet-stream;qs=1001 as a workaround
     * for JERSEY-2636, to ensure requests without a Content-Type get routed here.
     * This qs value does not parse with newer versions of Jersey, as qs values
     * must be between 0 and 1.  We use qs=1.000 to mark where this historical
     * anomaly had been.
     *
     * @param contentDisposition the content Disposition value
     * @param requestContentType the request content type
     * @param slug the slug value
     * @param requestBodyStream the request body stream
     * @param links the link values
     * @param digest the digest header
     * @return 201
     * @throws InvalidChecksumException if invalid checksum exception occurred
     * @throws IOException if IO exception occurred
     * @throws MalformedRdfException if malformed rdf exception occurred
     * @throws UnsupportedAlgorithmException
     */
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM + ";qs=1.000", WILDCARD})
    @Timed
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
            TURTLE_X, TEXT_HTML_WITH_CHARSET, "*/*"})
    public Response createObject(@HeaderParam(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
                                 @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
                                 @HeaderParam("Slug") final String slug,
                                 @ContentLocation final InputStream requestBodyStream,
                                 @HeaderParam(LINK) final List<String> links,
                                 @HeaderParam("Digest") final String digest,
                                 @HeaderParam("Memento-Datetime") final String mementoDatetimeString)
            throws InvalidChecksumException, IOException, MalformedRdfException, UnsupportedAlgorithmException,
                   ParseException {

        final String interactionModel = checkInteractionModel(links);

        if (!(resource() instanceof Container)) {
            throw new ClientErrorException("Object cannot have child nodes", CONFLICT);
        } else if (resource().hasType(FEDORA_PAIRTREE)) {
            throw new ClientErrorException("Objects cannot be created under pairtree nodes", FORBIDDEN);
        }

        final MediaType contentType = getSimpleContentType(requestContentType);

        final String contentTypeString = contentType.toString();

        final String newObjectPath = mintNewPid(slug);

        final AcquiredLock lock = lockManager.lockForWrite(newObjectPath, session.getFedoraSession(), nodeService);

        final SimpleDateFormat sdf = new SimpleDateFormat(RFC_1123_DATE_TIME, Locale.ENGLISH);

        Date mementoDatetime = null;

        if (mementoDatetimeString != null && !"".equals(mementoDatetimeString)) {
          mementoDatetime = sdf.parse(mementoDatetimeString);
        }

        try {

            final Collection<String> checksum = parseDigestHeader(digest);

            LOGGER.info("Ingest with path: {}", newObjectPath);

            final MediaType effectiveContentType
                    = requestBodyStream == null || requestContentType == null ? null : contentType;
            resource = createFedoraResource(newObjectPath, interactionModel, effectiveContentType,
                    !(requestBodyStream == null || requestContentType == null), mementoDatetime);

            try (final RdfStream resourceTriples =
                    resource.isNew() ? new DefaultRdfStream(asNode(resource())) : getResourceTriples()) {

                if (requestBodyStream == null) {
                    LOGGER.trace("No request body detected");
                    if (isTimeGateContainerUrl()) {
                        // Copy from OriginalResource/LDPRv to Memento/LDPRm
                    }
                } else {
                    LOGGER.trace("Received createObject with a request body and content type \"{}\"",
                            contentTypeString);

                    if ((resource instanceof Container) && isRdfContentType(contentTypeString)) {
                        replaceResourceWithStream(resource, requestBodyStream, contentType, resourceTriples);
                    } else if (resource instanceof FedoraBinary) {
                        LOGGER.trace("Created a datastream and have a binary payload.");
                        replaceResourceBinaryWithStream((FedoraBinary) resource,
                                requestBodyStream, contentDisposition, requestContentType, checksum);

                    } else if (contentTypeString.equals(contentTypeSPARQLUpdate)) {
                        LOGGER.trace("Found SPARQL-Update content, applying..");
                        patchResourcewithSparql(resource, IOUtils.toString(requestBodyStream, UTF_8), resourceTriples);
                    } else {
                        if (requestBodyStream.read() != -1) {
                            throw new ClientErrorException("Invalid Content Type " + contentTypeString,
                                    UNSUPPORTED_MEDIA_TYPE);
                        }
                    }
                }

                if (hasVersionedResourceLink(links)) {
                    resource.enableVersioning();
                }

                ensureInteractionType(resource, interactionModel,
                        (requestBodyStream == null || requestContentType == null));

                session.commit();
            } catch (final Exception e) {
                checkForInsufficientStorageException(e, e);
            }

            LOGGER.debug("Finished creating resource with path: {}", newObjectPath);
            return createUpdateResponse(resource, true);
        } finally {
            lock.release();
        }
    }


    /**
     * Returns true if there is a link with a VERSIONED_RESOURCE type.
     * @param links a list of link header values.
     * @return True if there is a matching link header.
     */
    private boolean hasVersionedResourceLink(final List<String> links) {
        if (!CollectionUtils.isEmpty(links)) {
            try {
                for (String link : links) {
                    final Link linq = Link.valueOf(link);
                    if ("type".equals(linq.getRel())) {
                        final Resource type = createResource(linq.getUri().toString());
                        if (type.equals(VERSIONED_RESOURCE)) {
                            return true;
                        }
                    }
                }
            } catch (final RuntimeException e) {
                if (e instanceof IllegalArgumentException | e instanceof UriBuilderException) {
                    throw new ClientErrorException("Invalid link specified: " + String.join(", ", links),
                            BAD_REQUEST);
                }
                throw e;
            }
        }

        return false;
    }
    /**
     * @param rootThrowable The original throwable
     * @param throwable The throwable under direct scrutiny.
     */
    private void checkForInsufficientStorageException(final Throwable rootThrowable, final Throwable throwable)
            throws InvalidChecksumException {
        final String message = throwable.getMessage();
        if (throwable instanceof IOException && message != null && message.contains(
                INSUFFICIENT_SPACE_IDENTIFYING_MESSAGE)) {
            throw new InsufficientStorageException(throwable.getMessage(), rootThrowable);
        }

        if (throwable.getCause() != null) {
            checkForInsufficientStorageException(rootThrowable, throwable.getCause());
        }

        if (rootThrowable instanceof InvalidChecksumException) {
            throw (InvalidChecksumException) rootThrowable;
        } else if (rootThrowable instanceof RuntimeException) {
            throw (RuntimeException) rootThrowable;
        } else {
            throw new RepositoryRuntimeException(rootThrowable);
        }
    }

    /**
     * Create the appropriate response after a create or update request is processed.  When a resource is created,
     * examine the Prefer and Accept headers to determine whether to include a representation.  By default, the
     * URI for the created resource is return as plain text.  If a minimal response is requested, then no body is
     * returned.  If a non-minimal return is requested, return the RDF for the created resource in the appropriate
     * RDF serialization.
     *
     * @param resource The created or updated Fedora resource.
     * @param created True for a newly-created resource, false for an updated resource.
     * @return 204 No Content (for updated resources), 201 Created (for created resources) including the resource
     *    URI or content depending on Prefer headers.
     */
    @SuppressWarnings("resource")
    private Response createUpdateResponse(final FedoraResource resource, final boolean created) {
        addCacheControlHeaders(servletResponse, resource, session);
        addResourceLinkHeaders(resource, created);
        if (!created) {
            return noContent().build();
        }

        final URI location = getUri(resource);
        final Response.ResponseBuilder builder = created(location);

        if (prefer == null || !prefer.hasReturn()) {
            final MediaType acceptablePlainText = acceptabePlainTextMediaType();
            if (acceptablePlainText != null) {
                return builder.type(acceptablePlainText).entity(location.toString()).build();
            }
            return notAcceptable(mediaTypes(TEXT_PLAIN_TYPE).build()).build();
        } else if (prefer.getReturn().getValue().equals("minimal")) {
            return builder.build();
        } else {
            if (prefer != null) {
                prefer.getReturn().addResponseHeaders(servletResponse);
            }
            final RdfNamespacedStream rdfStream = new RdfNamespacedStream(
                new DefaultRdfStream(asNode(resource()), getResourceTriples()),
                    session().getFedoraSession().getNamespaces());
            return builder.entity(rdfStream).build();
        }
    }

    /**
     * Returns an acceptable plain text media type if possible, or null if not.
     */
    private MediaType acceptabePlainTextMediaType() {
        final List<MediaType> acceptable = headers.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.size() == 0) {
            return TEXT_PLAIN_TYPE;
        }
        for (final MediaType type : acceptable ) {
            if (type.isWildcardType() || (type.isCompatible(TEXT_PLAIN_TYPE) && type.isWildcardSubtype())) {
                return TEXT_PLAIN_TYPE;
            } else if (type.isCompatible(TEXT_PLAIN_TYPE)) {
                return type;
            }
        }
        return null;
    }

    @Override
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        super.addResourceHttpHeaders(resource);

        if (session.isBatchSession()) {
            final String canonical = translator().reverse()
                    .convert(resource)
                    .toString()
                    .replaceFirst("/tx:[^/]+", "");


            servletResponse.addHeader(LINK, "<" + canonical + ">;rel=\"canonical\"");

        }

        addLinkAndOptionsHttpHeaders();
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }

    private void addLinkAndOptionsHttpHeaders() {
        // Add Link headers
        addResourceLinkHeaders(resource());

        // Add Options headers
        final String options;
        if (resource() instanceof FedoraBinary) {
            options = "DELETE,HEAD,GET,PUT,OPTIONS";

        } else if (resource() instanceof NonRdfSourceDescription) {
            options = "HEAD,GET,DELETE,PUT,PATCH,OPTIONS";
            servletResponse.addHeader(HTTP_HEADER_ACCEPT_PATCH, contentTypeSPARQLUpdate);

        } else if (resource() instanceof Container) {
            options = "MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS";
            servletResponse.addHeader(HTTP_HEADER_ACCEPT_PATCH, contentTypeSPARQLUpdate);

            final String rdfTypes = TURTLE + "," + N3 + "," + N3_ALT2 + ","
                    + RDF_XML + "," + NTRIPLES + "," + JSON_LD;
            servletResponse.addHeader("Accept-Post", rdfTypes + "," + MediaType.MULTIPART_FORM_DATA
                    + "," + contentTypeSPARQLUpdate);
        } else {
            options = "";
        }

        servletResponse.addHeader("Allow", options);
    }

    private static boolean isRDF(final MediaType requestContentType) {
        if (requestContentType == null) {
            return false;
        }

        final ContentType ctRequest = create(requestContentType.toString());

        // Text files and CSV files are not considered RDF to Fedora, though CSV is a valid
        // RDF type to Jena (although deprecated).
        if (matchContentType(ctRequest, ctTextPlain) || matchContentType(ctRequest, ctTextCSV)) {
            return false;
        }

        // SPARQL updates are done on containers.
        return isRdfContentType(requestContentType.toString()) || matchContentType(ctRequest, ctSPARQLUpdate);
    }

    private FedoraResource createFedoraResource(final String path, final String interactionModel,
            final MediaType contentType, final boolean contentPresent, final Date mementoDatetime) {
        final FedoraResource result;

        Calendar mementoDatetimeCal = null;

        if (isTimeGateContainerUrl()) {
          mementoDatetimeCal = Calendar.getInstance();
          if (mementoDatetime != null) {
            mementoDatetimeCal.setTime(mementoDatetime);
          }
        }
        if ("ldp:NonRDFSource".equals(interactionModel) ||
                (contentPresent && interactionModel == null && !isRDF(contentType))) {
            result = binaryService.findOrCreate(session.getFedoraSession(), path, mementoDatetimeCal);
        } else {
            result = containerService.findOrCreate(session.getFedoraSession(), path, mementoDatetimeCal);
            if (interactionModel != null && !interactionModel.equals("ldp:BasicContainer")) {
                result.addType(interactionModel);
            }
        }

        return result;
    }

    private String mintNewPid(final String slug) {
        String pid;

        if (slug != null && !slug.isEmpty()) {
            pid = slug;
        } else if (pidMinter != null) {
            pid = pidMinter.get();
        } else {
            pid = defaultPidMinter.get();
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
        } catch (final UnsupportedEncodingException e) {
            // noop
        }
        // remove leading slash left over from translation
        LOGGER.trace("Using internal identifier {} to create new resource.", pid);

        if (nodeService.exists(session.getFedoraSession(), pid)) {
            LOGGER.trace("Resource with path {} already exists; minting new path instead", pid);
            return mintNewPid(null);
        }

        return pid;
    }

    private String handleWantDigestHeader(final FedoraBinary binary, final String wantDigest)
            throws UnsupportedAlgorithmException {
        // handle the Want-Digest header with fixity check
        final Collection<String> preferredDigests = parseWantDigestHeader(wantDigest);
        if (preferredDigests.isEmpty()) {
            throw new UnsupportedAlgorithmException(
                    "Unsupported digest algorithm provided in 'Want-Digest' header: " + wantDigest);
        }

        final Collection<URI> checksumResults = binary.checkFixity(idTranslator, preferredDigests);
        final String digestValue = checksumResults.stream().map(uri -> uri.toString().replaceFirst("urn:", "")
                .replaceFirst(":", "=")).collect(Collectors.joining(","));
        return digestValue;
    }

    private static String checkInteractionModel(final List<String> links) {
        if (links == null) {
            return null;
        }

        try {
            for (String link : links) {
                final Link linq = Link.valueOf(link);
                if ("type".equals(linq.getRel())) {
                    final Resource type = createResource(linq.getUri().toString());
                    if (type.equals(NON_RDF_SOURCE) || type.equals(BASIC_CONTAINER) ||
                            type.equals(DIRECT_CONTAINER) || type.equals(INDIRECT_CONTAINER)) {
                        return "ldp:" + type.getLocalName();
                    } else if (type.equals(VERSIONED_RESOURCE)) {
                        // skip if versioned resource link header
                        // NB: the versioned resource header is used for enabling
                        // versioning on a resource and is thus orthogonal to
                        // issue of interaction models. Nevertheless, it is
                        // a possible link header and, therefore, must be ignored.
                    } else {
                        LOGGER.info("Invalid interaction model: {}", type);
                        throw new CannotCreateResourceException("Invalid interaction model: " + type);
                    }
                }
            }
        } catch (final RuntimeException e) {
            if (e instanceof IllegalArgumentException | e instanceof UriBuilderException) {
                throw new ClientErrorException("Invalid link specified: " + String.join(", ", links), BAD_REQUEST);
            }
            throw e;
        }

        return null;
    }

    /**
     * Parse the RFC-3230 Digest response header value.  Look for a
     * sha1 checksum and return it as a urn, if missing or malformed
     * an empty string is returned.
     * @param digest The Digest header value
     * @return the sha1 checksum value
     * @throws UnsupportedAlgorithmException if an unsupported digest is used
     */
    private static Collection<String> parseDigestHeader(final String digest) throws UnsupportedAlgorithmException {
        try {
            final Map<String,String> digestPairs = RFC3230_SPLITTER.split(nullToEmpty(digest));
            final boolean allSupportedAlgorithms = digestPairs.keySet().stream().allMatch(
                    ContentDigest.DIGEST_ALGORITHM::isSupportedAlgorithm);

            // If you have one or more digests that are all valid or no digests.
            if (digestPairs.isEmpty() || allSupportedAlgorithms) {
                return digestPairs.entrySet().stream()
                    .filter(entry -> ContentDigest.DIGEST_ALGORITHM.isSupportedAlgorithm(entry.getKey()))
                    .map(entry -> ContentDigest.asURI(entry.getKey(), entry.getValue()).toString())
                    .collect(Collectors.toSet());
            } else {
                throw new UnsupportedAlgorithmException(String.format("Unsupported Digest Algorithim: %1$s", digest));
            }
        } catch (final RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw new ClientErrorException("Invalid Digest header: " + digest + "\n", BAD_REQUEST);
            }
            throw e;
        }
    }

    /**
     * Parse the RFC-3230 Want-Digest header value.
     * @param wantDigest The Want-Digest header value with optional q value in format:
     *    'md5', 'md5, sha', 'MD5;q=0.3, sha;q=1' etc.
     * @return Digest algorithms that are supported
     */
    private static Collection<String> parseWantDigestHeader(final String wantDigest) {
        final Map<String, Double> digestPairs = new HashMap<String,Double>();
        try {
            final List<String> algs = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(wantDigest);
            // Parse the optional q value with default 1.0, and 0 ignore. Format could be: SHA-1;qvalue=0.1
            for (String alg : algs) {
                final String[] tokens = alg.split(";", 2);
                final double qValue = tokens.length == 1 || tokens[1].indexOf("=") < 0 ?
                        1.0 : Double.parseDouble(tokens[1].split("=", 2)[1]);
                digestPairs.put(tokens[0], qValue);
            }

            return digestPairs.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .filter(entry -> ContentDigest.DIGEST_ALGORITHM.isSupportedAlgorithm(entry.getKey()))
                .map(entry -> entry.getKey()).collect(Collectors.toSet());
        } catch (final NumberFormatException e) {
            throw new ClientErrorException("Invalid 'Want-Digest' header value: " + wantDigest, SC_BAD_REQUEST, e);
        } catch (final RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw new ClientErrorException("Invalid 'Want-Digest' header value: " + wantDigest + "\n", BAD_REQUEST);
            }
            throw e;
        }
    }

    private boolean isTimeGateContainerUrl() {
      boolean isTimeGate = false;
      if (uriInfo != null && uriInfo.getPath() != null) {
        isTimeGate = uriInfo.getPath().endsWith("fcr:versions");
      }
      return isTimeGate;
    }
}
