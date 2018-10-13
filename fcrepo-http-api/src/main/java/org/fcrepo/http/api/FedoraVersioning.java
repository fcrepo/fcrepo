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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.core.Response.ok;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_LINK_FORMAT;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_RFC_1123_FORMATTER;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.ItemExistsException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import com.google.common.annotations.VisibleForTesting;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.LinkFormatStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MementoDatetimeFormatException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * @author cabeer
 * @since 9/25/14
 */
@Scope("request")
@Path("/{path: .*}/fcr:versions")
public class FedoraVersioning extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraVersioning.class);

    @VisibleForTesting
    public static final String MEMENTO_DATETIME_HEADER = "Memento-Datetime";

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    @PathParam("path") protected String externalPath;


    /**
     * Default JAX-RS entry point
     */
    public FedoraVersioning() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraVersioning(final String externalPath) {
        this.externalPath = externalPath;
    }

    /**
     * Create a new version of a resource. If a memento-datetime header is provided, then the new version will be
     * based off the provided body using that datetime. If one was not provided, then a version is created based off
     * the current version of the resource.
     *
     * @param datetimeHeader memento-datetime header
     * @param requestContentType Content-Type of the request body
     * @param digest digests of the request body
     * @param requestBodyStream request body stream
     * @return response
     * @throws InvalidChecksumException thrown if one of the provided digests does not match the content
     * @throws MementoDatetimeFormatException if the header value of memento-datetime is not RFC-1123 format
     */
    @POST
    public Response addVersion(@HeaderParam(MEMENTO_DATETIME_HEADER) final String datetimeHeader,
            @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
            @HeaderParam("Digest") final String digest,
            final InputStream requestBodyStream,
            @HeaderParam(LINK) final List<String> rawLinks)
            throws InvalidChecksumException, MementoDatetimeFormatException {

        final FedoraResource resource = resource();
        final FedoraResource timeMap = resource.getTimeMap();

        final AcquiredLock lock = lockManager.lockForWrite(timeMap.getPath(),
            session.getFedoraSession(), nodeService);

        try {
            final MediaType contentType = getSimpleContentType(requestContentType);

            final String slug = headers.getHeaderString("Slug");
            if (slug != null) {
                throw new BadRequestException("Slug header is no longer supported for versioning label. "
                        + "Please use " + MEMENTO_DATETIME_HEADER + " header with RFC-1123 date-time.");
            }

            final Instant mementoInstant;
            try {
                mementoInstant = (isBlank(datetimeHeader) ? Instant.now()
                    : Instant.from(MEMENTO_RFC_1123_FORMATTER.parse(datetimeHeader)));
            } catch (final DateTimeParseException e) {
                throw new MementoDatetimeFormatException("Invalid memento date-time value. "
                        + "Please use RFC-1123 date-time format, such as 'Tue, 3 Jun 2008 11:05:30 GMT'", e);
            }

            final boolean createFromExisting = isBlank(datetimeHeader);

            try {
                LOGGER.debug("Request to add version for date '{}' for '{}'", datetimeHeader, externalPath);

                // Create memento
                FedoraResource memento = null;
                final boolean isBinary = resource instanceof FedoraBinary;
                if (isBinary) {
                    final FedoraBinary binaryResource = (FedoraBinary) resource;
                    if (createFromExisting) {
                        memento = versionService.createBinaryVersion(session.getFedoraSession(),
                                binaryResource, mementoInstant, storagePolicyDecisionPoint);
                    } else {
                        final List<String> links = unpackLinks(rawLinks);
                        final ExternalContentHandler extContent = extContentHandlerFactory.createFromLinks(links);

                        memento = createBinaryMementoFromRequest(binaryResource, mementoInstant,
                                requestBodyStream, extContent, digest);
                    }
                }
                // Create rdf memento if the request resource was an rdf resource or a binary from the
                // current version of the original resource.
                if (!isBinary || createFromExisting) {
                    // Version the description in case the original is a binary
                    final FedoraResource originalResource = resource().getDescription();
                    final InputStream bodyStream = createFromExisting ? null : requestBodyStream;
                    final Lang format = createFromExisting ? null : contentTypeToLang(contentType.toString());
                    if (!createFromExisting && format == null) {
                        throw new ClientErrorException("Invalid Content Type " + contentType.toString(),
                                UNSUPPORTED_MEDIA_TYPE);
                    }

                    final FedoraResource rdfMemento = versionService.createVersion(session.getFedoraSession(),
                            originalResource, idTranslator, mementoInstant, bodyStream, format);
                    // If a binary memento was also generated, use the binary in the response
                    if (!isBinary) {
                        memento = rdfMemento;
                    }
                }

                session.commit();
                return createUpdateResponse(memento, true);
            } catch (final Exception e) {
                checkForInsufficientStorageException(e, e);
                return null; // not reachable
            }
        } catch (final RepositoryRuntimeException e) {
            if (e.getCause() instanceof ItemExistsException) {
                throw new ClientErrorException("Memento with provided datetime already exists",
                        CONFLICT);
            } else {
                throw e;
            }
        } finally {
            lock.release();
        }
    }

    private FedoraBinary createBinaryMementoFromRequest(final FedoraBinary binaryResource,
            final Instant mementoInstant,
            final InputStream requestBodyStream,
            final ExternalContentHandler extContent,
            final String digest) throws InvalidChecksumException, UnsupportedAlgorithmException {

        final Collection<String> checksums = parseDigestHeader(digest);
        final Collection<URI> checksumURIs = checksums == null ? new HashSet<>() : checksums.stream().map(
                checksum -> checksumURI(checksum)).collect(Collectors.toSet());

        // Create internal binary either from supplied body or copy external uri
        if (extContent == null || extContent.isCopy()) {
            InputStream contentStream = requestBodyStream;
            if (extContent != null) {
                contentStream = extContent.fetchExternalContent();
            }

            return versionService.createBinaryVersion(session.getFedoraSession(), binaryResource,
                    mementoInstant, contentStream, checksumURIs, storagePolicyDecisionPoint);
        } else {
            return versionService.createExternalBinaryVersion(session.getFedoraSession(), binaryResource,
                    mementoInstant, checksumURIs, extContent.getHandling(), extContent.getURL());
        }
    }

    /**
     * Get the list of versions for the object
     *
     * @param rangeValue starting and ending byte offsets
     * @param acceptValue the rdf media-type
     * @return List of versions for the object as RDF
     * @throws IOException in case of error extracting content
     */
    @GET
    @HtmlTemplate(value = "fcr:versions")
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
        N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
        TURTLE_X, TEXT_HTML_WITH_CHARSET, APPLICATION_LINK_FORMAT })
    public Response getVersionList(@HeaderParam("Range") final String rangeValue,
        @HeaderParam("Accept") final String acceptValue) throws IOException {

        final FedoraResource theTimeMap = resource().getTimeMap();
        checkCacheControlHeaders(request, servletResponse, theTimeMap, session);

        LOGGER.debug("GET resource '{}'", externalPath);

        addResourceHttpHeaders(theTimeMap);

        if (acceptValue != null && acceptValue.equalsIgnoreCase(APPLICATION_LINK_FORMAT)) {
            final URI parentUri = getUri(resource());
            final List<Link> versionLinks = new ArrayList<>();
            versionLinks.add(Link.fromUri(parentUri).rel("original").build());
            versionLinks.add(Link.fromUri(parentUri).rel("timegate").build());
            // So we don't collect the children twice, store them in an array.
            final FedoraResource[] children = theTimeMap.getChildren().toArray(FedoraResource[]::new);

            Arrays.stream(children).forEach(t -> {
                final URI childUri = getUri(t);
                versionLinks.add(Link.fromUri(childUri).rel("memento")
                                     .param("datetime",
                        MEMENTO_RFC_1123_FORMATTER.format(t.getMementoDatetime()))
                                     .build());
            });
            // Based on the dates of the above mementos, add the range to the below link.
            final Instant[] Mementos = Arrays.stream(children).map(FedoraResource::getMementoDatetime)
                .sorted(Comparator.naturalOrder())
                .toArray(Instant[]::new);
            final Builder linkBuilder =
                Link.fromUri(parentUri + "/" + FCR_VERSIONS).rel("self").type(APPLICATION_LINK_FORMAT);
            if (Mementos.length >= 2) {
                // There are 2 or more Mementos so make a range.
                linkBuilder.param("from", MEMENTO_RFC_1123_FORMATTER.format(Mementos[0].atZone(ZoneId.of("UTC"))));
                linkBuilder.param("until",
                    MEMENTO_RFC_1123_FORMATTER.format(Mementos[Mementos.length - 1].atZone(ZoneId.of("UTC"))));
            }
            versionLinks.add(linkBuilder.build());
            return ok(new LinkFormatStream(versionLinks.stream())).build();
        } else {
            final AcquiredLock readLock = lockManager.lockForRead(theTimeMap.getPath());
            try (final RdfStream rdfStream = new DefaultRdfStream(asNode(theTimeMap))) {
                return getContent(rangeValue, getChildrenLimit(), rdfStream, theTimeMap);
            } finally {
                readLock.release();
            }
        }
    }

    /**
     * Outputs information about the supported HTTP methods, etc.
     *
     * @return the information about the supported HTTP methods, etc.
     */
    @OPTIONS
    public Response options() {
        final FedoraResource theTimeMap = resource().getTimeMap();
        LOGGER.info("OPTIONS for '{}'", externalPath);
        addResourceHttpHeaders(theTimeMap);
        return ok().build();
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }
}
