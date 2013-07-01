/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.api.repository;

import java.net.MalformedURLException;

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

import org.fcrepo.AbstractResource;
import org.fcrepo.api.FedoraNodes;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

/**
 * This class exposes the JCR workspace functionality. It may be
 * too JCR-y in the long run, but this lets us exercise the functionality.
 */
@Component
@Scope("prototype")
@Path("/fcr:workspaces")
public class FedoraRepositoryWorkspace extends AbstractResource {

    @InjectedSession
    protected Session session;

    /**
     * Get the list of accessible workspaces in this repository.
     *
     * TODO: serialize this as RDF (?)
     * @return
     * @throws RepositoryException
     */
    @GET
    public Response getWorkspaces() throws RepositoryException {
        return Response
                .ok(session.getWorkspace().getAccessibleWorkspaceNames())
                .build();
    }

    /**
     * Create a new workspace in the repository
     *
     * @param path
     * @param uriInfo
     * @return
     * @throws RepositoryException
     * @throws MalformedURLException
     */
    @POST
    @Path("{path}")
    public Response createWorkspace(@PathParam("path")
            final String path,
            @Context
            final UriInfo uriInfo) throws RepositoryException,
        MalformedURLException {
        final Workspace workspace = session.getWorkspace();
        workspace.createWorkspace(path);

        return Response.created(
                uriInfo.getAbsolutePathBuilder().path(FedoraNodes.class)
                        .buildFromMap(ImmutableMap.of("path", path))).build();

    }
}
