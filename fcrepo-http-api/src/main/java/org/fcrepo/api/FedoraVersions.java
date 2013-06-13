
package org.fcrepo.api;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.responses.GraphStoreStreamingOutput;
import org.fcrepo.session.InjectedSession;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Dataset;

@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:versions")
public class FedoraVersions extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraNodes.class);

    @GET
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Response getVersionList(@PathParam("path")
    final List<PathSegment> pathList, @Context
    final Request request, @Context
    final UriInfo uriInfo) throws RepositoryException {
        final String path = toPath(pathList);

        LOGGER.trace("getting versions list for {}", path);

        final Variant bestPossibleResponse =
                request.selectVariant(POSSIBLE_RDF_VARIANTS);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path);

            return Response.ok(
                    new GraphStoreStreamingOutput(resource
                            .getVersionDataset(new HttpGraphSubjects(
                                    FedoraNodes.class, uriInfo)),
                            bestPossibleResponse.getMediaType())).build();

        } finally {
            session.logout();
        }

    }

    @POST
    @Path("/{versionLabel}")
    public Response addVersionLabel(@PathParam("path")
    final List<PathSegment> pathList, @PathParam("versionLabel")
    final String versionLabel) throws RepositoryException {

        final String path = toPath(pathList);
        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path);
            resource.addVersionLabel(versionLabel);

            return Response.noContent().build();
        } finally {
            session.logout();
        }
    }

    @Path("/{versionLabel}")
    @GET
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Dataset getVersion(@PathParam("path")
    final List<PathSegment> pathList, @PathParam("versionLabel")
    final String versionLabel, @Context
    final UriInfo uriInfo) throws RepositoryException, IOException {
        final String path = toPath(pathList);
        LOGGER.trace("getting version profile for {} at version {}", path,
                versionLabel);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path, versionLabel);

            if (resource == null) {
                throw new WebApplicationException(status(NOT_FOUND).build());
            } else {

                return resource.getPropertiesDataset(new HttpGraphSubjects(
                        FedoraNodes.class, uriInfo), 0, -1);
            }

        } finally {
            session.logout();
        }

    }

    public void setSession(final Session session) {
        this.session = session;
    }
}
