
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
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
import org.fcrepo.exception.InvalidChecksumException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/rest/{path: .*}/fcr:content")
public class FedoraContent extends AbstractResource {

    private final Logger logger = getLogger(FedoraContent.class);

    /**
     * Create an anonymous DS with a newly minted name
     * and content from request body
     * @param pathList
     * @throws RepositoryException 
     */
    @POST
    @Timed
    public Response create(
            @PathParam("path") final List<PathSegment> pathList,
            @QueryParam("checksumType") final String checksumType,
            @QueryParam("checksum") final String checksum,
            @HeaderParam("Content-Type") final MediaType requestContentType,
            final InputStream requestBodyStream)
            throws IOException, InvalidChecksumException, RepositoryException {
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
        final Session session = getAuthenticatedSession();
        try {
            datastreamService.createDatastreamNode(session, path, contentType
                    .toString(), requestBodyStream, checksumType, checksum);
        } finally {
            session.save();
			session.logout();
        }
        return created(uriInfo.getBaseUriBuilder().path("/rest" + path).build()).build();
    }


    /**
     * Modify an existing datastream's content
     *
	 * @param pathList
     * @param requestContentType
     *            Content-Type header
     * @param requestBodyStream
     *            Binary blob
     * @return 201 Created
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException 
     */
    @PUT
    @Timed
    public Response modifyContent(
            @PathParam("path") List<PathSegment> pathList,
            @HeaderParam("Content-Type") final MediaType requestContentType,
            final InputStream requestBodyStream)
            throws RepositoryException, IOException, InvalidChecksumException {
        final Session session = getAuthenticatedSession();
        try {
            String path = toPath(pathList);
            final MediaType contentType =
                    requestContentType != null ? requestContentType
                            : APPLICATION_OCTET_STREAM_TYPE;

            logger.debug("create Datastream {}", path);
            final Node datastreamNode = datastreamService.createDatastreamNode(session, path, contentType
                                                                                                      .toString(), requestBodyStream);
            final boolean isNew = datastreamNode.isNew();
            session.save();

            if (isNew) {
                return created(uriInfo.getBaseUriBuilder().path("/rest" + path).build()).build();
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
    public Response getContent(
            @PathParam("path") List<PathSegment> pathList,
            @Context final Request request
            ) throws RepositoryException {

		final Session session = getAuthenticatedSession();
		try {
			String path = toPath(pathList);
			final Datastream ds = datastreamService.getDatastream(session, path);

			final EntityTag etag = new EntityTag(ds.getContentDigest().toString());
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

			return builder.cacheControl(cc).lastModified(date).tag(etag).build();
		} finally {
			session.logout();
		}
	}

}
