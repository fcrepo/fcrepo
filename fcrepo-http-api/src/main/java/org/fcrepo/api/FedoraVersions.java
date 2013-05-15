
package org.fcrepo.api;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.provider.GraphStreamingOutput;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/rest/{path: .*}/fcr:versions")
public class FedoraVersions extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraNodes.class);

    @GET
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Response getVersionList(@PathParam("path")
    final List<PathSegment> pathList, @Context Request request) throws RepositoryException {
        final String path = toPath(pathList);

        LOGGER.trace("getting versions list for {}", path);

        Variant bestPossibleResponse = request.selectVariant(POSSIBLE_RDF_VARIANTS);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource = nodeService.getObject(session, path);

            return Response.ok(new GraphStreamingOutput(resource.getVersionGraphStore(),
                                                                  bestPossibleResponse.getMediaType())).build();

        } finally {
            session.logout();
        }

    }

    @POST
    @Path("/{versionLabel}")
    public Response addVersionLabel(@PathParam("path")
                                    final List<PathSegment> pathList, @PathParam("versionLabel") final String versionLabel) throws RepositoryException {

        final String path = toPath(pathList);
        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource = nodeService.getObject(session, path);
            resource.addVersionLabel(versionLabel);

            return Response.noContent().build();
        } finally {
            session.logout();
        }
    }

    @Path("/{versionLabel}")
    @GET
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Response getVersion(@PathParam("path")
    final List<PathSegment> pathList, @PathParam("versionLabel")
    final String versionLabel, @Context Request request) throws RepositoryException, IOException {
        final String path = toPath(pathList);
        LOGGER.trace("getting version profile for {} at version {}", path, versionLabel);

        Variant bestPossibleResponse = request.selectVariant(POSSIBLE_RDF_VARIANTS);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource = nodeService.getObject(session, path, versionLabel);

            if (resource == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {

                return Response.ok(new GraphStreamingOutput(resource.getGraphStore(),
                                                               bestPossibleResponse.getMediaType())).build();
            }

        } finally {
            session.logout();
        }

    }

}
