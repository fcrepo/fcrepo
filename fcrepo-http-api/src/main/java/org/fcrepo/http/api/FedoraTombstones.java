/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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

        final Tombstone tombstone = (Tombstone) resource;
        final var deletedResource = tombstone.getDeletedObject();
        if (deletedResource.getArchivalGroupId().isPresent()) {
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).allow().build();
        }

        try {
            LOGGER.info("Delete tombstone: {}", resource.getFedoraId());
            doInDbTxWithRetry(() -> {
                purgeResourceService.perform(transaction(), tombstone.getDeletedObject(), getUserPrincipal());
                transaction().commitIfShortLived();
            });
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
        final var resource = resource();
        if (!(resource instanceof Tombstone)) {
            // If the resource is not deleted there is no tombstone.
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final var response = Response.ok().allow();
        final Tombstone tombstone = (Tombstone) resource;
        final var deletedResource = tombstone.getDeletedObject();
        if (deletedResource.getArchivalGroupId().isEmpty()) {
            response.allow("DELETE");
        }

        return response.build();
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
        final var response = Response.status(Response.Status.METHOD_NOT_ALLOWED).allow();
        final var resource = resource();
        if (resource instanceof Tombstone) {
            final var deleted = ((Tombstone) resource).getDeletedObject();
            if (deleted.getArchivalGroupId().isEmpty()) {
                response.allow("DELETE");
            }
        }

        return response.build();
    }
}
