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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.commons.api.rdf.HttpTripleUtil;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.ManagedRdf;
import org.fcrepo.kernel.impl.rdf.impl.AclRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.HashRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.LdpIsMemberOfRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.RootRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.impl.services.TransactionServiceImpl;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.jvnet.hk2.annotations.Optional;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.kernel.RdfLexicon.isManagedNamespace;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An abstract class that sits between AbstractResource and any resource that
 * wishes to share the routines for building responses containing binary
 * content.
 *
 * @author Mike Durbin
 */
public abstract class ContentExposingResource extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(ContentExposingResource.class);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;

    @Inject
    @Optional
    private HttpTripleUtil httpTripleUtil;

    @Inject
    @Optional
    StoragePolicyDecisionPoint storagePolicyDecisionPoint;

    protected FedoraResource resource;

    private static long MAX_BUFFER_SIZE = 10240000;

    protected abstract String externalPath();

    protected abstract void addResourceHttpHeaders(FedoraResource resource);

    protected Response getContent(final Prefer prefer,
                                  final String rangeValue,
                                  final RdfStream rdfStream) throws IOException {
        if (resource() instanceof FedoraBinary) {

            final String contentTypeString = ((FedoraBinary) resource()).getMimeType();

            final Lang lang = contentTypeToLang(contentTypeString);

            if (!contentTypeString.equals("text/plain") && lang != null) {

                final String format = lang.getName().toUpperCase();

                final InputStream content = ((FedoraBinary) resource()).getContent();

                final Model inputModel = createDefaultModel()
                        .read(content, getUri(resource()).toString(), format);

                rdfStream.concat(Iterators.transform(inputModel.listStatements(),
                        new Function<Statement, Triple>() {

                            @Override
                            public Triple apply(final Statement input) {
                                return input.asTriple();
                            }
                        }));
            } else {
                return getBinaryContent(rangeValue);
            }

        } else {
            rdfStream.concat(getResourceTriples(prefer));

            if (prefer != null) {
                prefer.getReturn().addResponseHeaders(servletResponse);
            }

        }
        servletResponse.addHeader("Vary", "Accept, Range, Accept-Encoding, Accept-Language");

        return Response.ok(rdfStream).build();
    }

    protected RdfStream getResourceTriples(final Prefer prefer) {

        final PreferTag returnPreference;

        if (prefer != null && prefer.hasReturn()) {
            returnPreference = prefer.getReturn();
        } else if (prefer != null && prefer.hasHandling()) {
            returnPreference = prefer.getHandling();
        } else {
            returnPreference = new PreferTag("");
        }

        final LdpPreferTag ldpPreferences = new LdpPreferTag(returnPreference);

        final RdfStream rdfStream = new RdfStream();

        final Predicate<Triple> tripleFilter;
        if (ldpPreferences.prefersServerManaged()) {
            tripleFilter = alwaysTrue();
        } else {
            tripleFilter = not(ManagedRdf.isManagedTriple);
        }

        rdfStream.concat(filter(getTriples(PropertiesRdfContext.class), tripleFilter));


        if (ldpPreferences.prefersServerManaged()) {
            rdfStream.concat(getTriples(TypeRdfContext.class));
        } else {
            rdfStream.concat(filter(getTriples(TypeRdfContext.class), new Predicate<Triple>() {
                @Override
                public boolean apply(final Triple input) {
                    return !isManagedNamespace.apply(input.getObject().getNameSpace());
                }
            }));
        }


        if (httpTripleUtil != null && ldpPreferences.prefersServerManaged()) {
            httpTripleUtil.addHttpComponentModelsForResourceToStream(rdfStream, resource(), uriInfo, translator());
        }

        if (!returnPreference.getValue().equals("minimal")) {

            if (ldpPreferences.prefersServerManaged()) {
                rdfStream.concat(getTriples(AclRdfContext.class));
                rdfStream.concat(getTriples(RootRdfContext.class));
                rdfStream.concat(getTriples(ContentRdfContext.class));
            }

            rdfStream.concat(filter(getTriples(HashRdfContext.class), tripleFilter));

            if (resource() instanceof Datastream) {
                rdfStream.concat(filter(
                        getTriples(((Datastream) resource()).getBinary(), PropertiesRdfContext.class), tripleFilter));
            }

            if (ldpPreferences.prefersReferences()) {
                rdfStream.concat(getTriples(ReferencesRdfContext.class));
            }

            if (ldpPreferences.prefersServerManaged()) {
                rdfStream.concat(getTriples(ParentRdfContext.class));
            }

            if (ldpPreferences.prefersContainment()) {
                rdfStream.concat(getTriples(ChildrenRdfContext.class));
            }

            if (ldpPreferences.prefersMembership()) {
                rdfStream.concat(getTriples(LdpContainerRdfContext.class));
                rdfStream.concat(getTriples(LdpIsMemberOfRdfContext.class));
            }

            if (ldpPreferences.prefersEmbed()) {

                final Iterator<FedoraResource> children = resource().getChildren();

                rdfStream.concat(filter(concat(transform(children,
                        new Function<FedoraResource, RdfStream>() {

                            @Override
                            public RdfStream apply(final FedoraResource child) {
                                return child.getTriples(translator(), PropertiesRdfContext.class);
                            }
                        })), tripleFilter));

            }

            if (ldpPreferences.prefersServerManaged()) {
                rdfStream.concat(getTriples(ContainerRdfContext.class));
            }
        }

        return rdfStream;
    }

    /**
     * Get the binary content of a datastream
     *
     * @return Binary blob
     * @throws RepositoryException
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
                    final long maxBufferSize = MAX_BUFFER_SIZE; // 10MB max buffer size?
                    final long rangeStart = range.start();
                    final long rangeSize = range.size() == -1 ? contentSize - rangeStart : range.size();
                    final long remainingBytes = contentSize - rangeStart;
                    final long bufSize = rangeSize < remainingBytes ? rangeSize : remainingBytes;

                    if (bufSize < maxBufferSize) {
                        // Small size range content retrieval use javax.jcr.Binary to improve performance
                        final byte[] buf = new byte[(int) bufSize];

                        final Binary binaryContent = binary.getBinaryContent();
                        try {
                            binaryContent.read(buf, rangeStart);
                        } catch (RepositoryException e1) {
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

            final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                    .fileName(binary.getFilename())
                    .creationDate(binary.getCreatedDate())
                    .modificationDate(binary.getLastModifiedDate())
                    .size(binary.getContentSize())
                    .build();

            addResourceHttpHeaders(binary);
            return builder.type(binary.getMimeType())
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Disposition", contentDisposition)
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
     * Evaluate the cache control headers for the request to see if it can be served from
     * the cache.
     *
     * @param request
     * @param servletResponse
     * @param resource
     * @param session
     * @throws javax.jcr.RepositoryException
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
     * @param servletResponse
     * @param resource
     */
    protected static void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final FedoraResource resource,
                                                 final Session session) {

        final String txId = TransactionServiceImpl.getCurrentTransactionId(session);
        if (txId != null) {
            // Do not add caching headers if in a transaction
            return;
        }

        final EntityTag etag = new EntityTag(resource.getEtagValue());
        final Date date = resource.getLastModifiedDate();

        if (!etag.getValue().isEmpty()) {
            servletResponse.addHeader("ETag", etag.toString());
        }

        if (date != null) {
            servletResponse.addDateHeader("Last-Modified", date.getTime());
        }
    }

    /**
     * Evaluate request preconditions to ensure the resource is the expected state
     * @param request
     * @param resource
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
                                                   final String contentTypeString,
                                                   final String checksum) throws InvalidChecksumException {
        final URI checksumURI = checksumURI(checksum);
        final String originalFileName = contentDisposition != null ? contentDisposition.getFileName() : "";

        result.setContent(requestBodyStream,
                contentTypeString,
                checksumURI,
                originalFileName,
                storagePolicyDecisionPoint);
    }

    protected void replaceResourceWithStream(final FedoraResource resource,
                                             final InputStream requestBodyStream,
                                             final MediaType contentType,
                                             final RdfStream resourceTriples) {
        final Lang format = contentTypeToLang(contentType.toString());

        final Model inputModel = createDefaultModel()
                .read(requestBodyStream, getUri(resource).toString(), format.getName().toUpperCase());

        resource.replaceProperties(translator(), inputModel, resourceTriples);
    }

    protected void patchResourcewithSparql(final FedoraResource resource,
                                           final String requestBody,
                                           final RdfStream resourceTriples) {
        resource.updateProperties(translator(), requestBody, resourceTriples);
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
