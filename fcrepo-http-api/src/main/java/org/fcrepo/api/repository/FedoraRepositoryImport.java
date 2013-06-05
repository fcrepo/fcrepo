package org.fcrepo.api.repository;


import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Resource;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Path("/fcr:import")
public class FedoraRepositoryImport extends AbstractResource {

    @Resource
    private Map<String, FedoraObjectSerializer> serializers;

    private final Logger logger = getLogger(this.getClass());

    @POST
    public Response importObject(@QueryParam("format")
                                 @DefaultValue("jcr/xml")
                                 final String format, final InputStream stream) throws IOException,
                                                                                               RepositoryException, InvalidChecksumException {

        final String path = "/";
        logger.debug("Deserializing at {}", path);
        final Session session = getAuthenticatedSession();

        try {
            serializers.get(format).deserialize(session, path, stream);
            session.save();
            // TODO return proper URI for new resource
            return created(uriInfo.getAbsolutePath()).build();
        } finally {
            session.logout();
        }
    }

    public void setSerializers(final Map<String, FedoraObjectSerializer> serializers) {
        this.serializers = serializers;
    }

}
