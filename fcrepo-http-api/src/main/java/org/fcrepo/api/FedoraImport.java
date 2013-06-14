
package org.fcrepo.api;

import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.session.InjectedSession;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author ajs6f
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:import")
public class FedoraImport extends AbstractResource {

    @InjectedSession
    protected Session session;

    @Autowired
    protected SerializerUtil serializers;

    private final Logger logger = getLogger(this.getClass());

    @POST
    public Response importObject(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("format")
    @DefaultValue("jcr/xml")
    final String format, final InputStream stream) throws IOException,
                                                                                                      RepositoryException, InvalidChecksumException, URISyntaxException {

        final String path = toPath(pathList);
        logger.debug("Deserializing at {}", path);


        final HttpGraphSubjects subjects =
                new HttpGraphSubjects(FedoraNodes.class, uriInfo, session);

        try {
            serializers.getSerializer(format)
                    .deserialize(session, path, stream);
            session.save();
            return created(new URI(subjects.getGraphSubject(session.getNode(path)).getURI())).build();
        } finally {
            session.logout();
        }
    }

    public void setSerializers(final SerializerUtil serializers) {
        this.serializers = serializers;
    }

    public void setSession(final Session session) {
        this.session = session;
    }
}
