/**
 * Copyright 2013 DuraSpace, Inc.
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

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sun.jersey.core.header.ContentDisposition;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * Content controller for adding, reading, and manipulating
 * binary streams
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:content")
public class FedoraContent extends AbstractResource {

    public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    public static final int PARTIAL_CONTENT = 206;

    @InjectedSession
    protected Session session;

    private final Logger logger = getLogger(FedoraContent.class);

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
                    final InputStream requestBodyStream)
        throws IOException, InvalidChecksumException,
                   RepositoryException, URISyntaxException, ParseException {
        final MediaType contentType =
                requestContentType != null ? requestContentType
                        : APPLICATION_OCTET_STREAM_TYPE;


        final String newDatastreamPath;
        final String path = toPath(pathList);

        if (nodeService.exists(session, path)) {
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


        logger.debug("Attempting to ingest fcr:content with path: {}", newDatastreamPath);

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


            final Node datastreamNode =
                    datastreamService.createDatastreamNode(session, newDatastreamPath,
                            contentType.toString(), originalFileName, requestBodyStream,
                            checksumURI);

            final HttpGraphSubjects subjects =
                    new HttpGraphSubjects(session, FedoraNodes.class,
                            uriInfo);

            session.save();
            versionService.nodeUpdated(session, path);
            return created(
                    new URI(subjects.getGraphSubject(
                            datastreamNode.getNode(JCR_CONTENT)).getURI()))
                    .build();

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
     * @throws IOException
     * @throws InvalidChecksumException
     */
    @PUT
    @Timed
    public Response modifyContent(@PathParam("path") final List<PathSegment> pathList,
                                  @QueryParam("checksum") final String checksum,
                                  @HeaderParam("Content-Disposition") final String contentDisposition,
                                  @HeaderParam("Content-Type") final MediaType requestContentType,
                                  final InputStream requestBodyStream,
                                  @Context final Request request)
        throws RepositoryException, IOException, InvalidChecksumException, URISyntaxException, ParseException {

        try {
            final String path = toPath(pathList);
            final MediaType contentType =
                    requestContentType != null ? requestContentType
                            : APPLICATION_OCTET_STREAM_TYPE;

            if (nodeService.exists(session, path)) {

                final Datastream ds =
                        datastreamService.getDatastream(session, path);

                final EntityTag etag =
                        new EntityTag(ds.getContentDigest().toString());
                final Date date = ds.getLastModifiedDate();
                final Date roundedDate = new Date();
                roundedDate
                        .setTime(date.getTime() - date.getTime() % 1000);
                final ResponseBuilder builder =
                        request.evaluatePreconditions(roundedDate, etag);

                if (builder != null) {
                    throw new WebApplicationException(builder.build());
                }
            }

            logger.debug("create Datastream {}", path);

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

            final Node datastreamNode =
                datastreamService.createDatastreamNode(session, path,
                    contentType.toString(), originalFileName, requestBodyStream, checksumURI);

            final boolean isNew = datastreamNode.isNew();
            session.save();
            versionService.nodeUpdated(session, path);

            if (isNew) {
                final HttpGraphSubjects subjects =
                        new HttpGraphSubjects(session, FedoraNodes.class,
                                uriInfo);

                return created(
                        new URI(
                                subjects.getGraphSubject(
                                        datastreamNode
                                                .getNode(JCR_CONTENT))
                                        .getURI())).build();
            } else {
                return noContent().build();
            }
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
    public Response getContent(@PathParam("path")
        final List<PathSegment> pathList, @HeaderParam("Range")
        final String rangeValue, @Context
        final Request request) throws RepositoryException, IOException {

        try {
            final String path = toPath(pathList);
            final Datastream ds =
                    datastreamService.getDatastream(session, path);

            final EntityTag etag =
                    new EntityTag(ds.getContentDigest().toString());
            final Date date = ds.getLastModifiedDate();
            final Date roundedDate = new Date();
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);
            ResponseBuilder builder =
                    request.evaluatePreconditions(roundedDate, etag);

            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);

            if (builder == null) {

                final InputStream content = ds.getContent();

                if (rangeValue != null && rangeValue.startsWith("bytes")) {

                    final Range range = Range.convert(rangeValue);

                    final long contentSize = ds.getContentSize();

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
                        builder =
                                status(
                                        REQUESTED_RANGE_NOT_SATISFIABLE)
                                        .header("Content-Range",
                                                contentRangeValue);
                    } else {
                        final RangeRequestInputStream rangeInputStream =
                                new RangeRequestInputStream(content, range
                                        .start(), range.size());

                        builder =
                                status(PARTIAL_CONTENT).entity(
                                        rangeInputStream)
                                        .header("Content-Range",
                                                contentRangeValue);
                    }

                } else {
                    builder = ok(content);
                }
            }

            final HttpGraphSubjects subjects =
                    new HttpGraphSubjects(session, FedoraNodes.class,
                            uriInfo);

            final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                                                              .fileName(ds.getFilename())
                                                              .creationDate(ds.getCreatedDate())
                                                              .modificationDate(ds.getLastModifiedDate())
                                                              .size(ds.getContentSize())
                                                              .build();

            return builder.type(ds.getMimeType()).header(
                    "Link",
                    subjects.getGraphSubject(ds.getNode()) +
                            ";rel=\"meta\"").header("Accept-Ranges",
                    "bytes").cacheControl(cc).lastModified(date).tag(etag)
                    .header("Content-Disposition", contentDisposition)
                    .build();
        } finally {
            session.logout();
        }
    }

}
