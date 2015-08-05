/**
 * Copyright 2015 DuraSpace, Inc.
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


import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.modeshape.rdf.ManagedRdf.isManagedTriple;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.jena.atlas.RuntimeIOException;
import org.fcrepo.http.commons.api.rdf.HttpTripleUtil;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.AclRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.SkolemNodeRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ContentRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.HashRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpContainerRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpIsMemberOfRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.RootRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.modeshape.services.TransactionServiceImpl;

import org.apache.jena.riot.Lang;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.jvnet.hk2.annotations.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

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

    @BeanParam
    protected MultiPrefer prefer;

    @Inject
    @Optional
    StoragePolicyDecisionPoint storagePolicyDecisionPoint;

    protected FedoraResource resource;

    private static final long MAX_BUFFER_SIZE = 10240000;

    private static final Predicate<Triple> IS_MANAGED_TYPE = t -> t.getPredicate().equals(type.asNode()) &&
            isManagedNamespace.test(t.getObject().getNameSpace());

    protected abstract String externalPath();

    protected Response getContent(final String rangeValue,
                                  final RdfStream rdfStream) throws IOException {
        if (resource() instanceof FedoraBinary) {

            final String contentTypeString = ((FedoraBinary) resource()).getMimeType();

            final Lang lang = contentTypeToLang(contentTypeString);

            if (!contentTypeString.equals("text/plain") && lang != null) {

                final String format = lang.getName().toUpperCase();

                final InputStream content = ((FedoraBinary) resource()).getContent();

                final Model inputModel = createDefaultModel()
                        .read(content,  (resource()).toString(), format);

                rdfStream.concat(Iterators.transform(inputModel.listStatements(), Statement::asTriple));
            } else {

                final MediaType mediaType = MediaType.valueOf(contentTypeString);
                if (MESSAGE_EXTERNAL_BODY.isCompatible(mediaType)
                        && mediaType.getParameters().containsKey("access-type")
                        && mediaType.getParameters().get("access-type").equals("URL")
                        && mediaType.getParameters().containsKey("URL") ) {
                    try {
                        return temporaryRedirect(new URI(mediaType.getParameters().get("URL"))).build();
                    } catch (final URISyntaxException e) {
                        throw new RepositoryRuntimeException(e);
                    }
                }
                return getBinaryContent(rangeValue);
            }

        } else {
            rdfStream.concat(getResourceTriples());

            if (prefer != null) {
                prefer.getReturn().addResponseHeaders(servletResponse);
            }

        }
        servletResponse.addHeader("Vary", "Accept, Range, Accept-Encoding, Accept-Language");

        return Response.ok(rdfStream).build();
    }

    protected RdfStream getResourceTriples() {

        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else if (prefer != null && prefer.hasHandling()) {
            returnPreference = prefer.getHandling();
        } else {
            returnPreference = PreferTag.emptyTag();
        }

        final LdpPreferTag ldpPreferences = new LdpPreferTag(returnPreference);

        final RdfStream rdfStream = new RdfStream();

        final Predicate<Triple> tripleFilter;
        if (ldpPreferences.prefersServerManaged()) {
            tripleFilter = x -> true;
        } else {
            tripleFilter = IS_MANAGED_TYPE.or(isManagedTriple).negate();
        }

        if (ldpPreferences.prefersServerManaged()) {
            rdfStream.concat(getTriples(LdpRdfContext.class));
        }

        rdfStream.concat(filter(getTriples(TypeRdfContext.class), tripleFilter::test));

        rdfStream.concat(filter(getTriples(PropertiesRdfContext.class), tripleFilter::test));

        if (!returnPreference.getValue().equals("minimal")) {

            // Additional server-managed triples about this resource
            if (ldpPreferences.prefersServerManaged()) {
                rdfStream.concat(getTriples(AclRdfContext.class));
                rdfStream.concat(getTriples(RootRdfContext.class));
                rdfStream.concat(getTriples(ContentRdfContext.class));
                rdfStream.concat(getTriples(ParentRdfContext.class));
            }

            // containment triples about this resource
            if (ldpPreferences.prefersContainment()) {
                rdfStream.concat(getTriples(ChildrenRdfContext.class));
            }

            // LDP container membership triples for this resource
            if (ldpPreferences.prefersMembership()) {
                rdfStream.concat(getTriples(LdpContainerRdfContext.class));
                rdfStream.concat(getTriples(LdpIsMemberOfRdfContext.class));
            }

            // Include binary properties if this is a binary description
            if (resource() instanceof NonRdfSourceDescription) {
                final FedoraResource described = ((NonRdfSourceDescription) resource()).getDescribedResource();
                rdfStream.concat(filter(described.getTriples(translator(), ImmutableList.of(TypeRdfContext.class,
                        PropertiesRdfContext.class,
                        ContentRdfContext.class)), tripleFilter::test));
                if (ldpPreferences.prefersServerManaged()) {
                    rdfStream.concat(getTriples(described,LdpRdfContext.class));
                }
            }

            // Embed all hash and blank nodes
            rdfStream.concat(filter(getTriples(HashRdfContext.class), tripleFilter::test));
            rdfStream.concat(filter(getTriples(SkolemNodeRdfContext.class), tripleFilter::test));

            // Include inbound references to this object
            if (ldpPreferences.prefersReferences()) {
                rdfStream.concat(getTriples(ReferencesRdfContext.class));
            }

            // Embed the children of this object
            if (ldpPreferences.prefersEmbed()) {

                final Iterator<FedoraResource> children = resource().getChildren();

                rdfStream.concat(filter(concat(transform(children, child ->
                child.getTriples(translator(),
                        ImmutableList.of(
                                TypeRdfContext.class, PropertiesRdfContext.class, SkolemNodeRdfContext.class)))),
                        tripleFilter::test));

            }
        }

        if (httpTripleUtil != null && ldpPreferences.prefersServerManaged()) {
            httpTripleUtil.addHttpComponentModelsForResourceToStream(rdfStream, resource(), uriInfo, translator());
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
                    final long rangeStart = range.start();
                    final long rangeSize = range.size() == -1 ? contentSize - rangeStart : range.size();
                    final long remainingBytes = contentSize - rangeStart;
                    final long bufSize = rangeSize < remainingBytes ? rangeSize : remainingBytes;

                    if (bufSize < MAX_BUFFER_SIZE) {
                        // Small size range content retrieval use javax.jcr.Binary to improve performance
                        final byte[] buf = new byte[(int) bufSize];

                        final Binary binaryContent = binary.getBinaryContent();
                        try {
                            binaryContent.read(buf, rangeStart);
                        } catch (final RepositoryException e1) {
                            throw new RepositoryRuntimeException(e1);
                        }
                        binaryContent.dispose();

                        builder = status(PARTIAL_CONTENT).entity(buf)
                                .header("Content-Range", contentRangeValue);
                    } else {
                        // For large range content retrieval, go with the InputStream class to balance
                        // the memory usage, though this is a rare case in range content retrieval.
                        final InputStream content = binary.getContent();
                        final RangeRequestInputStream rangeInputStream =
                                new RangeRequestInputStream(content, range.start(), range.size());

                        builder = status(PARTIAL_CONTENT).entity(rangeInputStream)
                                .header("Content-Range", contentRangeValue);
                    }
                }

            } else {
                final InputStream content = binary.getContent();
                builder = ok(content);
            }


            // we set the content-type explicitly to avoid content-negotiation from getting in the way
            return builder.type(binary.getMimeType())
                    .cacheControl(cc)
                    .build();

        }

    protected RdfStream getTriples(final Class<? extends RdfStream> x) {
        return getTriples(resource(), x);
    }

    protected RdfStream getTriples(final FedoraResource resource, final Class<? extends RdfStream> x) {
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
        if (resource == null) {
            resource = getResourceFromPath(externalPath());
        }

        return resource;
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

        if (resource instanceof NonRdfSource) {
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

        final FedoraResource mutableResource = resource instanceof NonRdfSourceDescription
                ? ((NonRdfSourceDescription) resource).getDescribedResource() : resource;
        final EntityTag etag = new EntityTag(mutableResource.getEtagValue());
        final Date date = mutableResource.getLastModifiedDate();

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

        final EntityTag etag = new EntityTag(resource.getEtagValue());
        final Date date = resource.getLastModifiedDate();
        final Date roundedDate = new Date();

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

        } catch (RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                throw new MalformedRdfException(e.getCause());
            }
            throw new RepositoryRuntimeException(e);
        }

        resource.replaceProperties(translator(), inputModel, resourceTriples);
    }

    protected void patchResourcewithSparql(final FedoraResource resource,
            final String requestBody,
            final RdfStream resourceTriples)
                    throws MalformedRdfException, AccessDeniedException {
        if (resource instanceof NonRdfSourceDescription) {
            // update the described resource instead
            ((NonRdfSourceDescription) resource).getDescribedResource()
                    .updateProperties(translator(), requestBody, resourceTriples);
        } else {
            resource.updateProperties(translator(), requestBody, resourceTriples);
        }
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
