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

package org.fcrepo.auth.roles.common;

import static com.sun.jersey.api.Responses.notFound;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.FedoraResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * RESTful interface to create and manage access roles
 *
 * @author Gregory Jansen
 * @date Sep 5, 2013
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:accessroles")
public class AccessRoles extends AbstractResource {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccessRoles.class);

    @InjectedSession
    protected Session session;

    @Autowired
    private AccessRolesProvider accessRolesProvider = null;

    @Context
    protected HttpServletRequest request;


    /**
     * @return the accessRolesProvider
     */
    private AccessRolesProvider getAccessRolesProvider() {
        return accessRolesProvider;
    }

    /**
     * Retrieve the roles assigned to each principal on this specific path.
     *
     * @param pathList
     * @return JSON representation of assignment map
     * @throws RepositoryException
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Timed
    public Response get(@PathParam("path")
        final List<PathSegment> pathList, @QueryParam("effective")
        final String effective) throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.debug("Get access roles for: {}", path);
        LOGGER.debug("effective: {}", effective);
        Response.ResponseBuilder response;
        try {
            final Node node = nodeService.getObject(session, path).getNode();
            final Map<String, List<String>> data =
                    this.getAccessRolesProvider().getRoles(node,
                            (effective != null));
            if (data == null) {
                LOGGER.debug("no content response");
                response = Response.noContent();
            } else {
                response = Response.ok(data);
            }
        } catch (final PathNotFoundException e) {
            response = notFound().entity(e.getMessage());
        } catch (final AccessDeniedException e) {
            response = Response.status(Status.FORBIDDEN);
        } finally {
            session.logout();
        }
        return response.build();
    }

    /**
     * Apply new role assignments at the specified node.
     *
     * @param pathList
     * @param data
     * @return
     * @throws RepositoryException
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Timed
    public Response post(@PathParam("path")
        final List<PathSegment> pathList, final Map<String, Set<String>> data)
        throws RepositoryException {
        final String path = toPath(pathList);
        LOGGER.debug("POST Received request param: {}", request);
        Response.ResponseBuilder response;

        try {
            validatePOST(data);
        } catch (final IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        try {
            final FedoraResource resource =
                    nodeService.getObject(session, path);
            this.getAccessRolesProvider().postRoles(resource.getNode(), data);
            session.save();
            LOGGER.debug("Saved access roles {}", data);
            response =
                    Response.created(getUriInfo().getBaseUriBuilder()
                            .path(path).path("fcr:accessroles").build());
        } catch (final AccessDeniedException e) {
            response = Response.status(Status.FORBIDDEN);
        } finally {
            session.logout();
        }

        return response.build();
    }

    /**
     * @param data
     */
    private void validatePOST(final Map<String, Set<String>> data)
        throws IllegalArgumentException {
        if (data.isEmpty()) {
            throw new IllegalArgumentException(
                    "Posted access roles must include role assignments");
        }
        for (final Map.Entry<String, Set<String>> entry : data.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                throw new IllegalArgumentException(
                        "Assignments must include principal name and one or more roles");
            }
            if (entry.getKey().trim().length() == 0) {
                throw new IllegalArgumentException(
                        "Principal names cannot be an empty strings or whitespace.");
            }
            for (final String r : entry.getValue()) {
                if (r.trim().length() == 0) {
                    throw new IllegalArgumentException(
                            "Role names cannot be an empty strings or whitespace.");
                }
            }
        }
    }

    /**
     * Delete the access roles and node type.
     */
    @DELETE
    @Timed
    public Response deleteNodeType(@PathParam("path")
        final List<PathSegment> pathList) throws RepositoryException {
        final String path = toPath(pathList);
        try {
            final Node node = nodeService.getObject(session, path).getNode();
            this.getAccessRolesProvider().deleteRoles(node);
            session.save();
            return Response.noContent().build();
        } catch (final AccessDeniedException e) {
            return Response.status(Status.FORBIDDEN).build();
        } finally {
            session.logout();
        }
    }

    private UriInfo getUriInfo() {
        return this.uriInfo;
    }

}
