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

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.EnumSet.of;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
import static javax.ws.rs.core.Variant.mediaTypes;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.http.api.FedoraVersioning.MEMENTO_DATETIME_HEADER;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.kernel.api.FedoraExternalContent.COPY;
import static org.fcrepo.kernel.api.FedoraExternalContent.PROXY;
import static org.fcrepo.kernel.api.FedoraExternalContent.REDIRECT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEGATE_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_NAMESPACE_VALUE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RequiredRdfContext.EMBED_RESOURCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_CONTAINMENT;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_MEMBERSHIP;
import static org.fcrepo.kernel.api.RequiredRdfContext.MINIMAL;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.fcrepo.http.commons.api.HttpHeaderInjector;
import org.fcrepo.http.commons.api.rdf.HttpTripleUtil;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.ACLAuthorizationConstraintViolationException;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PreconditionException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.models.FedoraWebacAcl;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.jvnet.hk2.annotations.Optional;
import org.slf4j.Logger;

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

    static final String WEBAC_ACCESS_TO = WEBAC_NAMESPACE_VALUE + "accessTo";

    static final String WEBAC_ACCESS_TO_CLASS = WEBAC_NAMESPACE_VALUE + "accessToClass";

    static final Node WEBAC_ACCESS_TO_URI = createURI(WEBAC_ACCESS_TO);

    static final Node WEBAC_ACCESS_TO_CLASS_URI = createURI(WEBAC_ACCESS_TO_CLASS);

    static final Property WEBAC_ACCESS_TO_PROPERTY = createProperty(WEBAC_ACCESS_TO);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected ServletContext context;

    @Inject
    @Optional
    private HttpTripleUtil httpTripleUtil;

    @Inject
    @Optional
    private HttpHeaderInjector httpHeaderInject;

    @BeanParam
    protected MultiPrefer prefer;

    @Inject
    @Optional
    StoragePolicyDecisionPoint storagePolicyDecisionPoint;

    private FedoraResource fedoraResource;

    @Inject
    protected  PathLockManager lockManager;

    @Inject
    protected ExternalContentHandlerFactory extContentHandlerFactory;

    private static final Predicate<Triple> IS_MANAGED_TYPE = t -> t.getPredicate().equals(type.asNode()) &&
            isManagedNamespace.test(t.getObject().getNameSpace());
    private static final Predicate<Triple> IS_MANAGED_TRIPLE = IS_MANAGED_TYPE
        .or(t -> isManagedPredicate.test(createProperty(t.getPredicate().getURI())));

    protected abstract String externalPath();

    protected static final Splitter.MapSplitter RFC3230_SPLITTER =
        Splitter.on(',').omitEmptyStrings().trimResults().withKeyValueSeparator(Splitter.on('=').limit(2));

    /**
     * This method returns an HTTP response with content body appropriate to the following arguments.
     *
     * @param rangeValue starting and ending byte offsets, see {@link Range}
     * @param limit is the number of child resources returned in the response, -1 for all
     * @param rdfStream to which response RDF will be concatenated
     * @param resource the fedora resource
     * @return HTTP response
     * @throws IOException in case of error extracting content
     * @throws UnsupportedAccessTypeException if mimeType not a valid message/external-body content type
     */
    protected Response getContent(final String rangeValue,
                                  final int limit,
                                  final RdfStream rdfStream,
                                  final FedoraResource resource) throws IOException, UnsupportedAccessTypeException {

        final RdfNamespacedStream outputStream;

        if (resource instanceof FedoraBinary) {
            return getBinaryContent(rangeValue, resource);
        } else {
            outputStream = new RdfNamespacedStream(
                    new DefaultRdfStream(rdfStream.topic(), concat(rdfStream,
                        getResourceTriples(limit, resource))),
                    session.getFedoraSession().getNamespaces());
        }
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

        if (resource.isVersioned()) {
            varyValues.add(ACCEPT_DATETIME);
        }

        varyValues.stream().forEach(x -> servletResponse.addHeader("Vary", x));
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

        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else if (prefer != null && prefer.hasHandling()) {
            returnPreference = prefer.getHandling();
        } else {
            returnPreference = PreferTag.emptyTag();
        }

        final LdpPreferTag ldpPreferences = new LdpPreferTag(returnPreference);

        final Predicate<Triple> tripleFilter = ldpPreferences.prefersServerManaged() ? x -> true :
            IS_MANAGED_TRIPLE.negate();

        final List<Stream<Triple>> streams = new ArrayList<>();


        if (returnPreference.getValue().equals("minimal")) {
            streams.add(getTriples(resource, of(PROPERTIES, MINIMAL)).filter(tripleFilter));

            // Mementos already have the server managed properties in the PROPERTIES category
            // since mementos are immutable and these triples are no longer managed
            if (ldpPreferences.prefersServerManaged() && !resource.isMemento())  {
                streams.add(getTriples(resource, of(SERVER_MANAGED, MINIMAL)));
            }
        } else {
            streams.add(getTriples(resource, PROPERTIES).filter(tripleFilter));

            // Additional server-managed triples about this resource
            // Mementos already have the server managed properties in the PROPERTIES category
            // since mementos are immutable and these triples are no longer managed
             if (ldpPreferences.prefersServerManaged() && !resource.isMemento()) {
                streams.add(getTriples(resource, SERVER_MANAGED));
            }

            // containment triples about this resource
            if (ldpPreferences.prefersContainment()) {
                if (limit == -1) {
                    streams.add(getTriples(resource, LDP_CONTAINMENT));
                } else {
                    streams.add(getTriples(resource, LDP_CONTAINMENT).limit(limit));
                }
            }

            // LDP container membership triples for this resource
            if (ldpPreferences.prefersMembership()) {
                streams.add(getTriples(resource, LDP_MEMBERSHIP));
            }

            // Include inbound references to this object
            if (ldpPreferences.prefersReferences()) {
                streams.add(getTriples(resource, INBOUND_REFERENCES));
            }

            // Embed the children of this object
            if (ldpPreferences.prefersEmbed()) {
                streams.add(getTriples(resource, EMBED_RESOURCES));
            }
        }

        final RdfStream rdfStream = new DefaultRdfStream(
                asNode(resource), streams.stream().reduce(empty(), Stream::concat));

        if (httpTripleUtil != null && ldpPreferences.prefersServerManaged()) {
            return httpTripleUtil.addHttpComponentModelsForResourceToStream(rdfStream, resource, uriInfo,
                    translator());
        }

        return rdfStream;
    }

    /**
     * Get the binary content of a datastream
     *
     * @param rangeValue the range value
     * @param resource the fedora resource
     * @return Binary blob
     * @throws IOException if io exception occurred
     */
    private Response getBinaryContent(final String rangeValue, final FedoraResource resource)
            throws IOException {
            final FedoraBinary binary = (FedoraBinary)resource;
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            final Response.ResponseBuilder builder;

            if (rangeValue != null && rangeValue.startsWith("bytes")) {

                final Range range = Range.convert(rangeValue);

                final long contentSize = binary.getContentSize();

                final String endAsString;

                if (range.end() == -1) {
                    endAsString = Long.toString(contentSize - 1);
                } else {
                    endAsString = Long.toString(range.end());
                }

                final String contentRangeValue =
                        String.format("bytes %s-%s/%s", range.start(),
                                endAsString, contentSize);

                if (range.end() > contentSize ||
                        (range.end() == -1 && range.start() > contentSize)) {

                    builder = status(REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header("Content-Range", contentRangeValue);
                } else {
                    @SuppressWarnings("resource")
                    final RangeRequestInputStream rangeInputStream =
                            new RangeRequestInputStream(binary.getContent(), range.start(), range.size());

                    builder = status(PARTIAL_CONTENT).entity(rangeInputStream)
                            .header("Content-Range", contentRangeValue)
                            .header(CONTENT_LENGTH, range.size());
                }

            } else {
                @SuppressWarnings("resource")
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

    protected RdfStream getTriples(final FedoraResource resource, final Set<? extends TripleCategory> x) {
        return resource.getTriples(translator(), x);
    }

    protected RdfStream getTriples(final FedoraResource resource, final TripleCategory x) {
        return resource.getTriples(translator(), x);
    }

    protected URI getUri(final FedoraResource resource) {
        try {
            final String uri = translator().reverse().convert(resource).getURI();
            return new URI(uri);
        } catch (final URISyntaxException e) {
            throw new BadRequestException(e);
        }
    }

    protected FedoraResource resource() {
        if (fedoraResource == null) {
            fedoraResource = getResourceFromPath(externalPath());
        }
        return fedoraResource;
    }

    /**
     * Add the standard Accept-Post header, for reuse.
     */
    protected void addAcceptPostHeader() {
        final String rdfTypes = TURTLE + "," + N3 + "," + N3_ALT2 + "," + RDF_XML + "," + NTRIPLES + "," + JSON_LD;
        servletResponse.addHeader("Accept-Post", rdfTypes);
    }

    /**
     * Add the standard Accept-External-Content-Handling header, for reuse.
     */
    protected void addAcceptExternalHeader() {
        servletResponse.addHeader(ACCEPT_EXTERNAL_CONTENT, COPY + "," + REDIRECT + "," + PROXY);
    }

    protected void addMementoHeaders(final FedoraResource resource) {
        if (resource.isMemento()) {
            final Instant mementoInstant = resource.getMementoDatetime();
            if (mementoInstant != null) {
                final String mementoDatetime = DateTimeFormatter.RFC_1123_DATE_TIME
                        .format(mementoInstant.atZone(ZoneOffset.UTC));
                servletResponse.addHeader(MEMENTO_DATETIME_HEADER, mementoDatetime);
            }
            servletResponse.addHeader(LINK, buildLink(MEMENTO_TYPE, "type"));
        }
    }

    protected void addExternalContentHeaders(final FedoraResource resource) {
        if (resource instanceof FedoraBinary) {
            final FedoraBinary binary = (FedoraBinary)resource;

            if (binary.isProxy()) {
                servletResponse.addHeader(CONTENT_LOCATION, binary.getProxyURL());
            } else if (binary.isRedirect()) {
                servletResponse.addHeader(CONTENT_LOCATION, binary.getRedirectURL());
            }
        }
    }

    protected void addAclHeader(final FedoraResource resource) {
        if (!(resource instanceof FedoraWebacAcl) && !resource.isMemento()) {
            final String resourceUri = getUri(resource.getDescribedResource()).toString();
            final String aclLocation =  resourceUri + (resourceUri.endsWith("/") ? "" : "/") + FCR_ACL;
            servletResponse.addHeader(LINK, buildLink(aclLocation, "acl"));
        }
    }

    protected void addResourceLinkHeaders(final FedoraResource resource) {
        addResourceLinkHeaders(resource, false);
    }

    protected void addResourceLinkHeaders(final FedoraResource resource, final boolean includeAnchor) {
        if (resource instanceof NonRdfSourceDescription) {
            // Link to the original described resource
            final FedoraResource described = resource.getOriginalResource().getDescribedResource();
            final URI uri = getUri(described);
            final Link link = Link.fromUri(uri).rel("describes").build();
            servletResponse.addHeader(LINK, link.toString());
        } else if (resource instanceof FedoraBinary) {
            // Link to the original description
            final FedoraResource description = resource.getOriginalResource().getDescription();
            final URI uri = getUri(description);
            final Link.Builder builder = Link.fromUri(uri).rel("describedby");

            if (includeAnchor) {
                builder.param("anchor", getUri(resource).toString());
            }
            servletResponse.addHeader(LINK, builder.build().toString());

            final String path = context.getContextPath().equals("/") ? "" : context.getContextPath();
            final String constraintURI = uriInfo.getBaseUri().getScheme() + "://" +
                    uriInfo.getBaseUri().getAuthority() + path +
                    "/static/constraints/NonRDFSourceConstraints.rdf";
            servletResponse.addHeader(LINK,
                buildLink(constraintURI, CONSTRAINED_BY.getURI()));
        } else {
            final String path = context.getContextPath().equals("/") ? "" : context.getContextPath();
            final String constraintURI = uriInfo.getBaseUri().getScheme() + "://" +
                    uriInfo.getBaseUri().getAuthority() + path +
                    "/static/constraints/ContainerConstraints.rdf";
            servletResponse.addHeader(LINK,
                buildLink(constraintURI, CONSTRAINED_BY.getURI()));
        }

        final boolean isVersioned = resource.isVersioned();
        // Add versioning headers for versioned originals and mementos
        if (isVersioned || resource.isMemento() || resource instanceof FedoraTimeMap) {
            final URI originalUri = getUri(resource.getOriginalResource());
            final URI timemapUri = getUri(resource.getTimeMap());
            servletResponse.addHeader(LINK, buildLink(originalUri, "timegate"));
            servletResponse.addHeader(LINK, buildLink(originalUri, "original"));
            servletResponse.addHeader(LINK, buildLink(timemapUri, "timemap"));

            if (isVersioned) {
                servletResponse.addHeader(LINK, buildLink(VERSIONED_RESOURCE.getURI(), "type"));
                servletResponse.addHeader(LINK, buildLink(VERSIONING_TIMEGATE_TYPE, "type"));
            } else if (resource instanceof FedoraTimeMap) {
                servletResponse.addHeader(LINK, buildLink(VERSIONING_TIMEMAP_TYPE, "type"));
            }
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
        addAcceptExternalHeader();

        // Add Options headers
        final String options;
        if (resource.isMemento()) {
            options = "GET,HEAD,OPTIONS,DELETE";
        } else if (resource instanceof FedoraTimeMap) {
            options = "POST,HEAD,GET,OPTIONS,DELETE";
            servletResponse.addHeader("Vary-Post", MEMENTO_DATETIME_HEADER);
            addAcceptPostHeader();
        } else if (resource instanceof FedoraBinary) {
            options = "DELETE,HEAD,GET,PUT,OPTIONS";
        } else if (resource instanceof NonRdfSourceDescription) {
            options = "HEAD,GET,DELETE,PUT,PATCH,OPTIONS";
            servletResponse.addHeader(HTTP_HEADER_ACCEPT_PATCH, contentTypeSPARQLUpdate);
        } else if (resource instanceof Container) {
            options = "MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS";
            servletResponse.addHeader(HTTP_HEADER_ACCEPT_PATCH, contentTypeSPARQLUpdate);
            addAcceptPostHeader();
        } else {
            options = "";
        }

        servletResponse.addHeader("Allow", options);
    }

    /**
     * Utility function for building a Link.
     *
     * @param linkUri String of URI for the link.
     * @param relation the relation string.
     * @return the string version of the link.
     */
    protected static String buildLink(final String linkUri, final String relation) {
        return buildLink(URI.create(linkUri), relation);
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
     * Multi-value Link header values parsed by the javax.ws.rs.core are not split out by the framework Therefore we
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
                .flatMap(x -> Arrays.asList(x.split(",")).stream())
                .collect(Collectors.toList());
    }

    /**
     * Add any resource-specific headers to the response
     * @param resource the resource
     */
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        if (resource instanceof FedoraBinary) {
            final FedoraBinary binary = (FedoraBinary)resource;
            final Date createdDate = binary.getCreatedDate() != null ? Date.from(binary.getCreatedDate()) : null;
            final Date modDate = binary.getLastModifiedDate() != null ? Date.from(binary.getLastModifiedDate()) : null;

            final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                    .fileName(binary.getFilename())
                    .creationDate(createdDate)
                    .modificationDate(modDate)
                    .size(binary.getContentSize())
                    .build();

            servletResponse.addHeader(CONTENT_TYPE, binary.getMimeType());
            // Returning content-length > 0 causes the client to wait for additional data before following the redirect.
            if (!binary.isRedirect()) {
                servletResponse.addHeader(CONTENT_LENGTH, String.valueOf(binary.getContentSize()));
            }
            servletResponse.addHeader("Accept-Ranges", "bytes");
            servletResponse.addHeader(CONTENT_DISPOSITION, contentDisposition.toString());
        }

        servletResponse.addHeader(LINK, "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");

        if (resource instanceof FedoraBinary) {
            servletResponse.addHeader(LINK, "<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\"");
        } else if (resource instanceof Container || resource instanceof FedoraTimeMap) {
            servletResponse.addHeader(LINK, "<" + CONTAINER.getURI() + ">;rel=\"type\"");
            servletResponse.addHeader(LINK, buildLink(RDF_SOURCE.getURI(), "type"));
            if (resource.hasType(LDP_BASIC_CONTAINER)) {
                servletResponse.addHeader(LINK, "<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\"");
            } else if (resource.hasType(LDP_DIRECT_CONTAINER)) {
                servletResponse.addHeader(LINK, "<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\"");
            } else if (resource.hasType(LDP_INDIRECT_CONTAINER)) {
                servletResponse.addHeader(LINK, "<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\"");
            } else {
                servletResponse.addHeader(LINK, "<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\"");
            }
        } else {
            servletResponse.addHeader(LINK, buildLink(RDF_SOURCE.getURI(), "type"));
        }
        if (httpHeaderInject != null) {
            httpHeaderInject.addHttpHeaderToResponseStream(servletResponse, uriInfo, resource);
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
     * @param session the session
     */
    protected void checkCacheControlHeaders(final Request request,
                                                   final HttpServletResponse servletResponse,
                                                   final FedoraResource resource,
                                                   final HttpSession session) {
        evaluateRequestPreconditions(request, servletResponse, resource, session, true);
        addCacheControlHeaders(servletResponse, resource, session);
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
     * @param session the session
     */
    protected void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final FedoraResource resource,
                                                 final HttpSession session) {

        if (session.isBatchSession()) {
            // Do not add caching headers if in a transaction
            return;
        }

        final EntityTag etag;
        final Instant date;

        // See note about this code in the javadoc above.
        if (resource instanceof FedoraBinary) {
            // Use a strong ETag for LDP-NR
            etag = new EntityTag(resource.getEtagValue());
            date = resource.getLastModifiedDate();
        } else {
            // Use a weak ETag for the LDP-RS
            etag = new EntityTag(resource.getEtagValue(), true);
            date = resource.getLastModifiedDate();
        }

        if (!etag.getValue().isEmpty()) {
            servletResponse.addHeader("ETag", etag.toString());
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
     * @param session the session
     */
    protected void evaluateRequestPreconditions(final Request request,
                                                       final HttpServletResponse servletResponse,
                                                       final FedoraResource resource,
                                                       final HttpSession session) {
        evaluateRequestPreconditions(request, servletResponse, resource, session, false);
    }

    @VisibleForTesting
    void evaluateRequestPreconditions(final Request request,
                                                     final HttpServletResponse servletResponse,
                                                     final FedoraResource resource,
                                                     final HttpSession session,
                                                     final boolean cacheControl) {

        if (session.isBatchSession()) {
            // Force cache revalidation if in a transaction
            servletResponse.addHeader(CACHE_CONTROL, "must-revalidate");
            servletResponse.addHeader(CACHE_CONTROL, "max-age=0");
            return;
        }

        final EntityTag etag;
        final Instant date;
        Instant roundedDate = Instant.now();

        // See the related note about the next block of code in the
        // ContentExposingResource::addCacheControlHeaders method
        if (resource instanceof FedoraBinary) {
            // Use a strong ETag for the LDP-NR
            etag = new EntityTag(resource.getEtagValue());
            date = resource.getLastModifiedDate();
        } else {
            // Use a strong ETag for the LDP-RS when validating If-(None)-Match headers
            etag = new EntityTag(resource.getEtagValue());
            date = resource.getLastModifiedDate();
        }

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
            final Response response = builder.build();
            final Object message = response.getEntity();
            throw new PreconditionException(message != null ? message.toString()
                    : "Request failed due to unspecified failed precondition.", response.getStatus());
        }
    }

    /**
     * Returns an acceptable plain text media type if possible, or null if not.
     * @return an acceptable plain-text media type, or null
     */
    protected MediaType acceptabePlainTextMediaType() {
        final List<MediaType> acceptable = headers.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.size() == 0) {
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
    @SuppressWarnings("resource")
    protected Response createUpdateResponse(final FedoraResource resource, final boolean created) {
        addCacheControlHeaders(servletResponse, resource, session);
        addResourceLinkHeaders(resource, created);
        addExternalContentHeaders(resource);
        addAclHeader(resource);
        addMementoHeaders(resource);

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
                new DefaultRdfStream(asNode(resource), getResourceTriples(resource)),
                session().getFedoraSession().getNamespaces());
            return builder.entity(rdfStream).build();
        }
    }

    protected static MediaType getSimpleContentType(final MediaType requestContentType) {
        return requestContentType != null ?
                new MediaType(requestContentType.getType(), requestContentType.getSubtype())
                : APPLICATION_OCTET_STREAM_TYPE;
    }

    protected static boolean isRdfContentType(final String contentTypeString) {
        return contentTypeToLang(contentTypeString) != null;
    }

    protected void replaceResourceBinaryWithStream(final FedoraBinary result,
                                                   final InputStream requestBodyStream,
                                                   final ContentDisposition contentDisposition,
                                                   final MediaType contentType,
                                                   final Collection<String> checksums,
                                                   final String externalHandling,
                                                   final String externalUrl) throws InvalidChecksumException {
        final Collection<URI> checksumURIs = checksums == null ?
                new HashSet<>() : checksums.stream().map(checksum -> checksumURI(checksum)).collect(Collectors.toSet());
        final String originalFileName = contentDisposition != null ? contentDisposition.getFileName() : "";
        final String originalContentType = contentType != null ? contentType.toString() : "";

        if (externalHandling != null) {
            result.setExternalContent(originalContentType,
                    checksumURIs,
                    originalFileName,
                    externalHandling,
                    externalUrl);
        } else {
            result.setContent(requestBodyStream,
                    originalContentType,
                    checksumURIs,
                    originalFileName,
                    storagePolicyDecisionPoint);
        }
    }

    protected void replaceResourceWithStream(final FedoraResource resource,
                                             final InputStream requestBodyStream,
                                             final MediaType contentType,
                                             final RdfStream resourceTriples) throws MalformedRdfException {
        final Model inputModel = parseBodyAsModel(requestBodyStream, contentType, resource);

        ensureValidMemberRelation(inputModel);

        ensureValidACLAuthorization(resource, inputModel);

        resource.replaceProperties(translator(), inputModel, resourceTriples);
    }

    /**
     * Parse the request body as a Model.
     *
     * @param requestBodyStream rdf request body
     * @param contentType content type of body
     * @param resource the fedora resource
     * @return Model containing triples from request body
     * @throws MalformedRdfException in case rdf json cannot be parsed
     */
    private Model parseBodyAsModel(final InputStream requestBodyStream,
            final MediaType contentType, final FedoraResource resource) throws MalformedRdfException {
        final Lang format = contentTypeToLang(contentType.toString());

        final Model inputModel;
        try {
            inputModel = createDefaultModel();
            inputModel.read(requestBodyStream, getUri(resource).toString(), format.getName().toUpperCase());
            return inputModel;
        } catch (final RiotException e) {
            throw new BadRequestException("RDF was not parsable: " + e.getMessage(), e);

        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                throw new MalformedRdfException(e.getCause());
            }
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * This method throws an exception if the arg model contains a triple with 'ldp:hasMemberRelation' as a predicate
     *   and a server-managed property as the object.
     *
     * @param inputModel to be checked
     * @throws ServerManagedPropertyException
     */
    private void ensureValidMemberRelation(final Model inputModel) throws BadRequestException {
        // check that ldp:hasMemberRelation value is not server managed predicate.
        inputModel.listStatements().forEachRemaining((final Statement s) -> {
            LOGGER.debug("statement: s={}, p={}, o={}", s.getSubject(), s.getPredicate(), s.getObject());

            if (s.getPredicate().equals(HAS_MEMBER_RELATION)) {
                final RDFNode obj = s.getObject();
                if (obj.isURIResource()) {
                    final String uri = obj.asResource().getURI();

                    // Throw exception if object is a server-managed property
                    if (isManagedPredicate.test(createProperty(uri))) {
                            throw new ServerManagedPropertyException(
                                    MessageFormat.format(
                                            "{0} cannot take a server managed property " +
                                                    "as an object: property value = {1}.",
                                            HAS_MEMBER_RELATION, uri));
                    }
                }
            }
        });
    }

    /**
     * This method does two things:
     * - Throws an exception if an authorization has both accessTo and accessToClass
     * - Adds a default accessTo target if an authorization has neither accessTo nor accessToClass
     * 
     * @param resource the fedora resource
     * @param inputModel to be checked and updated
     */
    private void ensureValidACLAuthorization(final FedoraResource resource, final Model inputModel) {
        if (resource.isAcl()) {
            final Set<Node> uniqueAuthSubjects = new HashSet<>();
            inputModel.listStatements().forEachRemaining((final Statement s) -> {
                LOGGER.debug("statement: s={}, p={}, o={}", s.getSubject(), s.getPredicate(), s.getObject());
                final Node subject = s.getSubject().asNode();
                // If subject is Authorization Hash Resource, add it to the map with its accessTo/accessToClass status.
                if (subject.toString().contains("/" + FCR_ACL + "#")) {
                    uniqueAuthSubjects.add(subject);
                }
            });
            final Graph graph = inputModel.getGraph();
            uniqueAuthSubjects.forEach((final Node subject) -> {
                if (graph.contains(subject, WEBAC_ACCESS_TO_URI, Node.ANY) &&
                        graph.contains(subject, WEBAC_ACCESS_TO_CLASS_URI, Node.ANY)) {
                    throw new ACLAuthorizationConstraintViolationException(
                        MessageFormat.format(
                                "Using both accessTo and accessToClass within " +
                                        "a single Authorization is not allowed: {0}.",
                                subject.toString().substring(subject.toString().lastIndexOf("#"))));
                } else if (!(graph.contains(subject, WEBAC_ACCESS_TO_URI, Node.ANY) ||
                        graph.contains(subject, WEBAC_ACCESS_TO_CLASS_URI, Node.ANY))) {
                    inputModel.add(createDefaultAccessToStatement(subject.toString()));
                }
            });
        }
    }

    /**
     * Returns a Statement with the resource containing the acl to be the accessTo target for the given auth subject.
     * 
     * @param authSubject - acl authorization subject uri string
     * @return
     */
    private Statement createDefaultAccessToStatement(final String authSubject) {
        final String currentResourcePath = authSubject.substring(0, authSubject.indexOf("/" + FCR_ACL));
        return createStatement(
                        createResource(authSubject),
                        WEBAC_ACCESS_TO_PROPERTY,
                        createResource(currentResourcePath));
    }

    protected void patchResourcewithSparql(final FedoraResource resource,
            final String requestBody,
            final RdfStream resourceTriples) {
        resource.updateProperties(translator(), requestBody, resourceTriples);
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
            return MediaType.valueOf(((FedoraBinary) resource).getMimeType());
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Syntactically incorrect MediaType encountered on resource {}: '{}'",
                    resource.getPath(), ((FedoraBinary)resource).getMimeType());
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }

    /**
     * Create a checksum URI object.
     **/
    protected static URI checksumURI( final String checksum ) {
        if (!isBlank(checksum)) {
            return URI.create(checksum);
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
     * Check if a path has a segment prefixed with fedora:
     *
     * @param externalPath the path.
     */
    protected static void hasRestrictedPath(final String externalPath) {
        final String[] pathSegments = externalPath.split("/");
        if (Arrays.asList(pathSegments).stream().anyMatch(p -> p.startsWith("fedora:"))) {
            throw new ServerManagedTypeException("Path cannot contain a fedora: prefixed segment.");
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
    protected static Collection<String> parseDigestHeader(final String digest) throws UnsupportedAlgorithmException {
        try {
            final Map<String, String> digestPairs = RFC3230_SPLITTER.split(nullToEmpty(digest));
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
            throw new RepositoryRuntimeException(rootThrowable);
        }
    }
}
