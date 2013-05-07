
package org.fcrepo.api;

import org.fcrepo.AbstractResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/rest/{path: .*}/fcr:import")
public class FedoraImport extends AbstractResource {

    @Resource
    private Map<String, FedoraObjectSerializer> serializers;

    private final Logger logger = getLogger(this.getClass());

    @POST
    public Response importObject(@PathParam("path") final List<PathSegment> pathList, @QueryParam("format")
    @DefaultValue("jcr/xml")
    final String format, final InputStream stream) throws IOException,
            RepositoryException, InvalidChecksumException {

		final String path = toPath(pathList);

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

    public void setSerializers(
            final Map<String, FedoraObjectSerializer> serializers) {
        this.serializers = serializers;
    }
}
