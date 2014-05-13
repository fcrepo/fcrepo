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

import com.codahale.metrics.annotation.Timed;
import com.sun.jersey.core.header.ContentDisposition;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Content controller for adding, reading, and manipulating
 * binary streams
 *
 * @author awoods
 * @author gregjan
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:content")
public class FedoraContent extends ContentExposingResource {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraContent.class);

    /**
     * Create an anonymous DS with a newly minted name and content from request
     * body
     *
     * @param pathList
     * @throws RepositoryException
     */
    @POST
    @Timed
    public Response create(@PathParam("path")
            final List<PathSegment> pathList,
            @HeaderParam("Slug") final String slug,
            @HeaderParam("Content-Disposition") final String contentDisposition,
            @QueryParam("checksum") final String checksum,
            @HeaderParam("Content-Type") final MediaType requestContentType,
                    final InputStream requestBodyStream, @Context final HttpServletResponse servletResponse)
        throws InvalidChecksumException, RepositoryException, URISyntaxException, ParseException {

        final HttpIdentifierTranslator subjects =
                new HttpIdentifierTranslator(session, FedoraNodes.class,
                                             uriInfo);

        final MediaType contentType = getSimpleContentType(requestContentType);


        final String newDatastreamPath;
        final String path = toPath(pathList);
        final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
        if (nodeService.exists(session, jcrPath)) {
            if ( nodeService.getObject(session, jcrPath).hasContent() ) {
                return status(SC_CONFLICT)
                           .entity(path + " is an existing resource!").build();
            }

            final String pid;

            if (slug != null) {
                pid = slug;
            }  else {
                pid = pidMinter.mintPid();
            }
            newDatastreamPath = getJCRPath(createResource(uriInfo.getBaseUri() + path + "/" + pid), subjects);
        } else {
            newDatastreamPath = jcrPath;
        }


        LOGGER.trace("Attempting to ingest fcr:content with path: {}", newDatastreamPath);

        try {

            if (nodeService.exists(session, newDatastreamPath)) {
                return status(SC_CONFLICT)
                           .entity(path + " is an existing resource!").build();
            }

            final URI checksumURI;

            if (checksum != null && !checksum.equals("")) {
                checksumURI = new URI(checksum);
            } else {
                checksumURI = null;
            }

            final String originalFileName;

            if (contentDisposition != null) {
                final ContentDisposition disposition = new ContentDisposition(contentDisposition);
                originalFileName = disposition.getFileName();
            } else {
                originalFileName = null;
            }


            final Datastream datastream =
                    datastreamService.createDatastream(session, newDatastreamPath,
                            contentType.toString(), originalFileName, requestBodyStream,
                            checksumURI);

            session.save();
            versionService.nodeUpdated(datastream.getNode());

            final ResponseBuilder builder = created(new URI(subjects.getSubject(
                    datastream.getContentNode().getPath()).getURI()));

            addCacheControlHeaders(servletResponse, datastream);

            return builder.build();

        } finally {
            session.logout();
        }
    }

    /**
     * Modify an existing datastream's content
     *
     * @param pathList
     * @param requestContentType Content-Type header
     * @param requestBodyStream Binary blob
     * @return 201 Created
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    @PUT
    @Timed
    public Response modifyContent(@PathParam("path") final List<PathSegment> pathList,
                                  @QueryParam("checksum") final String checksum,
                                  @HeaderParam("Content-Disposition") final String contentDisposition,
                                  @HeaderParam("Content-Type") final MediaType requestContentType,
                                  final InputStream requestBodyStream,
                                  @Context final Request request, @Context final HttpServletResponse servletResponse)
        throws RepositoryException, InvalidChecksumException, URISyntaxException, ParseException {

        try {
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class,
                                                 uriInfo);

            final String path = toPath(pathList);
            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            final MediaType contentType = getSimpleContentType(requestContentType);

            if (nodeService.exists(session, jcrPath)) {

                final Datastream ds =
                        datastreamService.getDatastream(session, jcrPath);

                evaluateRequestPreconditions(request, ds);
            }

            LOGGER.debug("PUT: Create Datastream {}", jcrPath);

            final URI checksumURI;

            if (checksum != null && !checksum.equals("")) {
                checksumURI = new URI(checksum);
            } else {
                checksumURI = null;
            }

            final String originalFileName;

            if (contentDisposition != null) {
                final ContentDisposition disposition = new ContentDisposition(contentDisposition);
                originalFileName = disposition.getFileName();
            } else {
                originalFileName = null;
            }

            final Datastream datastream =
                datastreamService.createDatastream(session, jcrPath,
                    contentType.toString(), originalFileName, requestBodyStream, checksumURI);

            final boolean isNew = datastream.isNew();
            session.save();
            versionService.nodeUpdated(datastream.getNode());

            ResponseBuilder builder;
            if (isNew) {
                builder = created(new URI(subjects.getSubject(
                        datastream.getContentNode().getPath()).getURI()));
            } else {
                builder = noContent();
            }

            addCacheControlHeaders(servletResponse, datastream);

            return builder.build();
        } finally {
            session.logout();
        }
    }

    /**
     * Get the binary content of a datastream
     *
     * @param pathList
     * @return Binary blob
     * @throws RepositoryException
     */
    @GET
    @Timed
    public Response getContent(@PathParam("path") final List<PathSegment> pathList,
                               @HeaderParam("Range") final String rangeValue,
                               @Context final Request request,
                               @Context final HttpServletResponse servletResponse)
        throws RepositoryException, IOException {
        try {
            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class,
                                                 uriInfo);

            final String path = toPath(pathList);
            final String jcrPath = getJCRPath(createResource(uriInfo.getBaseUri() + path), subjects);
            LOGGER.info("GET: Attempting get {} from hierarchy path {}.", path, jcrPath);

            final Datastream ds =
                    datastreamService.getDatastream(session, jcrPath);

            return getDatastreamContentResponse(ds, rangeValue, request, servletResponse,
                                                   subjects);

        } finally {
            session.logout();
        }
    }

}
