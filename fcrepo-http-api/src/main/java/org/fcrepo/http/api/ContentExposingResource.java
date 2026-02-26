/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.ctSPARQLUpdate;
import static org.apache.jena.riot.WebContent.ctTextCSV;
import static org.apache.jena.riot.WebContent.ctTextPlain;
import static org.apache.jena.riot.WebContent.matchContentType;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_ENDPOINT_REL;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_PREFIX;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.models.ExternalContent.COPY;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.fcrepo.kernel.api.models.ExternalContent.REDIRECT;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_RFC_1123_FORMATTER;
import static org.slf4j.LoggerFactory.getLogger;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static jakarta.ws.rs.core.Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
import static jakarta.ws.rs.core.Response.created;
import static jakarta.ws.rs.core.Response.noContent;
import static jakarta.ws.rs.core.Response.notAcceptable;
import static jakarta.ws.rs.core.Response.ok;
import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Variant.mediaTypes;
import static java.net.URI.create;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.empty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import org.fcrepo.config.DigestAlgorithm;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.http.api.services.EtagService;
import org.fcrepo.http.api.services.HttpRdfService;
import org.fcrepo.http.commons.api.rdf.HttpTripleUtil;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.PreconditionException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;
import org.fcrepo.kernel.api.services.CreateResourceService;
import org.fcrepo.kernel.api.services.DeleteResourceService;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.kernel.api.services.ResourceTripleService;
import org.fcrepo.kernel.api.services.UpdatePropertiesService;
import org.fcrepo.kernel.api.utils.ContentDigest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.jvnet.hk2.annotations.Optional;
import org.slf4j.Logger;
import org.springframework.http.ContentDisposition;

/**
 * An abstract class that sits between AbstractResource and any resource that
 * wishes to share the routines for building responses containing binary
 * content.
 *
 * @author Mike Durbin
 * @author ajs6f
 */
