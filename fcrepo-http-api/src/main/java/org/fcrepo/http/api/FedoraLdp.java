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
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_TYPE;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_OCTET_STREAM_TYPE;

import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODEL_RESOURCES;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_RFC_1123_FORMATTER;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

import io.micrometer.core.annotation.Timed;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.GhostNodeException;
import org.fcrepo.kernel.api.exception.InteractionModelViolationException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.MementoDatetimeFormatException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.services.FixityService;
import org.fcrepo.kernel.api.services.ReplaceBinariesService;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */

@Timed
@Scope("request")
@Path("/{path: .*}")
public class FedoraLdp extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraLdp.class);

    private static final String WANT_DIGEST = "Want-Digest";

    private static final String DIGEST = "Digest";

    private static final MediaType DEFAULT_RDF_CONTENT_TYPE = TURTLE_TYPE;
    private static final MediaType DEFAULT_NON_RDF_CONTENT_TYPE = APPLICATION_OCTET_STREAM_TYPE;

    @PathParam("path") protected String externalPath;

    @Inject
    private FixityService fixityService;

    @Inject
    private FedoraHttpConfiguration httpConfiguration;

    @Inject
    protected ReplaceBinariesService replaceBinariesService;

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
     * Retrieve the node headers
     *
     * @return response
     * @throws UnsupportedAlgorithmException if unsupported digest algorithm occurred
     */
    @HEAD
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
        N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
        TURTLE_X, TEXT_HTML_WITH_CHARSET })
    public Response head() throws UnsupportedAlgorithmException {
        LOGGER.info("HEAD for: {}", externalPath);

        final String datetimeHeader = headers.getHeaderString(ACCEPT_DATETIME);
        if (!isBlank(datetimeHeader) && resource().isOriginalResource()) {
            return getMemento(datetimeHeader, resource());
        }

        checkCacheControlHeaders(request, servletResponse, resource(), transaction());

        addResourceHttpHeaders(resource());

        Response.ResponseBuilder builder = ok();

        if (resource() instanceof Binary) {
            final Binary binary = (Binary) resource();
            final MediaType mediaType = getBinaryResourceMediaType(binary);

            if (binary.isRedirect()) {
                builder = temporaryRedirect(binary.getExternalURI());
            }

            // we set the content-type explicitly to avoid content-negotiation from getting in the way
            builder.type(mediaType.toString());

            // Respect the Want-Digest header with fixity check
            final String wantDigest = headers.getHeaderString(WANT_DIGEST);
            if (!isNullOrEmpty(wantDigest)) {
                builder.header(DIGEST, handleWantDigestHeader(binary, wantDigest));
            }
        } else {
            final String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
            if (accept == null || "*/*".equals(accept)) {
                builder.type(TURTLE_WITH_CHARSET);
            }
            setVaryAndPreferenceAppliedHeaders(servletResponse, prefer, resource());
        }


        return builder.build();
    }

    /**
     * Outputs information about the supported HTTP methods, etc.
     * @return the outputs information about the supported HTTP methods, etc.
     */
    @OPTIONS
    public Response options() {
        LOGGER.info("OPTIONS for '{}'", externalPath);

        addLinkAndOptionsHttpHeaders(resource());
        return ok().build();
    }


    /**
     * Retrieve the node profile
     *
     * @param rangeValue the range value
     * @return a binary or the triples for the specified node
     * @throws IOException if IO exception occurred
     * @throws UnsupportedAlgorithmException if unsupported digest algorithm occurred
     */
    @GET
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
            TURTLE_X, TEXT_HTML_WITH_CHARSET})
    public Response getResource(@HeaderParam("Range") final String rangeValue)
            throws IOException, UnsupportedAlgorithmException {

        final String datetimeHeader = headers.getHeaderString(ACCEPT_DATETIME);
        if (!isBlank(datetimeHeader) && resource().isOriginalResource()) {
            return getMemento(datetimeHeader, resource());
        }

        checkCacheControlHeaders(request, servletResponse, resource(), transaction());

        final ImmutableList<MediaType> acceptableMediaTypes = ImmutableList.copyOf(headers
                .getAcceptableMediaTypes());

        LOGGER.info("GET resource '{}'", externalPath);
        addResourceHttpHeaders(resource());

        if (resource() instanceof Binary) {
            final Binary binary = (Binary) resource();
            if (!acceptableMediaTypes.isEmpty()) {
                final MediaType mediaType = getBinaryResourceMediaType(resource());

                if (acceptableMediaTypes.stream().noneMatch(t -> t.isCompatible(mediaType))) {
                    return notAcceptable(VariantListBuilder.newInstance().mediaTypes(mediaType).build()).build();
                }
            }

            // Respect the Want-Digest header for fixity check
            final String wantDigest = headers.getHeaderString(WANT_DIGEST);
            if (!isNullOrEmpty(wantDigest)) {
                servletResponse.addHeader(DIGEST, handleWantDigestHeader(binary, wantDigest));
            }

            if (binary.isRedirect()) {
                return temporaryRedirect(binary.getExternalURI()).build();
            } else {
                return getBinaryContent(rangeValue, binary);
            }
        } else {
            return getContent(getChildrenLimit(), resource());
        }
    }

    /**
     * Return the location of a requested Memento.
     *
     * @param datetimeHeader The RFC datetime for the Memento.
     * @param resource The fedora resource
     * @return A 302 Found response or 406 if no mementos.
     */
    private Response getMemento(final String datetimeHeader, final FedoraResource resource) {
        try {
            final Instant mementoDatetime = Instant.from(MEMENTO_RFC_1123_FORMATTER.parse(datetimeHeader));
            final FedoraResource memento = resource.findMementoByDatetime(mementoDatetime);
            final Response builder;
            if (memento != null) {
                builder =
                    status(FOUND).header(LOCATION, getUri(memento)).build();
            } else {
                builder = status(NOT_ACCEPTABLE).build();
            }
            addResourceHttpHeaders(resource);
            setVaryAndPreferenceAppliedHeaders(servletResponse, prefer, resource);
            return builder;
        } catch (final DateTimeParseException e) {
            throw new MementoDatetimeFormatException("Invalid Accept-Datetime value: " + e.getMessage()
                + ". Please use RFC-1123 date-time format, such as 'Tue, 3 Jun 2008 11:05:30 GMT'", e);
        }
    }

    /**
     * Deletes an object.
     *
     * @return response
     */
    @DELETE
    public Response deleteObject() {
        hasRestrictedPath(externalPath);
        if (resource() instanceof Container) {
            final String depth = headers.getHeaderString("Depth");
            LOGGER.debug("Depth header value is: {}", depth);
            if (depth != null && !depth.equalsIgnoreCase("infinity")) {
                throw new ClientErrorException("Depth header, if present, must be set to 'infinity' for containers",
                        SC_BAD_REQUEST);
            }
        }
        if (resource() instanceof NonRdfSourceDescription && resource().isOriginalResource()) {
            LOGGER.debug("Trying to delete binary description directly.");
            throw new ClientErrorException(
                "NonRDFSource descriptions are removed when their associated NonRDFSource object is removed.",
                METHOD_NOT_ALLOWED);
        }

        evaluateRequestPreconditions(request, servletResponse, resource(), transaction());

        LOGGER.info("Delete resource '{}'", externalPath);

        try {
            deleteResourceService.perform(transaction(), resource(), getUserPrincipal());
            transaction().commitIfShortLived();
            return noContent().build();
        } finally {
            transaction().releaseResourceLocksIfShortLived();
        }
    }

    /**
     * Create a resource at a specified path, or replace triples with provided RDF.
     *
     * @param requestContentType the request content type
     * @param requestBodyStream the request body stream
     * @param contentDisposition the content disposition value
     * @param ifMatch the if-match value
     * @param rawLinks the raw link values
     * @param digest the digest header
     * @return 204
     * @throws InvalidChecksumException if invalid checksum exception occurred
     * @throws MalformedRdfException if malformed rdf exception occurred
     * @throws UnsupportedAlgorithmException if an unsupported algorithm exception occurs
     */
    @PUT
    @Consumes
    public Response createOrReplaceObjectRdf(
            @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
            final InputStream requestBodyStream,
            @HeaderParam(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
            @HeaderParam("If-Match") final String ifMatch,
            @HeaderParam(LINK) final List<String> rawLinks,
            @HeaderParam("Digest") final String digest)
            throws InvalidChecksumException, MalformedRdfException, UnsupportedAlgorithmException,
                   PathNotFoundException {
        LOGGER.info("PUT to create resource with ID: {}", externalPath());

        hasRestrictedPath(externalPath);

        final var transaction = transaction();

        try {
            final List<String> links = unpackLinks(rawLinks);

            if (externalPath.contains("/" + FedoraTypes.FCR_VERSIONS)) {
                handleRequestDisallowedOnMemento();

                return status(METHOD_NOT_ALLOWED).build();
            }

            // If request is an external binary, verify link header before proceeding
            final ExternalContent extContent = extContentHandlerFactory.createFromLinks(links);

            final String interactionModel = checkInteractionModel(links);

            final FedoraId fedoraId = identifierConverter().pathToInternalId(externalPath());
            final boolean resourceExists = doesResourceExist(transaction, fedoraId, true);

            if (resourceExists) {

                if (httpConfiguration.putRequiresIfMatch() && StringUtils.isBlank(ifMatch)) {
                    throw new ClientErrorException("An If-Match header is required", 428);
                }

                final String resInteractionModel = resource().getInteractionModel();
                if (StringUtils.isNoneBlank(resInteractionModel, interactionModel) &&
                        !Objects.equals(resInteractionModel, interactionModel)) {
                    throw new InteractionModelViolationException("Changing the interaction model " + resInteractionModel
                            + " to " + interactionModel + " is not allowed!");
                }
                evaluateRequestPreconditions(request, servletResponse, resource(), transaction);
            }

            if (isGhostNode(transaction(), fedoraId)) {
                throw new GhostNodeException("Resource path " + externalPath() + " is an immutable resource.");
            }

            final var providedContentType = getSimpleContentType(requestContentType);

            boolean created = false;

            if ((resourceExists && resource() instanceof Binary) ||
                    (!resourceExists && isBinary(interactionModel,
                            providedContentType,
                            requestBodyStream != null && providedContentType != null,
                            extContent != null))) {
                ensureArchivalGroupHeaderNotPresentForBinaries(links);

                final Collection<URI> checksums = parseDigestHeader(digest);
                final var binaryType = requestContentType != null ? requestContentType : DEFAULT_NON_RDF_CONTENT_TYPE;
                final var contentType = extContent == null ? binaryType.toString() : extContent.getContentType();
                final String originalFileName = contentDisposition != null ? contentDisposition.getFileName() : "";
                final long contentSize = contentDisposition == null ? -1L : contentDisposition.getSize();

                if (resourceExists) {
                    replaceBinariesService.perform(transaction,
                            getUserPrincipal(),
                            fedoraId,
                            originalFileName,
                            contentType,
                            checksums,
                            requestBodyStream,
                            contentSize,
                            extContent);
                } else {
                    createResourceService.perform(transaction,
                            getUserPrincipal(),
                            fedoraId,
                            contentType,
                            originalFileName,
                            contentSize,
                            links,
                            checksums,
                            requestBodyStream,
                            extContent);
                    created = true;
                }
            } else {
                final var contentType = requestContentType != null ? requestContentType : DEFAULT_RDF_CONTENT_TYPE;
                final Model model = httpRdfService.bodyToInternalModel(fedoraId, requestBodyStream,
                        contentType, identifierConverter(), hasLenientPreferHeader());

                if (resourceExists) {
                    replacePropertiesService.perform(transaction,
                            getUserPrincipal(),
                            fedoraId,
                            model);
                } else {
                    createResourceService.perform(transaction, getUserPrincipal(), fedoraId, links, model);
                    created = true;
                }
            }

            // TODO: How to generate a response.
            LOGGER.debug("Finished creating resource with path: {}", externalPath());
            transaction.commitIfShortLived();
            return createUpdateResponse(getFedoraResource(transaction, fedoraId), created);
        } finally {
            transaction.releaseResourceLocksIfShortLived();
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
    public Response updateSparql(final InputStream requestBodyStream)
            throws IOException {
        hasRestrictedPath(externalPath);

        final var transaction = transaction();

        if (externalPath.contains("/" + FedoraTypes.FCR_VERSIONS)) {
            handleRequestDisallowedOnMemento();

            return status(METHOD_NOT_ALLOWED).build();
        }

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        if (resource() instanceof Binary) {
            throw new BadRequestException(resource().getFedoraId().getFullIdPath() +
                    " is not a valid object to receive a PATCH");
        }

        try {
            final String requestBody = IOUtils.toString(requestBodyStream, UTF_8);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), transaction);

            LOGGER.info("PATCH for '{}'", externalPath);
            final String newRequest = httpRdfService.patchRequestToInternalString(resource().getFedoraId(),
                    requestBody, identifierConverter());
            LOGGER.debug("PATCH request translated to '{}'", newRequest);
            patchResourcewithSparql(resource(), newRequest);
            transaction.commitIfShortLived();

            addCacheControlHeaders(servletResponse, reloadResource(), transaction);

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
            transaction.releaseResourceLocksIfShortLived();
        }
    }

    /**
     * Creates a new object.
     *
     * This originally used application/octet-stream;qs=1001 as a workaround
     * for JERSEY-2636, to ensure requests without a Content-Type get routed here.
     * This qs value does not parse with newer versions of Jersey, as qs values
     * must be between 0 and 1. We use qs=1.000 to mark where this historical
     * anomaly had been.
     *
     * @param contentDisposition the content Disposition value
     * @param requestContentType the request content type
     * @param slug the slug value
     * @param requestBodyStream the request body stream
     * @param rawLinks the link values
     * @param digest the digest header
     * @return 201
     * @throws InvalidChecksumException if invalid checksum exception occurred
     * @throws MalformedRdfException if malformed rdf exception occurred
     * @throws UnsupportedAlgorithmException if an unsupported algorithm exception occurs
     */
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM + ";qs=1.000", WILDCARD})
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
            TURTLE_X, TEXT_HTML_WITH_CHARSET, "*/*"})
    public Response createObject(@HeaderParam(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
                                 @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
                                 @HeaderParam("Slug") final String slug,
                                 final InputStream requestBodyStream,
                                 @HeaderParam(LINK) final List<String> rawLinks,
                                 @HeaderParam("Digest") final String digest)
            throws InvalidChecksumException, MalformedRdfException, UnsupportedAlgorithmException {

        final var transaction = transaction();

        try {
            final List<String> links = unpackLinks(rawLinks);

            if (externalPath.contains("/" + FedoraTypes.FCR_VERSIONS)) {
                handleRequestDisallowedOnMemento();

                return status(METHOD_NOT_ALLOWED).build();
            }

            // If request is an external binary, verify link header before proceeding
            final ExternalContent extContent = extContentHandlerFactory.createFromLinks(links);

            final String interactionModel = checkInteractionModel(links);

            final FedoraId fedoraId = identifierConverter().pathToInternalId(externalPath());
            final FedoraId newFedoraId = mintNewPid(fedoraId, slug);
            final var providedContentType = getSimpleContentType(requestContentType);

            LOGGER.info("POST to create resource with ID: {}, slug: {}", newFedoraId.getFullIdPath(), slug);

            if (isBinary(interactionModel,
                    providedContentType,
                    requestBodyStream != null && providedContentType != null,
                    extContent != null)) {
                ensureArchivalGroupHeaderNotPresentForBinaries(links);

                final Collection<URI> checksums = parseDigestHeader(digest);
                final String originalFileName = contentDisposition != null ? contentDisposition.getFileName() : "";
                final var binaryType = requestContentType != null ? requestContentType : DEFAULT_NON_RDF_CONTENT_TYPE;
                final var contentType = extContent == null ? binaryType.toString() : extContent.getContentType();
                final long contentSize = contentDisposition == null ? -1L : contentDisposition.getSize();

                createResourceService.perform(transaction,
                        getUserPrincipal(),
                        newFedoraId,
                        contentType,
                        originalFileName,
                        contentSize,
                        links,
                        checksums,
                        requestBodyStream,
                        extContent);
            } else {
                final var contentType = requestContentType != null ? requestContentType : DEFAULT_RDF_CONTENT_TYPE;
                final Model model = httpRdfService.bodyToInternalModel(newFedoraId, requestBodyStream,
                        contentType, identifierConverter(), hasLenientPreferHeader());
                createResourceService.perform(transaction,
                        getUserPrincipal(),
                        newFedoraId,
                        links,
                        model);
            }
            LOGGER.debug("Finished creating resource with path: {}", externalPath());
            transaction.commitIfShortLived();
            try {
                final var resource = getFedoraResource(transaction, newFedoraId);
                return createUpdateResponse(resource, true);
            } catch (final PathNotFoundException e) {
                throw new PathNotFoundRuntimeException(e.getMessage(), e);
            }
        } finally {
            transaction.releaseResourceLocksIfShortLived();
        }
    }

    @Override
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        super.addResourceHttpHeaders(resource);

        if (!transaction().isShortLived()) {
            final String canonical = identifierConverter().toExternalId(resource.getFedoraId().getFullId())
                    .replaceFirst("/tx:[^/]+", "");

            servletResponse.addHeader(LINK, "<" + canonical + ">;rel=\"canonical\"");

        }
        addExternalContentHeaders(resource);
        addTransactionHeaders(resource);
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }

    /**
     * Determine based on several factors whether the interaction model should be ldp:NonRdfSource
     * @param interactionModel the interaction model from the links.
     * @param contentType the content type.
     * @param contentPresent is there a request body.
     * @param contentExternal is there an external content header.
     * @return Use ldp:NonRdfSource as the interaction model.
     */
    private boolean isBinary(final String interactionModel, final String contentType,
                             final boolean contentPresent, final boolean contentExternal) {
        final String simpleContentType = contentPresent ? contentType : null;
        final boolean isRdfContent = isRdfContentType(simpleContentType);
        return NON_RDF_SOURCE.getURI().equals(interactionModel) || contentExternal ||
                (contentPresent && interactionModel == null && !isRdfContent);
    }

    private String handleWantDigestHeader(final Binary binary, final String wantDigest)
            throws UnsupportedAlgorithmException {
        // handle the Want-Digest header with fixity check
        final Collection<String> preferredDigests = parseWantDigestHeader(wantDigest);
        if (preferredDigests.isEmpty()) {
            throw new UnsupportedAlgorithmException(
                    "Unsupported digest algorithm provided in 'Want-Digest' header: " + wantDigest);
        }

        final Collection<URI> checksumResults = fixityService.getFixity(binary, preferredDigests);
        return checksumResults.stream().map(uri -> uri.toString().replaceFirst("urn:", "")
                .replaceFirst(":", "=").replaceFirst("sha1=", "sha=")).collect(Collectors.joining(","));
    }

    private static void ensureArchivalGroupHeaderNotPresentForBinaries(final List<String> links) {
        if (links == null) {
            return;
        }

        if (links.stream().map(Link::valueOf)
                      .filter(l -> l.getUri().toString().equals(ARCHIVAL_GROUP.getURI()))
                      .anyMatch(l -> l.getRel().equals("type"))) {
            throw new ClientErrorException("Binary resources cannot be created as an" +
                    " ArchiveGroup. Please remove the ArchiveGroup link header and try again", BAD_REQUEST);
        }
    }

    private static String checkInteractionModel(final List<String> links) {
        if (links == null) {
            return null;
        }

        try {
            for (final String link : links) {
                final Link linq = Link.valueOf(link);
                if ("type".equals(linq.getRel())) {
                    //skip ArchivalGroup types
                    if (linq.getUri().toString().equals(ARCHIVAL_GROUP.getURI())) {
                        continue;
                    }
                    final Resource type = createResource(linq.getUri().toString());
                    if (INTERACTION_MODEL_RESOURCES.contains(type)) {
                        return type.getURI();
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
            if (e instanceof IllegalArgumentException || e instanceof UriBuilderException) {
                throw new ClientErrorException("Invalid link specified: " + String.join(", ", links), BAD_REQUEST);
            }
            throw e;
        }

        return null;
    }

    /**
     * Parse the RFC-3230 Want-Digest header value.
     * @param wantDigest The Want-Digest header value with optional q value in format:
     *    'md5', 'md5, sha', 'MD5;q=0.3, sha;q=1' etc.
     * @return Digest algorithms that are supported
     */
    private static Collection<String> parseWantDigestHeader(final String wantDigest) {
        final Map<String, Double> digestPairs = new HashMap<>();
        try {
            final List<String> algs = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(wantDigest);
            // Parse the optional q value with default 1.0, and 0 ignore. Format could be: SHA-1;qvalue=0.1
            for (final String alg : algs) {
                final String[] tokens = alg.split(";", 2);
                final double qValue = tokens.length == 1 || !tokens[1].contains("=") ?
                        1.0 : Double.parseDouble(tokens[1].split("=", 2)[1]);
                digestPairs.put(tokens[0], qValue);
            }

            return digestPairs.entrySet().stream().filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .filter(ContentDigest.DIGEST_ALGORITHM::isSupportedAlgorithm)
                    .collect(Collectors.toSet());
        } catch (final NumberFormatException e) {
            throw new ClientErrorException("Invalid 'Want-Digest' header value: " + wantDigest, SC_BAD_REQUEST, e);
        } catch (final RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw new ClientErrorException("Invalid 'Want-Digest' header value: " + wantDigest + "\n", BAD_REQUEST);
            }
            throw e;
        }
    }

    private void handleRequestDisallowedOnMemento() {
        try {
            addLinkAndOptionsHttpHeaders(resource());
        } catch (final Exception ex) {
            // Catch the exception to ensure status 405 for any requests on memento.
            LOGGER.debug("Unable to add link and options headers for PATCH request to memento path {}: {}.",
                externalPath, ex.getMessage());
        }

        LOGGER.info("Unable to handle {} request on a path containing {}. Path was: {}", request.getMethod(),
            FedoraTypes.FCR_VERSIONS, externalPath);
    }

    private FedoraId mintNewPid(final FedoraId fedoraId, final String slug) {
        final String pid;

        if (isGhostNode(transaction(), fedoraId)) {
            LOGGER.debug("Resource with path {} is an immutable resource; it cannot be POSTed to.", fedoraId);
            throw new CannotCreateResourceException("Cannot create resource as child of the immutable resource at " +
                    fedoraId.getFullIdPath());
        }
        if (!isBlank(slug)) {
            pid = slug;
        } else if (pidMinter != null) {
            pid = pidMinter.get();
        } else {
            pid = defaultPidMinter.get();
        }

        final FedoraId fullTestPath = fedoraId.resolve(pid);
        hasRestrictedPath(fullTestPath.getFullIdPath());

        if (doesResourceExist(transaction(), fullTestPath, true) || isGhostNode(transaction(), fullTestPath)) {
            LOGGER.debug("Resource with path {} already exists or is an immutable resource; minting new path instead",
                    fullTestPath);
            return mintNewPid(fedoraId, null);
        }

        return fullTestPath;
    }

}
