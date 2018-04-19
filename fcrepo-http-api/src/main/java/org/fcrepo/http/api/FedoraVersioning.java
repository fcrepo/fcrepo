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

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.core.Response.noContent;
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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.ItemExistsException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.LinkFormatStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.MementoDatetimeFormatException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryVersionRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.glassfish.jersey.media.multipart.ContentDisposition;
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
     * Disable versioning
     * @return the response
     */
    @DELETE
    public Response disableVersioning() {
        LOGGER.info("Disable versioning for '{}'", externalPath);
        resource().disableVersioning();
        session.commit();
        return noContent().build();
    }

    /**
     * Create a new version of a resource. If a memento-datetime header is provided, then the new version will be
     * based off the provided body using that datetime. If one was not provided, then a version is created based off
     * the current version of the resource.
     *
     * @param datetimeHeader memento-datetime header
     * @param requestContentType Content-Type of the request body
     * @param contentDisposition Content-Disposition
     * @param digest digests of the request body
     * @param requestBodyStream request body stream
     * @return response
     * @throws InvalidChecksumException thrown if one of the provided digests does not match the content
     * @throws MementoDatetimeFormatException if the header value of memento-datetime is not RFC-1123 format
     */
    @POST
    public Response addVersion(@HeaderParam(MEMENTO_DATETIME_HEADER) final String datetimeHeader,
            @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
            @HeaderParam(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
            @HeaderParam("Digest") final String digest,
            @ContentLocation final InputStream requestBodyStream)
            throws InvalidChecksumException, MementoDatetimeFormatException {

        final FedoraResource timeMap = resource().findOrCreateTimeMap();
        // If the timemap was created in this request, commit it.
        if (timeMap.isNew()) {
            session.commit();
        }

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
                    : Instant.from(RFC_1123_DATE_TIME.parse(datetimeHeader)));
            } catch (final DateTimeParseException e) {
                throw new MementoDatetimeFormatException("Invalid memento date-time value. "
                        + "Please use RFC-1123 date-time format, such as 'Tue, 3 Jun 2008 11:05:30 GMT'", e);
            }

            final boolean createFromExisting = isBlank(datetimeHeader);

            if (requestContentType == null && !createFromExisting) {
                throw new ClientErrorException("Content Type is required for creating a binary memento",
                        UNSUPPORTED_MEDIA_TYPE);
            }

            try {
                LOGGER.info("Request to add version for date '{}' for '{}'", datetimeHeader, externalPath);

                // Create memento
                final FedoraResource memento;
                if (resource instanceof FedoraBinary) {
                    memento = versionService.createBinaryVersion(session.getFedoraSession(), resource(),
                            mementoInstant, null, null, null, null);
                } else {
                    final InputStream bodyStream = createFromExisting ? null : requestBodyStream;
                    final Lang format = createFromExisting ? null : contentTypeToLang(contentType.toString());
                    if (!createFromExisting && format == null) {
                        throw new ClientErrorException("Invalid Content Type " + contentType.toString(),
                                UNSUPPORTED_MEDIA_TYPE);
                    }

                    memento = versionService.createVersion(session.getFedoraSession(), resource(),
                            idTranslator, mementoInstant, bodyStream, format);
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

    /**
     * Get the list of versions for the object
     *
     * @return List of versions for the object as RDF
     */
    @GET
    @HtmlTemplate(value = "fcr:versions")
    @Produces({ TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8",
        N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET, RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET,
        TURTLE_X, TEXT_HTML_WITH_CHARSET, APPLICATION_LINK_FORMAT })
    public Response getVersionList(@HeaderParam("Range") final String rangeValue,
        @HeaderParam("Accept") final String acceptValue) throws IOException, UnsupportedAccessTypeException {
        if (!resource().isVersioned()) {
            throw new RepositoryVersionRuntimeException("This operation requires that the node be versionable");
        }
        final FedoraResource theTimeMap = resource().findOrCreateTimeMap();
        checkCacheControlHeaders(request, servletResponse, theTimeMap, session);

        LOGGER.info("GET resource '{}'", externalPath);

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
                                            RFC_1123_DATE_TIME.format(t.getMementoDatetime()
                                                                       .atZone(ZoneOffset.UTC)))
                                     .build());
            });
            // Based on the dates of the above mementos, add the range to the below link.
            final Instant[] Mementos = Arrays.stream(children).map(FedoraResource::getMementoDatetime)
                .sorted((a, b) -> a.compareTo(b))
                .toArray(Instant[]::new);
            final Builder linkBuilder =
                Link.fromUri(parentUri + "/" + FCR_VERSIONS).rel("self").type(APPLICATION_LINK_FORMAT);
            if (Mementos.length >= 2) {
                // There are 2 or more Mementos so make a range.
                linkBuilder.param("from", RFC_1123_DATE_TIME.format(Mementos[0].atOffset(ZoneOffset.UTC)));
                linkBuilder.param("until",
                    RFC_1123_DATE_TIME.format(Mementos[Mementos.length - 1].atOffset(ZoneOffset.UTC)));
            }
            versionLinks.add(linkBuilder.build());
            return ok(new LinkFormatStream(versionLinks.stream())).build();
        } else {
            final AcquiredLock readLock = lockManager.lockForRead(theTimeMap.getPath());
            try (final RdfStream rdfStream = new DefaultRdfStream(asNode(theTimeMap))) {
                // Need to set the timemap as the resource for the below function.
                setResource(theTimeMap);
                return getContent(rangeValue, getChildrenLimit(), rdfStream);
            } finally {
                readLock.release();
            }
        }
    }

    /**
     * Set the resource to an alternate from that retrieved automatically.
     *
     * @param resource a FedoraResource
     */
    private void setResource(final FedoraResource resource) {
        this.resource = resource;
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }
}
