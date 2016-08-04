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


import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.EnumSet.of;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;

import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicateURI;
import static org.fcrepo.kernel.api.RequiredRdfContext.EMBED_RESOURCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_CONTAINMENT;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_MEMBERSHIP;
import static org.fcrepo.kernel.api.RequiredRdfContext.MINIMAL;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.riot.RiotException;
import org.fcrepo.http.commons.api.HttpHeaderInjector;
import org.fcrepo.http.commons.api.rdf.HttpTripleUtil;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.modeshape.services.TransactionServiceImpl;

import org.apache.jena.riot.Lang;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.jvnet.hk2.annotations.Optional;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * An abstract class that sits between AbstractResource and any resource that
 * wishes to share the routines for building responses containing binary
 * content.
 *
 * @author Mike Durbin
 * @author ajs6f
 */
public abstract class ContentExposingResource extends FedoraBaseResource {

    public static final MediaType MESSAGE_EXTERNAL_BODY = MediaType.valueOf("message/external-body");

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;

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

    protected FedoraResource resource;

    private static final Predicate<Triple> IS_MANAGED_TYPE = t -> t.getPredicate().equals(type.asNode()) &&
            isManagedNamespace.test(t.getObject().getNameSpace());
    private static final Predicate<Triple> IS_MANAGED_TRIPLE = IS_MANAGED_TYPE
        .or(t -> isManagedPredicateURI.test(t.getPredicate().getURI()));

    protected abstract String externalPath();

    protected Response getContent(final String rangeValue,
                                  final RdfStream rdfStream) throws IOException {
        return getContent(rangeValue, -1, rdfStream);
    }

    /**
     * This method returns an HTTP response with content body appropriate to the following arguments.
     *
     * @param rangeValue starting and ending byte offsets, see {@link Range}
     * @param limit is the number of child resources returned in the response, -1 for all
     * @param rdfStream to which response RDF will be concatenated
     * @return HTTP response
     * @throws IOException in case of error extracting content
     */
    protected Response getContent(final String rangeValue,
                                  final int limit,
                                  final RdfStream rdfStream) throws IOException {

        final RdfNamespacedStream outputStream;

        if (resource() instanceof FedoraBinary) {

            final MediaType mediaType = MediaType.valueOf(((FedoraBinary) resource()).getMimeType());

            if (MESSAGE_EXTERNAL_BODY.isCompatible(mediaType) && mediaType.getParameters().containsKey(
                    "access-type") && mediaType.getParameters().get("access-type").equals("URL") && mediaType
                    .getParameters().containsKey("URL")) {
                return temporaryRedirect(URI.create(mediaType.getParameters().get("URL"))).build();
            }

            return getBinaryContent(rangeValue);
        } else {
            outputStream = new RdfNamespacedStream(
                    new DefaultRdfStream(rdfStream.topic(), concat(rdfStream,
                        getResourceTriples(limit))),
                    namespaceService.getNamespaces(session()));
            if (prefer != null) {
                prefer.getReturn().addResponseHeaders(servletResponse);
            }
        }
        servletResponse.addHeader("Vary", "Accept, Range, Accept-Encoding, Accept-Language");

        return ok(outputStream).build();
    }

    protected RdfStream getResourceTriples() {
        return getResourceTriples(-1);
    }

    /**
     * This method returns a stream of RDF triples associated with this target resource
     *
     * @param limit is the number of child resources returned in the response, -1 for all
     * @return {@link RdfStream}
     */
    protected RdfStream getResourceTriples(final int limit) {
        // use the thing described, not the description, for the subject of descriptive triples
        if (resource() instanceof NonRdfSourceDescription) {
            resource = resource().getDescribedResource();
        }
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
            streams.add(getTriples(of(PROPERTIES, MINIMAL)).filter(tripleFilter));

            if (ldpPreferences.prefersServerManaged()) {
                streams.add(getTriples(of(SERVER_MANAGED, MINIMAL)));
            }
        } else {
            streams.add(getTriples(PROPERTIES).filter(tripleFilter));

            // Additional server-managed triples about this resource
            if (ldpPreferences.prefersServerManaged()) {
                streams.add(getTriples(SERVER_MANAGED));
            }

            // containment triples about this resource
            if (ldpPreferences.prefersContainment()) {
                if (limit == -1) {
                    streams.add(getTriples(LDP_CONTAINMENT));
                } else {
                    streams.add(getTriples(LDP_CONTAINMENT).limit(limit));
                }
            }

            // LDP container membership triples for this resource
            if (ldpPreferences.prefersMembership()) {
                streams.add(getTriples(LDP_MEMBERSHIP));
            }

            // Include inbound references to this object
            if (ldpPreferences.prefersReferences()) {
                streams.add(getTriples(INBOUND_REFERENCES));
            }

            // Embed the children of this object
            if (ldpPreferences.prefersEmbed()) {
                streams.add(getTriples(EMBED_RESOURCES));
            }
        }

        final RdfStream rdfStream = new DefaultRdfStream(
                asNode(resource()), streams.stream().reduce(empty(), Stream::concat));

