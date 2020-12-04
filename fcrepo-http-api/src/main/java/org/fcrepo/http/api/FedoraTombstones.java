/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.annotation.Timed;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.services.PurgeResourceService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * CRUD operations on Fedora tombstones
 *
 * @author cbeer
 */
@Timed
@Scope("request")
@Path("/{path: .*}/fcr:tombstone")
public class FedoraTombstones extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraTombstones.class);

    @PathParam("path") protected String externalPath;

    @Inject
    private PurgeResourceService purgeResourceService;

    /**
     * Default JAX-RS entry point
     */
    public FedoraTombstones() {
        super();
    }

    /**
     * Create a new FedoraTombstones instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraTombstones(final String externalPath) {
        this.externalPath = externalPath;
    }

    /**
     * Delete a tombstone resource (freeing the original resource to be reused)
     * @return the free resource
     */
    @DELETE
    public Response delete() {
        final FedoraResource resource = resource();
        if (!(resource instanceof Tombstone)) {
            // If the resource is not deleted there is no tombstone.
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            final Tombstone tombstone = (Tombstone) resource;
            LOGGER.info("Delete tombstone: {}", resource.getFedoraId());
            purgeResourceService.perform(transaction(), tombstone.getDeletedObject(), getUserPrincipal());
            transaction().commitIfShortLived();
            return noContent().build();
        } finally {
            transaction().releaseResourceLocksIfShortLived();
        }
    }

    /*
     * These methods are disallowed, but need to exist here or the path gets caught by the FedoraLdp path matcher.
     */
    @GET
    public Response get() {
        return methodNotAllowed();
    }

    @POST
    public Response post() {
        return methodNotAllowed();
    }
    @PUT
    public Response put() {
        return methodNotAllowed();
    }

    @OPTIONS
    public Response options() {
        return Response.ok().header(ALLOW, "DELETE").build();
    }

    @Override
    protected FedoraResource resource() {
        final FedoraId resourceId = identifierConverter().pathToInternalId(externalPath);
        try {
            return getFedoraResource(transaction(), resourceId);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected String externalPath() {
        return null;
    }

    private Response methodNotAllowed() {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).header(ALLOW, "DELETE").build();
    }
}
