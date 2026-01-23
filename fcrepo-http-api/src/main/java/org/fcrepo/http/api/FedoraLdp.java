/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.Response.noContent;
import static jakarta.ws.rs.core.Response.notAcceptable;
import static jakarta.ws.rs.core.Response.ok;
import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.temporaryRedirect;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FOUND;
import static jakarta.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
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
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_TYPE;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_OCTET_STREAM_TYPE;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODEL_RESOURCES;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_RFC_1123_FORMATTER;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilderException;
import jakarta.ws.rs.core.Variant.VariantListBuilder;

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
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.services.FixityService;
import org.fcrepo.kernel.api.services.ReplaceBinariesService;
import org.fcrepo.config.DigestAlgorithm;

import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ContentDisposition;

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

    /**
     * List of RDF_TYPES for comparison, text/plain isn't really an RDF type but it is still accepted.
     */
    private static final List<MediaType> RDF_TYPES = Stream.of(TURTLE_WITH_CHARSET, JSON_LD,
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET
    ).map(MediaType::valueOf).collect(Collectors.toList());

    /**
     * This predicate allows comparing a list of accept headers to a list of RDF types.
     * It is needed to account for charset variations.
     */
    private static final Predicate<List<MediaType>> IS_RDF_TYPE = t -> {
        assert t != null;
        return t.stream()
                .anyMatch(c -> RDF_TYPES.stream().anyMatch(c::isCompatible));
    };

    /**
     * This predicate checks if the list does not have a mediatype that is wildcard
     */
    private static final Predicate<List<MediaType>> NOT_WILDCARD = t -> {
        assert t != null;
        return t.stream().noneMatch(MediaType::isWildcardType);
    };

    /**
     * This predicate checks if the list does not have a mediatype that is compatible with text html
     */
    private static final Predicate<List<MediaType>> NOT_HTML =
            t -> t.stream().noneMatch(TEXT_HTML_TYPE::isCompatible);

    private static final VariantListBuilder RDF_VARIANT_BUILDER = VariantListBuilder.newInstance();
    static {
        RDF_TYPES.forEach(t -> RDF_VARIANT_BUILDER.mediaTypes(t).add());
    }

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
     * @param inlineDisposition whether to return a Content-Disposition inline header for a binary
     * @return response
     * @throws UnsupportedAlgorithmException if unsupported digest algorithm occurred
     */
    @HEAD
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
            TEXT_HTML_WITH_CHARSET, "*/*"})
    public Response head(@DefaultValue("false") @QueryParam("inline") final boolean inlineDisposition)
            throws UnsupportedAlgorithmException {
        LOGGER.info("HEAD for: {}", externalPath);

        final String datetimeHeader = headers.getHeaderString(ACCEPT_DATETIME);
        if (!isBlank(datetimeHeader) && resource(true).isOriginalResource()) {
            return getMemento(datetimeHeader, resource(true), inlineDisposition);
        }

        final ImmutableList<MediaType> acceptableMediaTypes = ImmutableList.copyOf(headers
                .getAcceptableMediaTypes());

        final var fedoraResource = resource();

        checkCacheControlHeaders(request, servletResponse, fedoraResource, transaction());

        addResourceHttpHeaders(fedoraResource, inlineDisposition);

        Response.ResponseBuilder builder = ok();

        if (fedoraResource instanceof Binary binary) {
            final MediaType mediaType = getBinaryResourceMediaType(binary);

            // Content negotiation guard clause (do not touch servletResponse/resource-derived side effects on 406 path)
            if (!acceptableMediaTypes.isEmpty()
                    && acceptableMediaTypes.stream().noneMatch(t -> t.isCompatible(mediaType))) {
                return notAcceptable(
                        VariantListBuilder.newInstance()
                                .mediaTypes(mediaType)
                                .build()
                ).build();
            }

            if (binary.isRedirect()) {
                builder = temporaryRedirect(binary.getExternalURI());
            }

            // We set the content-type explicitly to avoid content-negotiation from getting in the way
            builder.type(mediaType.toString());

            // Respect the Want-Digest header with fixity check
            final String wantDigest = headers.getHeaderString(WANT_DIGEST);
            if (!isNullOrEmpty(wantDigest)) {
                builder.header(DIGEST, handleWantDigestHeader(binary, wantDigest));
            }

            setVaryAndPreferenceAppliedHeaders(servletResponse, prefer, fedoraResource);
            return builder.build();
        }

        // RDF / container-ish resources
        if (!acceptableMediaTypes.isEmpty()
                && NOT_WILDCARD.test(acceptableMediaTypes)
                && !IS_RDF_TYPE.test(acceptableMediaTypes)) {
            return notAcceptable(VariantListBuilder.newInstance().mediaTypes().build()).build();
        }

        // If there is no Accept header or it is */*, default to text/turtle
        if (acceptableMediaTypes.isEmpty() || !NOT_WILDCARD.test(acceptableMediaTypes)) {
            builder.type(TURTLE_WITH_CHARSET);
        }

        setVaryAndPreferenceAppliedHeaders(servletResponse, prefer, fedoraResource);
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
     * @param inlineDisposition whether to return a Content-Disposition inline header for a binary
     * @return a binary or the triples for the specified node
     * @throws IOException if IO exception occurred
     * @throws UnsupportedAlgorithmException if unsupported digest algorithm occurred
     */
    @GET
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
            TEXT_HTML_WITH_CHARSET, "*/*"})
    public Response getResource(
            @HeaderParam("Range") final String rangeValue,
            @DefaultValue("false") @QueryParam("inline") final boolean inlineDisposition)
            throws IOException, UnsupportedAlgorithmException {

        final String datetimeHeader = headers.getHeaderString(ACCEPT_DATETIME);
        if (!isBlank(datetimeHeader) && resource(true).isOriginalResource()) {
            return getMemento(datetimeHeader, resource(true), inlineDisposition);
        }

        final ImmutableList<MediaType> acceptableMediaTypes = ImmutableList.copyOf(headers
                .getAcceptableMediaTypes());

        final var fedoraResource = resource();

        if (fedoraResource instanceof Binary binary) {
            final MediaType mediaType = getBinaryResourceMediaType(binary);

            if (!acceptableMediaTypes.isEmpty()
                    && acceptableMediaTypes.stream().noneMatch(t -> t.isCompatible(mediaType))) {
                return notAcceptable(
                        VariantListBuilder.newInstance()
                                .mediaTypes(mediaType)
                                .build()
                ).build();
            }
        } else {
            if (!acceptableMediaTypes.isEmpty()
                    && NOT_WILDCARD.test(acceptableMediaTypes)
                    && NOT_HTML.test(acceptableMediaTypes)
                    && !IS_RDF_TYPE.test(acceptableMediaTypes)) {
                // Accept header is not empty and is not */* and is not text/html and is not a valid RDF type.
                return notAcceptable(RDF_VARIANT_BUILDER.build()).build();
            }
        }

        checkCacheControlHeaders(request, servletResponse, fedoraResource, transaction());

        LOGGER.info("GET resource '{}'", externalPath);
        addResourceHttpHeaders(fedoraResource, inlineDisposition);

        if (fedoraResource instanceof Binary binary) {
            // Respect the Want-Digest header for fixity check
            final String wantDigest = headers.getHeaderString(WANT_DIGEST);
            if (!isNullOrEmpty(wantDigest)) {
                servletResponse.addHeader(DIGEST, handleWantDigestHeader(binary, wantDigest));
            }

            if (binary.isRedirect()) {
                return temporaryRedirect(binary.getExternalURI()).build();
            }

            return getBinaryContent(rangeValue, binary);
        }

        return getContent(getChildrenLimit(), fedoraResource);
    }

    /**
     * Return the location of a requested Memento.
     *
     * @param datetimeHeader The RFC datetime for the Memento.
     * @param resource The fedora resource
     * @param inlineDisposition whether to return binary as Content-Disposition inline
     * @return A 302 Found response or 406 if no mementos.
     */
    private Response getMemento(final String datetimeHeader, final FedoraResource resource,
                                final boolean inlineDisposition) {
        try {
            final Instant mementoDatetime = Instant.from(MEMENTO_RFC_1123_FORMATTER.parse(datetimeHeader));
            final FedoraResource memento = resource.findMementoByDatetime(mementoDatetime);
            final Response builder;
            boolean isRedirect = false;
            if (memento != null) {
                isRedirect = true;
                builder = status(FOUND).header(LOCATION, getUri(memento)).build();
            } else {
                builder = status(NOT_ACCEPTABLE).build();
            }
            addResourceHttpHeaders(resource, inlineDisposition, isRedirect);
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
        LOGGER.info("Delete resource '{}'", externalPath);
        if (externalPath.contains("/" + FedoraTypes.FCR_VERSIONS)) {
            handleRequestDisallowedOnMemento();

            return status(METHOD_NOT_ALLOWED).build();
        }

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

        try {
            evaluateRequestPreconditions(request, servletResponse, resource(), transaction());

            doInDbTxWithRetry(() -> {
                deleteResourceService.perform(transaction(), resource(), getUserPrincipal());
                transaction().commitIfShortLived();
            });
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
     * @param contentDispositionRaw the content disposition value
     * @param ifMatch the if-match value
     * @param rawLinks the raw link values
     * @param digest the digest header
     * @param overwriteTombstoneRaw the Overwrite-Tombstone header
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
            @HeaderParam(CONTENT_DISPOSITION) final String contentDispositionRaw,
            @HeaderParam("If-Match") final String ifMatch,
            @HeaderParam(LINK) final List<String> rawLinks,
            @HeaderParam("Digest") final String digest,
            @HeaderParam(HTTP_HEADER_OVERWRITE_TOMBSTONE) final String overwriteTombstoneRaw)
            throws InvalidChecksumException, MalformedRdfException, UnsupportedAlgorithmException,
            PathNotFoundException {
        LOGGER.info("PUT to create resource with ID: {}", externalPath());

        final var overwriteTombstone = Boolean.parseBoolean(overwriteTombstoneRaw);
        if (externalPath.contains("/" + FedoraTypes.FCR_VERSIONS)) {
            handleRequestDisallowedOnMemento();

            return status(METHOD_NOT_ALLOWED).build();
        }

        hasRestrictedPath(externalPath);

        final var transaction = transaction();

        try {
            final List<String> links = unpackLinks(rawLinks);

            // If request is an external binary, verify link header before proceeding
            final ExternalContent extContent = extContentHandlerFactory.createFromLinks(links);

            final String interactionModel = checkInteractionModel(links);

            FedoraResource resource = null;
            final var isTombstoneOverwrite = new AtomicBoolean(false);
            final FedoraId fedoraId = identifierConverter().pathToInternalId(externalPath());
            final boolean resourceExists = doesResourceExist(transaction, fedoraId, true);

            if (resourceExists) {

                if (httpConfiguration.putRequiresIfMatch() && StringUtils.isBlank(ifMatch)) {
                    throw new ClientErrorException("An If-Match header is required", 428);
                }

                resource = resource(overwriteTombstone);
                if (resource instanceof Tombstone) {
                    isTombstoneOverwrite.set(true);
                    resource = ((Tombstone) resource).getDeletedObject();
                }

                final String resInteractionModel = resource.getInteractionModel();
                if (StringUtils.isNoneBlank(resInteractionModel, interactionModel) &&
                        !Objects.equals(resInteractionModel, interactionModel)) {
                    throw new InteractionModelViolationException("Changing the interaction model " + resInteractionModel
                            + " to " + interactionModel + " is not allowed!");
                }
                evaluateRequestPreconditions(request, servletResponse, resource, transaction);
            }

            if (isGhostNode(transaction(), fedoraId)) {
                throw new GhostNodeException("Resource path " + externalPath() + " is an immutable resource.");
            }

            if (!resourceExists && fedoraId.isDescription()) {
                // Can't PUT a description to a non-existent binary.
                final String message;
                if (fedoraId.asBaseId().isRepositoryRoot()) {
                    message = "The root of the repository is not a binary, so /" + FCR_METADATA + " does not exist.";
                } else {
                    message = "Binary at path " + fedoraId.asBaseId().getFullIdPath() + " not found";
                }
                throw new PathNotFoundException(message);
            }

            final var providedContentType = getSimpleContentType(requestContentType);
            final var created = new AtomicBoolean(false);

            if ((resourceExists && resource instanceof Binary) ||
                    (!resourceExists && isBinary(interactionModel,
                            providedContentType,
                            requestBodyStream != null && providedContentType != null,
                            extContent != null))) {
                ensureArchivalGroupHeaderNotPresentForBinaries(links);

                final Collection<URI> checksums = parseDigestHeader(digest);
                final var binaryType = requestContentType != null ?
                        requestContentType : DEFAULT_NON_RDF_CONTENT_TYPE;
                final var contentType = extContent == null ?
                        binaryType.toString() : extContent.getContentType();

                final String originalFileName;
                final long contentSize;

                if (StringUtils.isNotBlank(contentDispositionRaw)) {
                    final var contentDisposition = ContentDisposition.parse(contentDispositionRaw);
                    originalFileName = contentDisposition.getFilename();
                    contentSize = contentDisposition.getSize() == null ? -1L : contentDisposition.getSize();
                } else {
                    originalFileName = "";
                    contentSize = -1L;
                }

                doInDbTx(() -> {
                    if (resourceExists && !(resource() instanceof Tombstone)) {
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
                        created.set(true);
                    }
                    transaction.commitIfShortLived();
                });
            } else {
                final var contentType = requestContentType != null ? requestContentType : DEFAULT_RDF_CONTENT_TYPE;
                final Model model = httpRdfService.bodyToInternalModel(fedoraId, requestBodyStream,
                        contentType, identifierConverter(), hasLenientPreferHeader());

                doInDbTxWithRetry(() -> {
                    if (resourceExists && !(resource() instanceof Tombstone)) {
                        replacePropertiesService.perform(transaction, getUserPrincipal(), fedoraId, model);
                    } else {
                        createResourceService.perform(transaction, getUserPrincipal(), fedoraId, links, model,
                                isTombstoneOverwrite.get());
                        created.set(true);
                    }
                    transaction.commitIfShortLived();
                });
            }

            LOGGER.debug("Finished creating resource with path: {}", externalPath());
            return createUpdateResponse(getFedoraResource(transaction, fedoraId), created.get());
        } finally {
            transaction.releaseResourceLocksIfShortLived();
            IOUtils.closeQuietly(requestBodyStream);
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
        if (externalPath.contains("/" + FedoraTypes.FCR_VERSIONS)) {
            handleRequestDisallowedOnMemento();

            return status(METHOD_NOT_ALLOWED).build();
        }

        hasRestrictedPath(externalPath);

        if (null == requestBodyStream) {
            throw new BadRequestException("SPARQL-UPDATE requests must have content!");
        }

        if (resource() instanceof Binary) {
            throw new BadRequestException(resource().getFedoraId().getFullIdPath() +
                    " is not a valid object to receive a PATCH");
        }

        final var transaction = transaction();

        try {
            //final String requestBody = IOUtils.toString(requestBodyStream, UTF_8);
            // TODO: use a streaming parser instead of reading the entire body into memory,
            // but IOUtils hangs since the Jersey change.
            final byte[] body = requestBodyStream.readAllBytes();
            final String requestBody = new String(body);
            if (isBlank(requestBody)) {
                throw new BadRequestException("SPARQL-UPDATE requests must have content!");
            }

            evaluateRequestPreconditions(request, servletResponse, resource(), transaction);

            LOGGER.info("PATCH for '{}'", externalPath);
            final String newRequest = httpRdfService.patchRequestToInternalString(resource().getFedoraId(),
                    requestBody, identifierConverter());

            LOGGER.debug("PATCH request translated to '{}'", newRequest);

            doInDbTxWithRetry(() -> {
                patchResourcewithSparql(resource(), newRequest);
                transaction.commitIfShortLived();
            });

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
     * @param contentDispositionRaw the content Disposition value
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
            TEXT_HTML_WITH_CHARSET, "*/*"})
    public Response createObject(@HeaderParam(CONTENT_DISPOSITION) final String contentDispositionRaw,
                                 @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
                                 @HeaderParam("Slug") final String slug,
                                 final InputStream requestBodyStream,
                                 @HeaderParam(LINK) final List<String> rawLinks,
                                 @HeaderParam("Digest") final String digest)
            throws InvalidChecksumException, MalformedRdfException, UnsupportedAlgorithmException {

        final var decodedSlug = slug != null ? URLDecoder.decode(slug, UTF_8) : null;
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
            // If the resource doesn't exist and it's not a ghost node, throw an exception.
            // Ghost node checking is done further down in the code and returns a 400 Bad Request error.
            if (!doesResourceExist(transaction, fedoraId, false) && !isGhostNode(transaction, fedoraId)) {
                throw new PathNotFoundRuntimeException(String.format("Path %s not found", fedoraId.getFullIdPath()));
            }
            final FedoraId newFedoraId = mintNewPid(fedoraId, decodedSlug);
            final var providedContentType = getSimpleContentType(requestContentType);

            LOGGER.info("POST to create resource with ID: {}, slug: {}", newFedoraId.getFullIdPath(), decodedSlug);

            if (isBinary(interactionModel,
                    providedContentType,
                    requestBodyStream != null && providedContentType != null,
                    extContent != null)) {
                ensureArchivalGroupHeaderNotPresentForBinaries(links);

                final Collection<URI> checksums = parseDigestHeader(digest);

                final String originalFileName;
                final long contentSize;

                if (StringUtils.isNotBlank(contentDispositionRaw)) {
                    final var contentDisposition = ContentDisposition.parse(contentDispositionRaw);
                    originalFileName = contentDisposition.getFilename();
                    contentSize = contentDisposition.getSize() == null ? -1L : contentDisposition.getSize();
                } else {
                    originalFileName = "";
                    contentSize = -1L;
                }

                final var binaryType = requestContentType != null ?
                        requestContentType : DEFAULT_NON_RDF_CONTENT_TYPE;
                final var contentType = extContent == null ? binaryType.toString() : extContent.getContentType();

                doInDbTx(() -> {
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

                    transaction.commitIfShortLived();
                });
            } else {
                final var contentType = requestContentType != null ? requestContentType : DEFAULT_RDF_CONTENT_TYPE;
                final Model model = httpRdfService.bodyToInternalModel(newFedoraId, requestBodyStream,
                        contentType, identifierConverter(), hasLenientPreferHeader());

                doInDbTxWithRetry(() -> {
                    createResourceService.perform(transaction,
                            getUserPrincipal(),
                            newFedoraId,
                            links,
                            model);

                    transaction.commitIfShortLived();
                });
            }

            LOGGER.debug("Finished creating resource with path: {}", externalPath());

            try {
                final var resource = getFedoraResource(transaction, newFedoraId);
                return createUpdateResponse(resource, true);
            } catch (final PathNotFoundException e) {
                throw new PathNotFoundRuntimeException(e.getMessage(), e);
            }
        } finally {
            transaction.releaseResourceLocksIfShortLived();
            IOUtils.closeQuietly(requestBodyStream);
        }
    }

    @Override
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        addResourceHttpHeaders(resource, false);
    }

    @Override
    protected void addResourceHttpHeaders(final FedoraResource resource, final boolean dispositionInline) {
        addResourceHttpHeaders(resource, dispositionInline, false);
    }

    protected void addResourceHttpHeaders(final FedoraResource resource,
                                          final boolean dispositionInline,
                                          final boolean isRedirect) {
        super.addResourceHttpHeaders(resource, dispositionInline, isRedirect);

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
                    .filter(DigestAlgorithm::isSupportedAlgorithm)
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
