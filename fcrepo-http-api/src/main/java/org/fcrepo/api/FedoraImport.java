
package org.fcrepo.api;

import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/{path: .*}/fcr:import")
public class FedoraImport extends AbstractResource {

    @Autowired
    protected SerializerUtil serializers;

    private final Logger logger = getLogger(this.getClass());

    @POST
    public Response importObject(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("format")
    @DefaultValue("jcr/xml")
    final String format, final InputStream stream) throws IOException,
            RepositoryException, InvalidChecksumException {

        final String path = toPath(pathList);
        logger.debug("Deserializing at {}", path);
        final Session session = getAuthenticatedSession();

        try {
            serializers.getSerializer(format).deserialize(session, path, stream);
            session.save();
            return created(uriInfo.getAbsolutePathBuilder().path(FedoraNodes.class).build(path.substring(1))).build();
        } finally {
            session.logout();
        }
    }

    public void setSerializers(final SerializerUtil  serializers) {
        this.serializers = serializers;
    }
}
