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

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

import javax.inject.Inject;
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
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;


/**
 * Content controller for adding, reading, and manipulating
 * binary streams
 *
 * @author awoods
 * @author gregjan
 */
@Scope("prototype")
@Path("/{path: .*}/fcr:content")
public class FedoraContent extends ContentExposingResource {

    @Inject
    protected Session session;

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

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
    public Response create(@PathParam("path") final List<PathSegment> pathList,
                           @HeaderParam("Slug") final String slug,
                           @HeaderParam("Content-Disposition") final String contentDisposition,
                           @QueryParam("checksum") final String checksum,
                           @HeaderParam("Content-Type") final MediaType requestContentType,
                           @ContentLocation final InputStream requestBodyStream)
        throws InvalidChecksumException, ParseException {
        final MediaType contentType = getSimpleContentType(requestContentType);


        final String newDatastreamPath;
        final String path = toPath(pathList);

        if (nodeService.exists(session, path)) {
            if ( nodeService.getObject(session, path).hasContent() ) {
                return status(SC_CONFLICT)
                           .entity(path + " is an existing resource!").build();
            }

            final String pid;

            if (slug != null) {
                pid = slug;
            }  else {
                pid = pidMinter.mintPid();
            }

            newDatastreamPath = path + "/" + pid;
        } else {
            newDatastreamPath = path;
        }


        LOGGER.trace("Attempting to ingest fcr:content with path: {}", newDatastreamPath);

        try {

            if (nodeService.exists(session, newDatastreamPath)) {
                return status(SC_CONFLICT)
                           .entity(path + " is an existing resource!").build();
            }

            final URI checksumURI = checksumURI(checksum);
            final String originalFileName = originalFileName(contentDisposition);


            final Datastream datastream = datastreamService.findOrCreateDatastream(session, newDatastreamPath);

            final FedoraBinary binary = datastream.getBinary();

            binary.setContent(requestBodyStream,
                    contentType.toString(),
                    checksumURI,
                    originalFileName,
                    datastreamService.getStoragePolicyDecisionPoint());


            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class,
                            uriInfo);
            final ResponseBuilder builder;
            try {
                session.save();
                versionService.nodeUpdated(datastream.getNode());

                builder = created(URI.create(subjects.getSubject(
                        binary.getPath()).getURI()));
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            addCacheControlHeaders(servletResponse, binary, session);

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
                                  @ContentLocation final InputStream requestBodyStream)
        throws InvalidChecksumException, ParseException {

        try {
            final String path = toPath(pathList);
            final MediaType contentType = getSimpleContentType(requestContentType);

            if (nodeService.exists(session, path)) {

                final Datastream ds =
                        datastreamService.findOrCreateDatastream(session, path);
                evaluateRequestPreconditions(request, servletResponse, ds, session);
            }

            LOGGER.debug("create Datastream {}", path);

            final URI checksumURI = checksumURI(checksum);
            final String originalFileName = originalFileName(contentDisposition);

            final Datastream datastream = datastreamService.findOrCreateDatastream(session, path);

            final FedoraBinary binary = datastream.getBinary();

            binary.setContent(requestBodyStream,
                    contentType.toString(),
                    checksumURI,
                    originalFileName,
                    datastreamService.getStoragePolicyDecisionPoint());

            final boolean isNew = datastream.isNew();

            try {
                session.save();
                versionService.nodeUpdated(datastream.getNode());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            final ResponseBuilder builder;
            if (isNew) {
                final HttpIdentifierTranslator subjects =
                        new HttpIdentifierTranslator(session, FedoraNodes.class,
                                uriInfo);

                builder = created(URI.create(subjects.getSubject(
                        binary.getPath()).getURI()));
            } else {
                builder = noContent();
            }

            addCacheControlHeaders(servletResponse, binary, session);

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
                               @HeaderParam("Range") final String rangeValue)
        throws IOException {
        try {
            final String path = toPath(pathList);
            LOGGER.info("Attempting get of {}.", path);

            final Datastream ds =
                    datastreamService.findOrCreateDatastream(session, path);

            if (ds.getNode().hasProperty("fedorarelsext:hasExternalContent")) {
                final URI externalURI = URI.create(ds.getNode().getProperty("fedorarelsext:hasExternalContent")
                        .getValues()[0].getString());
                LOGGER.debug("Redirecting to external content {}", externalURI.toString());
                return status(SC_MOVED_TEMPORARILY).header("Location", externalURI.toString()).build();
            }

            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class,
                            uriInfo);

            return getDatastreamContentResponse(ds.getBinary(), rangeValue, request, servletResponse,
                    subjects, session);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } finally {
            session.logout();
        }
    }

}