        if (httpTripleUtil != null && ldpPreferences.prefersServerManaged()) {
            return httpTripleUtil.addHttpComponentModelsForResourceToStream(rdfStream, resource(), uriInfo,
                    translator());
        }

        return rdfStream;
    }

    /**
     * Get the binary content of a datastream
     *
     * @param rangeValue the range value
     * @return Binary blob
     * @throws IOException if io exception occurred
     */
    protected Response getBinaryContent(final String rangeValue)
            throws IOException {
            final FedoraBinary binary = (FedoraBinary)resource();

            // we include an explicit etag, because the default behavior is to use the JCR node's etag, not
            // the jcr:content node digest. The etag is only included if we are not within a transaction.
            final String txId = TransactionServiceImpl.getCurrentTransactionId(session());
            if (txId == null) {
                checkCacheControlHeaders(request, servletResponse, binary, session());
            }
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            Response.ResponseBuilder builder;

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
                            .header("Content-Range", contentRangeValue);
                }

            } else {
                @SuppressWarnings("resource")
                final InputStream content = binary.getContent();
                builder = ok(content);
            }


            // we set the content-type explicitly to avoid content-negotiation from getting in the way
            return builder.type(binary.getMimeType())
                    .cacheControl(cc)
                    .build();

        }

    protected RdfStream getTriples(final Set<? extends TripleCategory> x) {
        return getTriples(resource(), x);
    }

    protected RdfStream getTriples(final FedoraResource resource, final Set<? extends TripleCategory> x) {
        return resource.getTriples(translator(), x);
    }

    protected RdfStream getTriples(final TripleCategory x) {
        return getTriples(resource(), x);
    }

    protected RdfStream getTriples(final FedoraResource resource, final TripleCategory x) {
        return resource.getTriples(translator(), x);
    }

    protected URI getUri(final FedoraResource resource) {
        try {
            final String uri = resource.asUri(translator()).getURI();
            return new URI(uri);
        } catch (final URISyntaxException e) {
            throw new BadRequestException(e);
        }
    }

    protected FedoraResource resource() {
        if (resource == null) {
            resource = getResourceFromPath(externalPath());
        }
        return resource;
    }

    protected void addResourceLinkHeaders(final FedoraResource resource) {
        addResourceLinkHeaders(resource, false);
    }

    protected void addResourceLinkHeaders(final FedoraResource resource, final boolean includeAnchor) {
        if (resource instanceof NonRdfSourceDescription) {
            final URI uri = getUri(resource.getDescribedResource());
            final Link link = Link.fromUri(uri).rel("describes").build();
            servletResponse.addHeader("Link", link.toString());
        } else if (resource instanceof FedoraBinary) {
            final URI uri = getUri(resource.getDescription());
            final Link.Builder builder = Link.fromUri(uri).rel("describedby");

            if (includeAnchor) {
                builder.param("anchor", getUri(resource).toString());
            }
            servletResponse.addHeader("Link", builder.build().toString());
        }
    }

    /**
     * Add any resource-specific headers to the response
     * @param resource the resource
     */
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        if (resource instanceof FedoraBinary) {

            final FedoraBinary binary = (FedoraBinary)resource;
            final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                    .fileName(binary.getFilename())
                    .creationDate(binary.getCreatedDate())
                    .modificationDate(binary.getLastModifiedDate())
                    .size(binary.getContentSize())
                    .build();

            servletResponse.addHeader("Content-Type", binary.getMimeType());
            servletResponse.addHeader("Content-Length", String.valueOf(binary.getContentSize()));
            servletResponse.addHeader("Accept-Ranges", "bytes");
            servletResponse.addHeader("Content-Disposition", contentDisposition.toString());
        }

        servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"");

        if (resource instanceof FedoraBinary) {
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "NonRDFSource>;rel=\"type\"");
        } else if (resource instanceof Container) {
            servletResponse.addHeader("Link", "<" + CONTAINER.getURI() + ">;rel=\"type\"");
            if (resource.hasType(LDP_BASIC_CONTAINER)) {
                servletResponse.addHeader("Link", "<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\"");
            } else if (resource.hasType(LDP_DIRECT_CONTAINER)) {
                servletResponse.addHeader("Link", "<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\"");
            } else if (resource.hasType(LDP_INDIRECT_CONTAINER)) {
                servletResponse.addHeader("Link", "<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\"");
            } else {
                servletResponse.addHeader("Link", "<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\"");
            }
        } else {
            servletResponse.addHeader("Link", "<" + LDP_NAMESPACE + "RDFSource>;rel=\"type\"");
        }
        if (httpHeaderInject != null) {
            httpHeaderInject.addHttpHeaderToResponseStream(servletResponse, uriInfo, resource());
        }

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
    protected static void checkCacheControlHeaders(final Request request,
                                                   final HttpServletResponse servletResponse,
                                                   final FedoraResource resource,
                                                   final Session session) {
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
    protected static void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final FedoraResource resource,
                                                 final Session session) {

        final String txId = TransactionServiceImpl.getCurrentTransactionId(session);
        if (txId != null) {
            // Do not add caching headers if in a transaction
            return;
        }

        final EntityTag etag;
        final Date date;

        // See note about this code in the javadoc above.
        if (resource instanceof FedoraBinary) {
            // Use a strong ETag for LDP-NR
            etag = new EntityTag(resource.getDescription().getEtagValue());
            date = resource.getDescription().getLastModifiedDate();
        } else {
            // Use a weak ETag for the LDP-RS
            etag = new EntityTag(resource.getDescribedResource().getEtagValue(), true);
            date = resource.getDescribedResource().getLastModifiedDate();
        }

        if (!etag.getValue().isEmpty()) {
            servletResponse.addHeader("ETag", etag.toString());
        }

        if (date != null) {
            servletResponse.addDateHeader("Last-Modified", date.getTime());
        }
    }

    /**
     * Evaluate request preconditions to ensure the resource is the expected state
     * @param request the request
     * @param servletResponse the servlet response
     * @param resource the resource
     * @param session the session
     */
    protected static void evaluateRequestPreconditions(final Request request,
                                                       final HttpServletResponse servletResponse,
                                                       final FedoraResource resource,
                                                       final Session session) {
        evaluateRequestPreconditions(request, servletResponse, resource, session, false);
    }

    private static void evaluateRequestPreconditions(final Request request,
                                                     final HttpServletResponse servletResponse,
                                                     final FedoraResource resource,
                                                     final Session session,
                                                     final boolean cacheControl) {

        final String txId = TransactionServiceImpl.getCurrentTransactionId(session);
        if (txId != null) {
            // Force cache revalidation if in a transaction
            servletResponse.addHeader(CACHE_CONTROL, "must-revalidate");
            servletResponse.addHeader(CACHE_CONTROL, "max-age=0");
            return;
        }

        final EntityTag etag;
        final Date date;
        final Date roundedDate = new Date();

        // See the related note about the next block of code in the
        // ContentExposingResource::addCacheControlHeaders method
        if (resource instanceof FedoraBinary) {
            // Use a strong ETag for the LDP-NR
            etag = new EntityTag(resource.getDescription().getEtagValue());
            date = resource.getDescription().getLastModifiedDate();
        } else {
            // Use a strong ETag for the LDP-RS when validating If-(None)-Match headers
            etag = new EntityTag(resource.getDescribedResource().getEtagValue());
            date = resource.getDescribedResource().getLastModifiedDate();
        }

        if (date != null) {
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);
        }

        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
        if ( builder != null ) {
            builder = builder.entity("ETag mismatch");
        } else {
            builder = request.evaluatePreconditions(roundedDate);
            if ( builder != null ) {
                builder = builder.entity("Date mismatch");
            }
        }

        if (builder != null && cacheControl ) {
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            // here we are implicitly emitting a 304
            // the exception is not an error, it's genuinely
            // an exceptional condition
            builder = builder.cacheControl(cc).lastModified(date).tag(etag);
        }
        if (builder != null) {
            throw new WebApplicationException(builder.build());
        }
    }

    protected static MediaType getSimpleContentType(final MediaType requestContentType) {
        return requestContentType != null ? new MediaType(requestContentType.getType(), requestContentType.getSubtype())
                : APPLICATION_OCTET_STREAM_TYPE;
    }

    protected static boolean isRdfContentType(final String contentTypeString) {
        return contentTypeToLang(contentTypeString) != null;
    }

    protected void replaceResourceBinaryWithStream(final FedoraBinary result,
                                                   final InputStream requestBodyStream,
                                                   final ContentDisposition contentDisposition,
                                                   final MediaType contentType,
                                                   final String checksum) throws InvalidChecksumException {
        final URI checksumURI = checksumURI(checksum);
        final String originalFileName = contentDisposition != null ? contentDisposition.getFileName() : "";
        final String originalContentType = contentType != null ? contentType.toString() : "";

        result.setContent(requestBodyStream,
                originalContentType,
                checksumURI,
                originalFileName,
                storagePolicyDecisionPoint);
    }

    protected void replaceResourceWithStream(final FedoraResource resource,
                                             final InputStream requestBodyStream,
                                             final MediaType contentType,
                                             final RdfStream resourceTriples) throws MalformedRdfException {
        final Lang format = contentTypeToLang(contentType.toString());

        final Model inputModel = createDefaultModel();
        try {
            inputModel.read(requestBodyStream, getUri(resource).toString(), format.getName().toUpperCase());

        } catch (final RiotException e) {
            throw new BadRequestException("RDF was not parsable: " + e.getMessage(), e);

        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                throw new MalformedRdfException(e.getCause());
            }
            throw new RepositoryRuntimeException(e);
        }

        resource.replaceProperties(translator(), inputModel, resourceTriples);
    }

    protected void patchResourcewithSparql(final FedoraResource resource,
            final String requestBody,
            final RdfStream resourceTriples) {
        resource.getDescribedResource().updateProperties(translator(), requestBody, resourceTriples);
    }

    /**
     * Create a checksum URI object.
     **/
    private static URI checksumURI( final String checksum ) {
        if (!isBlank(checksum)) {
            return URI.create(checksum);
        }
        return null;
    }
}
