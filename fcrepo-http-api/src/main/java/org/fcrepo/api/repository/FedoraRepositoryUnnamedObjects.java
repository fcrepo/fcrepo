package org.fcrepo.api.repository;

import org.apache.http.HttpStatus;
import org.fcrepo.AbstractResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/rest/fcr:new")
public class FedoraRepositoryUnnamedObjects extends AbstractResource {

    private static final Logger logger = getLogger(FedoraRepositoryUnnamedObjects.class);

    /**
     * Create an anonymous object with a newly minted name
     * @return 201
     */
    @POST
    public Response ingestAndMint(
                                  @QueryParam("mixin") @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT) String mixin,
                                  @QueryParam("checksumType") final String checksumType,
                                  @QueryParam("checksum") final String checksum,
                                  @HeaderParam("Content-Type") final MediaType requestContentType,
                                  final InputStream requestBodyStream) throws RepositoryException, IOException, InvalidChecksumException {
        final String pid = pidMinter.mintPid();

        String path = "/" + pid;

        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();

        try {
            if (nodeService.exists(session, path)) {
                return Response.status(HttpStatus.SC_CONFLICT).entity(path + " is an existing resource").build();
            }

            createObjectOrDatastreamFromRequestContent(session, path, mixin, requestBodyStream, requestContentType, checksumType, checksum);

            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
            return created(uriInfo.getRequestUri()).entity(path).build();

        } finally {
            session.logout();
        }
    }
}
