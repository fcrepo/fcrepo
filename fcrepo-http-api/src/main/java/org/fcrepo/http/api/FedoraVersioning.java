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

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.apache.commons.lang3.StringUtils.isBlank;
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
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEMAP_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.LinkFormatStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryVersionRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
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
     * Create a new version checkpoint and tag it with the given label. If that label already describes another version
     * it will silently be reassigned to describe this version.
     *
     * @throws RepositoryException the exception
     * @return response
     * @throws UnsupportedAlgorithmException if an unsupported algorithm exception occurs
     * @throws InvalidChecksumException if an invalid checksum exception occurs
     */
    @POST
    public Response addVersion(@HeaderParam(MEMENTO_DATETIME_HEADER) final String datetimeHeader,
            @HeaderParam(CONTENT_TYPE) final MediaType requestContentType,
            @HeaderParam(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
            @HeaderParam("Digest") final String digest,
            @ContentLocation final InputStream requestBodyStream)
        throws RepositoryException, UnsupportedAlgorithmException, InvalidChecksumException {

        final AcquiredLock lock = lockManager.lockForWrite(resource().findOrCreateTimeMap().getPath(),
            session.getFedoraSession(), nodeService);

        try {
            final Collection<String> checksums = parseDigestHeader(digest);

            final MediaType contentType = getSimpleContentType(requestContentType);

            final Instant mementoInstant = (isBlank(datetimeHeader) ? Instant.now()
                : Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(datetimeHeader)));

            final FedoraResource memento =
                versionService.createVersion(session.getFedoraSession(), resource(), mementoInstant);

            LOGGER.info("Request to add version for date '{}' for '{}'", datetimeHeader, externalPath);
            // Ignore body unless a datetime header was provided
            if (datetimeHeader != null && requestBodyStream != null) {
                try (final RdfStream resourceTriples = new DefaultRdfStream(asNode(memento))) {
                    if (memento instanceof FedoraBinary) {
                        replaceResourceBinaryWithStream((FedoraBinary) memento,
                                requestBodyStream, contentDisposition, requestContentType, checksums);
                    } else if (isRdfContentType(contentType.toString())) {
                        // Get URI of memento to allow for remapping of subject to memento uri
                        final URI mementoUri = getUri(memento);
                        replaceResourceWithStream(memento, requestBodyStream, contentType,
                                resourceTriples, mementoUri);
                    }
                } catch (final Exception e) {
                    checkForInsufficientStorageException(e, e);
                }
            }

            return createUpdateResponse(memento, true);
        } finally {
            session.commit();
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
        TURTLE_X, TEXT_HTML_WITH_CHARSET, APPLICATION_LINK_FORMAT, "*/*" })
    public Response getVersionList(@HeaderParam("Range") final String rangeValue,
        @HeaderParam("Accept") final String acceptValue) throws IOException {
        if (!resource().isVersioned()) {
            throw new RepositoryVersionRuntimeException("This operation requires that the node be versionable");
        }
        final FedoraResource theTimeMap = resource().findOrCreateTimeMap();
        checkCacheControlHeaders(request, servletResponse, theTimeMap, session);

        LOGGER.info("GET resource '{}'", externalPath);

        final Link.Builder resourceLink = Link.fromUri(LDP_NAMESPACE + "Resource").rel("type");
        servletResponse.addHeader(LINK, resourceLink.build().toString());
        final Link.Builder rdfSourceLink = Link.fromUri(LDP_NAMESPACE + "RDFSource").rel("type");
        servletResponse.addHeader(LINK, rdfSourceLink.build().toString());
        servletResponse.addHeader(LINK, Link.fromUri(VERSIONING_TIMEMAP_TYPE).rel("type").build().toString());

        servletResponse.addHeader("Vary-Post", MEMENTO_DATETIME_HEADER);
        servletResponse.addHeader("Allow", "POST,HEAD,GET,OPTIONS");

        if (acceptValue != null && acceptValue.equalsIgnoreCase(APPLICATION_LINK_FORMAT)) {
            final URI parentUri = getUri(resource());
            final List<Link> versionLinks = new ArrayList<Link>();
            versionLinks.add(Link.fromUri(parentUri).rel("original").build());
            versionLinks.add(Link.fromUri(parentUri).rel("timegate").build());

            theTimeMap.getChildren().forEach(t -> {
                // Add mementos later.
                // https://jira.duraspace.org/browse/FCREPO-2617
            });
            // Based on the dates of the above mementos, add the range to the below link.
            final Link timeMapLink =
                Link.fromUri(parentUri + "/" + FCR_VERSIONS).rel("self").type(APPLICATION_LINK_FORMAT).build();
            versionLinks.add(timeMapLink);
            return ok(new LinkFormatStream(versionLinks.stream())).build();
        } else {
            final AcquiredLock readLock = lockManager.lockForRead(theTimeMap.getPath());
            try (final RdfStream rdfStream = new DefaultRdfStream(asNode(theTimeMap))) {
                addResourceHttpHeaders(theTimeMap);
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
