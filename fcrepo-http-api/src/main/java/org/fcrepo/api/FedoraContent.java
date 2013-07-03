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

package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.session.InjectedSession;
import org.modeshape.jcr.api.JcrConstants;
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
    public Response create(
            @PathParam("path")
            final List<PathSegment> pathList,
            @QueryParam("checksum")
            final String checksum,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            final InputStream requestBodyStream)
        throws IOException, InvalidChecksumException, RepositoryException,
        URISyntaxException {
        final MediaType contentType =
                requestContentType != null ? requestContentType
                        : APPLICATION_OCTET_STREAM_TYPE;

        String path = toPath(pathList);
        if (path.endsWith("/fcr:new")) {
            logger.debug("Creating a new unnamed object");
            final String dsid = pidMinter.mintPid();
            path = path.replaceFirst("\\/fcr\\:new$", "/" + dsid);
        }

        logger.debug("create Datastream {}", path);
        try {
            final URI checksumURI;

            if (checksum != null && !checksum.equals("")) {
                checksumURI = new URI(checksum);
            } else {
                checksumURI = null;
            }

            final Node datastreamNode =
                    datastreamService.createDatastreamNode(session, path,
                            contentType.toString(), requestBodyStream,
                            checksumURI);

            final HttpGraphSubjects subjects =
                    new HttpGraphSubjects(FedoraNodes.class, uriInfo, session);

            return created(
                    new URI(subjects.getGraphSubject(
                            datastreamNode.getNode(JcrConstants.JCR_CONTENT))
                            .getURI())).build();

        } finally {
            session.save();
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
    public Response modifyContent(
            @PathParam("path")
            final List<PathSegment> pathList,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            final InputStream requestBodyStream,
            @Context
            final Request request) throws RepositoryException, IOException,
        InvalidChecksumException, URISyntaxException {
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
                roundedDate.setTime(date.getTime() - date.getTime() % 1000);
                final ResponseBuilder builder =
                        request.evaluatePreconditions(roundedDate, etag);

                if (builder != null) {
                    throw new WebApplicationException(builder.build());
                }
            }

            logger.debug("create Datastream {}", path);
            final Node datastreamNode =
                    datastreamService.createDatastreamNode(session, path,
                            contentType.toString(), requestBodyStream);
            final boolean isNew = datastreamNode.isNew();
            session.save();

            if (isNew) {
                final HttpGraphSubjects subjects =
                        new HttpGraphSubjects(FedoraNodes.class, uriInfo,
                                session);

                return created(
                        new URI(subjects.getGraphSubject(
                                datastreamNode
                                        .getNode(JcrConstants.JCR_CONTENT))
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
            final List<PathSegment> pathList,
            @Context
            final Request request) throws RepositoryException {

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
                builder = Response.ok(ds.getContent(), ds.getMimeType());
            }

            return builder.cacheControl(cc).lastModified(date).tag(etag)
                    .build();
        } finally {
            session.logout();
        }
    }

}