public abstract class ContentExposingResource extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(ContentExposingResource.class);

    private static final List<String> VARY_HEADERS = Arrays.asList("Accept", "Range", "Accept-Encoding",
            "Accept-Language");

    static final String INSUFFICIENT_SPACE_IDENTIFYING_MESSAGE = "No space left on device";

    public static final String ACCEPT_DATETIME = "Accept-Datetime";

    static final String ACCEPT_EXTERNAL_CONTENT = "Accept-External-Content-Handling";

    static final String HTTP_HEADER_ACCEPT_PATCH = "Accept-Patch";

    public static final String HTTP_HEADER_OVERWRITE_TOMBSTONE = "Overwrite-Tombstone";

    private static final String HTTP_OCFL_PATH = "Fedora-Ocfl-Path";

    private static final String FCR_PREFIX = "fcr:";
    private static final Set<String> ALLOWED_FCR_PARTS = Set.of(FCR_METADATA, FCR_ACL);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected ServletContext context;

    @Inject
    @Optional
    private HttpTripleUtil httpTripleUtil;

    @BeanParam
    protected MultiPrefer prefer;

    private FedoraResource fedoraResource;

    @Inject
    protected ExternalContentHandlerFactory extContentHandlerFactory;

    @Inject
    protected RdfNamespaceRegistry namespaceRegistry;

    @Inject
    protected CreateResourceService createResourceService;

    @Inject
    protected DeleteResourceService deleteResourceService;

    @Inject
    protected ReplacePropertiesService replacePropertiesService;

    @Inject
    protected UpdatePropertiesService updatePropertiesService;

    @Inject
    protected EtagService etagService;

    @Inject
    protected HttpRdfService httpRdfService;

    @Inject
    protected ResourceTripleService resourceTripleService;

    @Inject
    protected OcflPropsConfig ocflPropsConfig;

    protected abstract String externalPath();

    protected static final Splitter.MapSplitter RFC3230_SPLITTER =
        Splitter.on(',').omitEmptyStrings().trimResults().withKeyValueSeparator(Splitter.on('=').limit(2));


    /**
     * This method returns an HTTP response with content body appropriate to the following arguments.
     *
     * @param limit is the number of child resources returned in the response, -1 for all
     * @param resource the fedora resource
     * @return HTTP response
     * @throws IOException in case of error extracting content
     */
    protected Response getContent(final int limit, final FedoraResource resource) throws IOException {
        final RdfStream rdfStream = httpRdfService.bodyToExternalStream(getUri(resource).toString(),
                getResourceTriples(limit, resource), identifierConverter());
        final var outputStream = new RdfNamespacedStream(
                    rdfStream, namespaceRegistry.getNamespaces());
        setVaryAndPreferenceAppliedHeaders(servletResponse, prefer, resource);
        return ok(outputStream).build();
    }

    protected void setVaryAndPreferenceAppliedHeaders(final HttpServletResponse servletResponse,
            final MultiPrefer prefer, final FedoraResource resource) {
        if (prefer != null) {
            prefer.getReturn().addResponseHeaders(servletResponse);
        }

        // add vary headers
        final List<String> varyValues = new ArrayList<>(VARY_HEADERS);

        if (resource.isOriginalResource()) {
            varyValues.add(ACCEPT_DATETIME);
        }

        varyValues.forEach(x -> servletResponse.addHeader("Vary", x));
    }

    /**
     * Utility to check if the Prefer header contains handling="lenient"
     * @return True if handling="lenient" was sent.
     */
    protected boolean hasLenientPreferHeader() {
        return (prefer.hasHandling() && prefer.getHandling().getValue().equals("lenient"));
    }

    protected RdfStream getResourceTriples(final FedoraResource resource) {
        return getResourceTriples(-1, resource);
    }

    /**
     * This method returns a stream of RDF triples associated with this target resource
     *
     * @param limit is the number of child resources returned in the response, -1 for all
     * @param resource the fedora resource
     * @return {@link RdfStream}
     */
    private RdfStream getResourceTriples(final int limit, final FedoraResource resource) {
        final LdpPreferTag ldpPreferences = getLdpPreferTag();

        final List<Stream<Triple>> embedStreams = new ArrayList<>();

        try {
            embedStreams.add(resourceTripleService.getResourceTriples(
                    transaction(), resource, ldpPreferences, limit));
        } catch (ItemNotFoundException e) {
            if (resource instanceof Tombstone && ((Tombstone) resource).getDeletedObject().isMemento()) {
                // There is a version created when the object is deleted.
                // This memento has no content file so an ItemNotFound is thrown. Generate a new 410 Gone response.
                final var orig_resource = ((Tombstone) resource).getDeletedObject();
                final var timeMap = identifierConverter().toExternalId(
                        orig_resource.getFedoraId().asTimemap().toString()
                );
                final var tombstone = identifierConverter().toExternalId(
                        resource.getFedoraId().asTombstone().toString()
                );
                throw new TombstoneException(orig_resource, tombstone, timeMap);
            }
            throw e;
        }

        // Embed the children of this object
        if (ldpPreferences.displayEmbed()) {
            final var containedResources = resourceFactory.getChildren(
                    transaction(),
                    resource.getFedoraId());
            embedStreams.add(containedResources.flatMap(child -> resourceTripleService.getResourceTriples(
                    transaction(), child, ldpPreferences, limit)));
        }

        final var rdfStream = new DefaultRdfStream(
                asNode(resource),
                embedStreams.stream().reduce(empty(), Stream::concat)
        );

        if (httpTripleUtil != null && ldpPreferences.displayServerManaged()) {
            // Adds fixity service triple to all resources and transaction triple to repo root.
            return httpTripleUtil.addHttpComponentModelsForResourceToStream(rdfStream, resource, uriInfo);
        }

        return rdfStream;
    }

    private LdpPreferTag getLdpPreferTag() {
        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else if (prefer != null && prefer.hasHandling()) {
            returnPreference = prefer.getHandling();
        } else {
            returnPreference = PreferTag.emptyTag();
        }

        return new LdpPreferTag(returnPreference);
    }

    /**
     * Get the binary content of a datastream
     *
     * @param rangeValue the range value
     * @param resource the fedora resource
     * @return Binary blob
     * @throws IOException if io exception occurred
     */
    protected Response getBinaryContent(final String rangeValue, final FedoraResource resource)
            throws IOException {
            final Binary binary = (Binary)resource;
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            final Response.ResponseBuilder builder;

            if (rangeValue != null && rangeValue.startsWith("bytes")) {

                final Range range = Range.convert(rangeValue);

                final long contentSize = binary.getContentSize();

                final var rangeOfLength = range.rangeOfLength(contentSize);

                final String contentRangeValue =
                        String.format("bytes %s-%s/%s", rangeOfLength.startAsString(),
                                rangeOfLength.endAsString(), contentSize);

                if (
                    !rangeOfLength.isSatisfiable()
                ) {

                    builder = status(REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header("Content-Range", contentRangeValue);
                } else {
                    final var rangeContent = binary.getRange(rangeOfLength.start(), rangeOfLength.end());

                    builder = status(PARTIAL_CONTENT).entity(rangeContent)
                            .header("Content-Range", contentRangeValue)
                            .header(CONTENT_LENGTH, rangeOfLength.size());
                }

            } else {
                final InputStream content = binary.getContent();
                builder = ok(content);
            }


            // we set the content-type explicitly to avoid content-negotiation from getting in the way
            // getBinaryResourceMediaType will try to use the mime type on the resource, falling back on
            // 'application/octet-stream' if the mime type is syntactically invalid
            return builder.type(getBinaryResourceMediaType(resource).toString())
                    .cacheControl(cc)
                    .build();

        }

    protected URI getUri(final FedoraResource resource) {
        try {
            final String uri = identifierConverter()
                    .toExternalId(resource.getFedoraId().getFullId());
            return new URI(uri);
        } catch (final URISyntaxException e) {
            throw new BadRequestException(e);
        }
    }

    protected FedoraResource resource(final boolean canReturnTombstone) {
        if (fedoraResource == null) {
            try {
                fedoraResource = getResourceFromPath(externalPath(), canReturnTombstone);
            } catch (TombstoneException e) {
                // We now want to display Mementos for a deleted object, so catch the Tombstone exception.
                fedoraResource = e.getFedoraResource();
                if (! fedoraResource.isMemento()) {
                    // Rethrow the exception if we are not requesting a Memento.
                    throw e;
                }
            }
        }

        return fedoraResource;
    }

    protected FedoraResource resource() {
        return resource(false);
    }

    protected FedoraResource reloadResource() {
        this.fedoraResource = null;
        return resource();
    }

    /**
     * Add the standard Accept-Post header, for reuse.
     */
    private void addAcceptPostHeader() {
        final String rdfTypes = TURTLE + "," + N3 + "," + N3_ALT2 + "," + RDF_XML + "," + NTRIPLES + "," + JSON_LD;
        servletResponse.addHeader("Accept-Post", rdfTypes);
    }

    /**
     * Add the standard Accept-External-Content-Handling header, for reuse.
     */
    private void addAcceptExternalHeader() {
        servletResponse.addHeader(ACCEPT_EXTERNAL_CONTENT, COPY + "," + REDIRECT + "," + PROXY);
    }

    private void addMementoHeaders(final FedoraResource resource) {
        if (resource.isMemento()) {
            final Instant mementoInstant = resource.getMementoDatetime();
            if (mementoInstant != null) {
                final String mementoDatetime = MEMENTO_RFC_1123_FORMATTER
                        .format(mementoInstant.atZone(ZoneOffset.UTC));
                servletResponse.addHeader(MEMENTO_DATETIME_HEADER, mementoDatetime);
            }
        }
    }

    private void addArchiveGroupLinkHeader(final FedoraResource resource) {
        resource.getArchivalGroupId().ifPresent(agFedoraId -> {
            try {
                final var uri = new URI(identifierConverter().toExternalId(agFedoraId.getFullId()));
                final var link = buildLink(uri, "archival-group");
                servletResponse.addHeader(LINK, link);
            } catch (final URISyntaxException e) {
                throw new BadRequestException(e);
            }
        });
    }

    protected void addExternalContentHeaders(final FedoraResource resource) {
        if (resource instanceof Binary binary) {

            if (binary.isProxy() || binary.isRedirect()) {
                servletResponse.addHeader(CONTENT_LOCATION, binary.getExternalURL());
            }
        }
    }

    private void addAclHeader(final FedoraResource resource) {
        if (!(resource instanceof WebacAcl) && !resource.isMemento()) {
            final FedoraResource resourceForAcl = resource instanceof TimeMap ?
                    resource.getOriginalResource().getDescribedResource() : resource.getDescribedResource();
            final String resourceUri = getUri(resourceForAcl).toString();
            final String aclLocation =  resourceUri + (resourceUri.endsWith("/") ? "" : "/") + FCR_ACL;
            servletResponse.addHeader(LINK, buildLink(aclLocation, "acl"));
        }
    }

    private void addResourceLinkHeaders(final FedoraResource resource) {
        addResourceLinkHeaders(resource, false);
    }

    private void addResourceLinkHeaders(final FedoraResource resource, final boolean includeAnchor) {
        if (resource instanceof NonRdfSourceDescription) {
            // Link to the original described resource
            final FedoraResource described = resource.getOriginalResource().getDescribedResource();
            final URI uri = getUri(described);
            final Link link = Link.fromUri(uri).rel("describes").build();
            servletResponse.addHeader(LINK, link.toString());
        } else if (resource instanceof Binary) {
            // Link to the original description
            final FedoraResource description = resource.getOriginalResource().getDescription();
            final URI uri = getUri(description);
            final Link.Builder builder = Link.fromUri(uri).rel("describedby");

            if (includeAnchor) {
                builder.param("anchor", getUri(resource).toString());
            }
            servletResponse.addHeader(LINK, builder.build().toString());

            if (resource.isMemento()) {
                final URI mementoDescription = getUri(resource.getDescription());
                servletResponse.addHeader(LINK, buildLink(mementoDescription, "describedby"));
            }
        }

        final boolean isOriginal = resource.isOriginalResource();
        // Add versioning headers for versioned originals and mementos
        if (isOriginal || resource.isMemento() || resource instanceof TimeMap) {
            final URI originalUri = getUri(resource.getOriginalResource());
            try {
                final URI timemapUri = getUri(resource.getTimeMap());
                servletResponse.addHeader(LINK, buildLink(originalUri, "timegate"));
                servletResponse.addHeader(LINK, buildLink(originalUri, "original"));
                servletResponse.addHeader(LINK, buildLink(timemapUri, "timemap"));
            } catch (final PathNotFoundRuntimeException e) {
                LOGGER.debug("TimeMap not found for {}, resource not versioned", getUri(resource));
            }
        }
        // Add all system and user types as Link headers.
        for (final var type : resource.getTypes()) {
            servletResponse.addHeader(LINK, buildLink(type, "type"));
        }
    }

    /**
     * Add Link and Option headers
     *
     * @param resource the resource to generate headers for
     */
    protected void addLinkAndOptionsHttpHeaders(final FedoraResource resource) {
        // Add Link headers
        addResourceLinkHeaders(resource);
        addArchiveGroupLinkHeader(resource);
        addAcceptExternalHeader();

        // Add Options headers
        final String options;
        if (resource.isMemento()) {
            options = "GET,HEAD,OPTIONS";
        } else if (resource instanceof TimeMap) {
            options = (resource.getOriginalResource() instanceof Tombstone ? "HEAD,GET,OPTIONS" :
                    "POST,HEAD,GET,OPTIONS");
            addAcceptPostHeader();
        } else if (resource instanceof Binary) {
            options = "DELETE,HEAD,GET,PUT,OPTIONS";
        } else if (resource instanceof NonRdfSourceDescription) {
            options = "HEAD,GET,DELETE,PUT,PATCH,OPTIONS";
            servletResponse.addHeader(HTTP_HEADER_ACCEPT_PATCH, contentTypeSPARQLUpdate);
        } else if (resource instanceof Container) {
            options = "DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS";
            servletResponse.addHeader(HTTP_HEADER_ACCEPT_PATCH, contentTypeSPARQLUpdate);
            addAcceptPostHeader();
        } else {
            options = "";
        }

        servletResponse.addHeader("Allow", options);
    }

    protected void addTransactionHeaders(final FedoraResource resource) {
        final var tx = transaction();
        if (!tx.isShortLived()) {
            final var externalId = identifierConverter()
                    .toExternalId(FEDORA_ID_PREFIX + "/" + TX_PREFIX + tx.getId());
            servletResponse.addHeader(ATOMIC_ID_HEADER, externalId);
        }
        if (resource.getFedoraId().isRepositoryRoot()) {
            final var txEndpointUri = identifierConverter()
                    .toExternalId(FEDORA_ID_PREFIX + "/" + TX_PREFIX);
            final Link link = Link.fromUri(txEndpointUri).rel(TX_ENDPOINT_REL).build();
            servletResponse.addHeader(LINK, link.toString());
        }
    }

    /**
     * Utility function for building a Link.
     *
     * @param linkUri String of URI for the link.
     * @param relation the relation string.
     * @return the string version of the link.
     */
    protected static String buildLink(final String linkUri, final String relation) {
        return buildLink(create(linkUri), relation);
    }

    /**
     * Utility function for building a Link.
     *
     * @param linkUri The URI for the link.
     * @param relation the relation string.
     * @return the string version of the link.
     */
    private static String buildLink(final URI linkUri, final String relation) {
        return Link.fromUri(linkUri).rel(relation).build().toString();
    }

    /**
     * Multi-value Link header values parsed by the jakarta.ws.rs.core are not split out by the framework Therefore we
     * must do this ourselves.
     *
     * @param rawLinks the list of unprocessed links
     * @return List of strings containing one link value per string.
     */
    protected List<String> unpackLinks(final List<String> rawLinks) {
        if (rawLinks == null) {
            return null;
        }

        return rawLinks.stream()
                .flatMap(x -> Arrays.stream(x.split(",")))
                .collect(Collectors.toList());
    }

    protected void addResourceHttpHeaders(final FedoraResource resource) {
        addResourceHttpHeaders(resource, false);
    }

    protected void addResourceHttpHeaders(final FedoraResource resource, final boolean dispositionInline) {
        addResourceHttpHeaders(resource, dispositionInline, false);
    }

    /**
     * Add any resource-specific headers to the response
     * @param resource the resource
     * @param dispositionInline whether to return a binary as Content-Disposition inline
     */
    protected void addResourceHttpHeaders(final FedoraResource resource,
                                          final boolean dispositionInline,
                                          final boolean isRedirect) {
        if (resource instanceof Binary binary) {

            final ContentDisposition.Builder dispositionBuilder = (dispositionInline ? ContentDisposition.inline() :
                    ContentDisposition.attachment());

            if (StringUtils.isNotBlank(binary.getFilename())) {
                final var encoder = StandardCharsets.ISO_8859_1.newEncoder();

                if (encoder.canEncode(binary.getFilename())) {
                    dispositionBuilder.filename(binary.getFilename());
                } else {
                    dispositionBuilder.filename(binary.getFilename(), StandardCharsets.UTF_8);
                }
            }

            servletResponse.addHeader(CONTENT_TYPE, binary.getMimeType());
            // Returning content-length > 0 causes the client to wait for additional data before following the redirect.
            // final var responseIsRedirect = servletResponse.getStatus() >= 300 && servletResponse.getStatus() < 400;
            if (!binary.isRedirect() && !isRedirect) {
                servletResponse.addHeader(CONTENT_LENGTH, String.valueOf(binary.getContentSize()));
            }
            servletResponse.addHeader("Accept-Ranges", "bytes");
            servletResponse.addHeader(CONTENT_DISPOSITION, dispositionBuilder.build().toString());
        }
        // Add ocflPath header if desired
        if (ocflPropsConfig.isShowPath()) {
            final var contentPath = resource.getStorageRelativePath();
            if (contentPath != null) {
                servletResponse.addHeader(HTTP_OCFL_PATH, contentPath.toString());
            }
        }

        addLinkAndOptionsHttpHeaders(resource);
        addAclHeader(resource);
        addMementoHeaders(resource);
    }

    /**
     * Evaluate the cache control headers for the request to see if it can be served from
     * the cache.
     *
     * @param request the request
     * @param servletResponse the servlet response
     * @param resource the fedora resource
     * @param transaction the transaction
     */
    protected void checkCacheControlHeaders(final Request request,
                                                   final HttpServletResponse servletResponse,
                                                   final FedoraResource resource,
                                                   final Transaction transaction) {
        evaluateRequestPreconditions(request, servletResponse, resource, transaction, true);
        addCacheControlHeaders(servletResponse, resource, transaction);
    }

    /**
     * Add ETag and Last-Modified cache control headers to the response
     * <p>
     * Note: In this implementation, the HTTP headers for ETags and Last-Modified dates are swapped
     * for fedora:Binary resources and their descriptions. Here, we are drawing a distinction between
     * the HTTP resource and the LDP resource. As an HTTP resource, the last-modified header should
     * reflect when the resource at the given URL was last changed. With fedora:Binary resources and
     * their descriptions, this is a little complicated, for the descriptions have, as their subjects,
     * the binary itself. And the fedora:lastModified property produced by that NonRdfSourceDescription
     * refers to the last-modified date of the binary -- not the last-modified date of the
     * NonRdfSourceDescription.
     * </p>
     * @param servletResponse the servlet response
     * @param resource the fedora resource
     * @param transaction the transaction
     */
    protected void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final FedoraResource resource,
                                                 final Transaction transaction) {

        final EntityTag etag;
        final Instant date;

        // See note about this code in the javadoc above.
        if (resource instanceof Binary) {
            // Use a strong ETag for LDP-NR
            etag = new EntityTag(resource.getEtagValue());
        } else {
            // Use a weak ETag for the LDP-RS
            etag = new EntityTag(etagService.getRdfResourceEtag(transaction, resource, getLdpPreferTag(),
                    headers.getAcceptableMediaTypes()), true);
        }

        date = resource.getLastModifiedDate();

        if (!etag.getValue().isEmpty()) {
            servletResponse.addHeader("ETag", formatETagHeader(etag));
        }

        if (resource.getStateToken() != null && !resource.getStateToken().isEmpty()) {
            //State Tokens, while not used for caching per se,  nevertheless belong
            //here since we can conveniently reuse the value of the etag for
            //our state token
            servletResponse.addHeader("X-State-Token", resource.getStateToken());
        }

        if (date != null) {
            servletResponse.addDateHeader("Last-Modified", date.toEpochMilli());
        }
    }

    /**
     * Evaluate request preconditions to ensure the resource is the expected state
     * @param request the request
     * @param servletResponse the servlet response
     * @param resource the resource
     * @param transaction the transaction
     */
    protected void evaluateRequestPreconditions(final Request request,
                                                       final HttpServletResponse servletResponse,
                                                       final FedoraResource resource,
                                                       final Transaction transaction) {
        // The resource must be locked prior to applying pre-conditions for the optimistic locking to be effective
        transaction.lockResource(resource.getFedoraId());
        evaluateRequestPreconditions(request, servletResponse, resource, transaction, false);
    }

    /**
     * Jakarta has deprecated the EntityTag.toString() method, this function duplicates the functionality
     * @param etag the entity tag
     * @return the formatted ETag header value
     */
    protected static String formatETagHeader(final EntityTag etag) {
        final String quotedValue = "\"" + etag.getValue() + "\"";
        return etag.isWeak() ? "W/" + quotedValue : quotedValue;
    }


    @VisibleForTesting
    void evaluateRequestPreconditions(final Request request,
                                                     final HttpServletResponse servletResponse,
                                                     final FedoraResource resource,
                                                     final Transaction transaction,
                                                     final boolean cacheControl) {

        if (!transaction.isShortLived()) {
            // Force cache revalidation if in a transaction
            servletResponse.addHeader(CACHE_CONTROL, "must-revalidate");
            servletResponse.addHeader(CACHE_CONTROL, "max-age=0");
        }

        final EntityTag etag;
        final Instant date;
        Instant roundedDate = Instant.now();

        // See the related note about the next block of code in the
        // ContentExposingResource::addCacheControlHeaders method
        if (resource instanceof Binary) {
            // Use a strong ETag for the LDP-NR
            etag = new EntityTag(resource.getEtagValue());
        } else {
            // Use a strong ETag for the LDP-RS when validating If-(None)-Match headers
            etag = new EntityTag(etagService.getRdfResourceEtag(transaction, resource, getLdpPreferTag(),
                    headers.getAcceptableMediaTypes()), false);
        }

        date = resource.getLastModifiedDate();

        if (date != null) {
            roundedDate = date.minusMillis(date.toEpochMilli() % 1000);
        }

        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
        if ( builder == null ) {
            builder = request.evaluatePreconditions(Date.from(roundedDate));
        }

        if (builder != null && cacheControl ) {
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            // here we are implicitly emitting a 304
            // the exception is not an error, it's genuinely
            // an exceptional condition
            builder = builder.cacheControl(cc).lastModified(Date.from(roundedDate)).tag(etag);
        }

        if (builder != null) {
            try (final Response response = builder.build()) {
                final Object message = response.getEntity();
                throw new PreconditionException(message != null ? message.toString()
                        : "Request failed due to unspecified failed precondition.", response.getStatus());
            }
        }

        final String method = request.getMethod();
        if (method.equals(HttpPut.METHOD_NAME) || method.equals(HttpPatch.METHOD_NAME)) {
            final String stateToken = resource.getStateToken();
            final String clientSuppliedStateToken = headers.getHeaderString("X-If-State-Token");
            if (clientSuppliedStateToken != null && !stateToken.equals(clientSuppliedStateToken)) {
                throw new PreconditionException(format(
                    "The client-supplied value ({0}) does not match the current state token ({1}).",
                    clientSuppliedStateToken, stateToken), 412);
            }
        }
    }

    /**
     * Returns an acceptable plain text media type if possible, or null if not.
     * @return an acceptable plain-text media type, or null
     */
    private MediaType acceptablePlainTextMediaType() {
        final List<MediaType> acceptable = headers.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.isEmpty()) {
            return TEXT_PLAIN_TYPE;
        }
        for (final MediaType type : acceptable) {
            if (type.isWildcardType() || (type.isCompatible(TEXT_PLAIN_TYPE) && type.isWildcardSubtype())) {
                return TEXT_PLAIN_TYPE;
            } else if (type.isCompatible(TEXT_PLAIN_TYPE)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Create the appropriate response after a create or update request is processed. When a resource is created,
     * examine the Prefer and Accept headers to determine whether to include a representation. By default, the URI for
     * the created resource is return as plain text. If a minimal response is requested, then no body is returned. If a
     * non-minimal return is requested, return the RDF for the created resource in the appropriate RDF serialization.
     *
     * @param resource The created or updated Fedora resource.
     * @param created True for a newly-created resource, false for an updated resource.
     * @return 204 No Content (for updated resources), 201 Created (for created resources) including the resource URI or
     *         content depending on Prefer headers.
     */
    protected Response createUpdateResponse(final FedoraResource resource, final boolean created) {
        addCacheControlHeaders(servletResponse, resource, transaction());
        addResourceLinkHeaders(resource, created);
        addExternalContentHeaders(resource);
        addAclHeader(resource);
        addMementoHeaders(resource);
        addTransactionHeaders(resource);

        if (!created) {
            return noContent().build();
        }

        final URI location = getUri(resource);
        final Response.ResponseBuilder builder = created(location);

        if (prefer == null || !prefer.hasReturn()) {
            final MediaType acceptablePlainText = acceptablePlainTextMediaType();
            if (acceptablePlainText != null) {
                return builder.type(acceptablePlainText).entity(location.toString()).build();
            }
            return notAcceptable(mediaTypes(TEXT_PLAIN_TYPE).build()).build();
        } else if (prefer.getReturn().getValue().equals("minimal")) {
            return builder.build();
        } else {
            prefer.getReturn().addResponseHeaders(servletResponse);
            final RdfNamespacedStream rdfStream = new RdfNamespacedStream(
                new DefaultRdfStream(asNode(resource), getResourceTriples(resource)),
                namespaceRegistry.getNamespaces());
            return builder.entity(rdfStream).build();
        }
    }

    protected static String getSimpleContentType(final MediaType requestContentType) {
        return requestContentType != null ?
                requestContentType.getType() + "/" + requestContentType.getSubtype()
                : null;
    }

    protected static boolean isRdfContentType(final String contentTypeString) {
        final ContentType requestContentType = ContentType.create(contentTypeString);
        if (requestContentType == null || matchContentType(requestContentType, ctTextPlain) ||
                matchContentType(requestContentType, ctTextCSV)) {
            // Text files and CSV files are not considered RDF to Fedora, though CSV is a valid
            // RDF type to Jena (although deprecated).
            return false;
        }
        return (contentTypeToLang(contentTypeString) != null) || matchContentType(requestContentType, ctSPARQLUpdate);
    }


    protected void patchResourcewithSparql(final FedoraResource resource,
            final String requestBody) {
        updatePropertiesService.updateProperties(transaction(),
                                                 getUserPrincipal(),
                                                 resource.getFedoraId(),
                                                 requestBody);
    }

    /**
     * This method returns a MediaType for a binary resource.
     * If the resource's media type is syntactically incorrect, it will
     * return 'application/octet-stream' as the media type.
     * @param  resource the fedora resource
     * @return the media type of of a binary resource
     */
    protected MediaType getBinaryResourceMediaType(final FedoraResource resource) {
        try {
            return MediaType.valueOf(((Binary) resource).getMimeType());
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Syntactically incorrect MediaType encountered on resource {}: '{}'",
                    resource.getId(), ((Binary)resource).getMimeType());
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }

    /**
     * Create a checksum URI object.
     * @param checksum the checksum
     * @return the new URI, or null
     **/
    protected static URI checksumURI( final String checksum ) {
        if (!isBlank(checksum)) {
            return create(checksum);
        }
        return null;
    }

    /**
     * Calculate the max number of children to display at once.
     *
     * @return the limit of children to display.
     */
    protected int getChildrenLimit() {
        final List<String> acceptHeaders = headers.getRequestHeader(ACCEPT);
        if (acceptHeaders != null && !acceptHeaders.isEmpty()) {
            final List<String> accept = Arrays.asList(acceptHeaders.getFirst().split(","));
            if (accept.contains(TEXT_HTML)) {
                // Magic number '100' is tied to common-metadata.vsl display of ellipses
                return 100;
            }
        }

        final List<String> limits = headers.getRequestHeader("Limit");
        if (null != limits && !limits.isEmpty()) {
            try {
                return Integer.parseInt(limits.getFirst());

            } catch (final NumberFormatException e) {
                LOGGER.warn("Invalid 'Limit' header value: {}", limits.getFirst());
                throw new ClientErrorException("Invalid 'Limit' header value: " + limits.getFirst(), SC_BAD_REQUEST, e);
            }
        }
        return -1;
    }

    /**
     * Check if a path has a segment prefixed with fcr: that is not fcr:metadata or fcr:acl
     *
     * @param externalPath the path.
     */
    protected static void hasRestrictedPath(final String externalPath) {
        final String[] pathSegments = externalPath.split("/");
        for (final var part : pathSegments) {
            if (part.startsWith(FCR_PREFIX) && !ALLOWED_FCR_PARTS.contains(part)) {
                throw new ServerManagedTypeException("Path cannot contain a fcr: prefixed segment.");
            }
        }
    }

    /**
     * Parse the RFC-3230 Digest response header value. Look for a sha1 checksum and return it as a urn, if missing or
     * malformed an empty string is returned.
     *
     * @param digest The Digest header value
     * @return the sha1 checksum value
     * @throws UnsupportedAlgorithmException if an unsupported digest is used
     */
    protected static Collection<URI> parseDigestHeader(final String digest) throws UnsupportedAlgorithmException {
        try {
            final var digestPairs = RFC3230_SPLITTER.split(nullToEmpty(digest));
            final var unsupportedAlgs = digestPairs.keySet().stream()
                    .filter(Predicate.not(DigestAlgorithm::isSupportedAlgorithm))
                    .collect(Collectors.toSet());

            // If you have one or more digests that are all valid or no digests.
            if (digestPairs.isEmpty() || unsupportedAlgs.isEmpty()) {
                return digestPairs.entrySet().stream()
                    .map(entry -> ContentDigest.asURI(entry.getKey(), entry.getValue()))
                    .collect(toSet());
            } else {
                throw new UnsupportedAlgorithmException(String.format("Unsupported Digest Algorithm%1$s: %2$s",
                        unsupportedAlgs.size() > 1 ? 's' : "", String.join(",", unsupportedAlgs)));
            }
        } catch (final IllegalArgumentException e) {
            throw new ClientErrorException("Invalid Digest header: " + digest + "\n", BAD_REQUEST);
        }
    }

    /**
     * @param rootThrowable The original throwable
     * @param throwable The throwable under direct scrutiny.
     * @throws InvalidChecksumException in case there was a checksum mismatch
     */
    protected void checkForInsufficientStorageException(final Throwable rootThrowable, final Throwable throwable)
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
            throw new RepositoryRuntimeException(rootThrowable.getMessage(), rootThrowable);
        }
    }

    /**
     * This is a helper method for using the idTranslator to convert this resource into an associated Jena Node.
     *
     * @param resource to be converted into a Jena Node
     * @return the Jena node
     */
    protected Node asNode(final FedoraResource resource) {
        return createURI(resource.getFedoraId().getFullId());
    }

    /**
     * Get the FedoraResource for the resource at the external path
     * @param externalPath the external path
     * @return the fedora resource at the external path
     */
    private FedoraResource getResourceFromPath(final String externalPath) {
        return getResourceFromPath(externalPath, false);
    }

    /**
     * Get the FedoraResource for the resource at the external path
     * @param externalPath the external path
     * @param canReturnTombstone if tombstones can be returned
     * @return the fedora resource at the external path
     */
    private FedoraResource getResourceFromPath(final String externalPath, final boolean canReturnTombstone) {
        final FedoraId fedoraId = identifierConverter().pathToInternalId(externalPath);

        try {
            final FedoraResource fedoraResource = resourceFactory.getResource(transaction(), fedoraId);

            final FedoraResource originalResource;
            if (fedoraId.isMemento()) {
                originalResource = fedoraResource.getOriginalResource();
            } else {
                originalResource = fedoraResource;
            }

            if (originalResource instanceof Tombstone && !canReturnTombstone) {
                final String tombstoneUri = identifierConverter().toExternalId(
                            originalResource.getFedoraId().asTombstone().getFullId());
                final String timemapUri = identifierConverter().toExternalId(
                        originalResource.getTimeMap().getFedoraId().getFullId());
                throw new TombstoneException(fedoraResource, tombstoneUri, timemapUri);
            }

            return fedoraResource;
        } catch (final PathNotFoundException exc) {
            throw new PathNotFoundRuntimeException(exc.getMessage(), exc);
        }
    }
}
