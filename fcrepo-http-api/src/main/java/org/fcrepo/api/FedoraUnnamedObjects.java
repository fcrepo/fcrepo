
package org.fcrepo.api;

import static javax.ws.rs.core.Response.created;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.session.InjectedSession;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author ajs6f
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:new")
public class FedoraUnnamedObjects extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger logger = getLogger(FedoraUnnamedObjects.class);

    /**
     * Create an anonymous object with a newly minted name
     * @param pathList
     * @return 201
     */
    @POST
    public Response ingestAndMint(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("mixin")
    @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT)
    final String mixin, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @HeaderParam("Content-Type")
    final MediaType requestContentType, final InputStream requestBodyStream,
            @Context
            final UriInfo uriInfo) throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = pidMinter.mintPid();

        final String path = toPath(pathList) + "/" + pid;

        logger.debug("Attempting to ingest with path: {}", path);

        try {
            if (nodeService.exists(this.session, path)) {
                return Response.status(SC_CONFLICT).entity(
                        path + " is an existing resource").build();
            }

            final FedoraResource resource =
                    createObjectOrDatastreamFromRequestContent(
                            FedoraNodes.class, this.session, path, mixin, uriInfo,
                            requestBodyStream, requestContentType,
                            checksumType, checksum);

            this.session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
            return created(
                    uriInfo.getBaseUriBuilder().path(FedoraNodes.class).build(
                            resource.getPath().substring(1))).entity(path)
                    .build();

        } finally {
        	this.session.logout();
        }
    }

    public void setSession(final Session session) {
        this.session = session;
    }

}
