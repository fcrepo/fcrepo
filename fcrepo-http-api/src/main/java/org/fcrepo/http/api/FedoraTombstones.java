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

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.HttpHeaders.ALLOW;
import static jakarta.ws.rs.core.Response.noContent;
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
