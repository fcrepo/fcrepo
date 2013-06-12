
package org.fcrepo.api.repository;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.FedoraNodes;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.session.InjectedSession;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Path("/fcr:new")
public class FedoraRepositoryUnnamedObjects extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger logger =
            getLogger(FedoraRepositoryUnnamedObjects.class);

    /**
     * Create an anonymous object with a newly minted name
     * @return 201
     */
    @POST
    public Response ingestAndMint(@QueryParam("mixin")
    @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT)
    final String mixin, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @HeaderParam("Content-Type")
    final MediaType requestContentType, final InputStream requestBodyStream,
            @Context
            final UriInfo uriInfo) throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = pidMinter.mintPid();
        final String path = "/" + pid;
        logger.debug("Attempting to ingest with path: {}", path);

        try {
            if (nodeService.exists(session, path)) {
                return status(SC_CONFLICT).entity(
                        path + " is an existing resource").build();
            }

            final FedoraResource resource =
                    createObjectOrDatastreamFromRequestContent(
                            FedoraNodes.class, session, path, mixin, uriInfo,
                            requestBodyStream, requestContentType,
                            checksumType, checksum);

            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
            return created(
                    uriInfo.getBaseUriBuilder().path(FedoraNodes.class).build(
                            resource.getPath().substring(1))).entity(path)
                    .build();
        } finally {
            session.logout();
        }
    }

    public void setSession(final Session session) {
        this.session = session;
    }
}
