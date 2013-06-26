
package org.fcrepo.api.repository;

import com.google.common.collect.ImmutableMap;
import org.fcrepo.AbstractResource;
import org.fcrepo.api.FedoraNodes;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;

@Component
@Scope("prototype")
@Path("/fcr:workspaces")
public class FedoraRepositoryWorkspace extends AbstractResource {

    @InjectedSession
    protected Session session;

    @GET
    public Response getWorkspaces() throws RepositoryException {
        return Response
                .ok(session.getWorkspace().getAccessibleWorkspaceNames())
                .build();
    }

    @POST
    @Path("{path}")
    public Response createWorkspace(@PathParam("path")
    final String path, @Context
    final UriInfo uriInfo) throws RepositoryException, MalformedURLException {
        final Workspace workspace = session.getWorkspace();
        workspace.createWorkspace(path);

        return Response.created(
                uriInfo.getAbsolutePathBuilder().path(FedoraNodes.class)
                        .buildFromMap(ImmutableMap.of("path", path))).build();

    }

    public void setSession(final Session session) {
        this.session = session;
    }

}
