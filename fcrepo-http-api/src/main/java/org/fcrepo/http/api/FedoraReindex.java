/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.services.ReindexService;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import io.micrometer.core.annotation.Timed;

/**
 * @author dbernstein
 * @since 12/01/20
 */
@Timed
@Scope("request")
@Path("/{path: (.+/)?}fcr:reindex")
public class FedoraReindex extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraReindex.class);

    @Inject
    private ReindexService reindexService;

    @PathParam("path")
    protected String externalPath;

    /**
     * Default JAX-RS entry point
     */
    public FedoraReindex() {
        super();
    }

    /**
     * Reindex a fedora resource.
     *
     * @return A response
     */
    @POST
    @Produces({APPLICATION_JSON + ";qs=1.0",
            TEXT_PLAIN_WITH_CHARSET})
    public Response reindexObject() {
        LOGGER.info("receiving reindex request for fedora_id = {}", externalPath);
        try {
            final var transaction = transaction();
            final var id = FedoraId.create(externalPath);
            if (doesResourceExist(transaction, id, true)) {
                //TODO : remove this block once reindexing of existing resources is supported.
                //       c.f.  https://jira.lyrasis.org/browse/FCREPO-3553
                return status(HttpStatus.SC_CONFLICT, "Reindexing of existing resources is not currently supported. " +
                        "Only resources that have not yet been indexed are allowed.").build();
            } else {
                try {
                    final var baseId = id.asBaseId();

                    doInDbTxWithRetry(() -> {
                        this.reindexService.reindexByFedoraId(transaction(), getUserPrincipal(), baseId);
                        transaction.commitIfShortLived();
                    });

                    final var message = format("successfully reindexed %s", id.getBaseId());
                    LOGGER.info(message);
                    return status(HttpStatus.SC_NO_CONTENT).entity(message).build();
                } finally {
                    transaction().releaseResourceLocksIfShortLived();
                }
            }

        } catch (final Exception ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @DELETE
    public Response delete() {
        return methodNotAllowed();
    }

    @PUT
    public Response put() {
        return methodNotAllowed();
    }

    @GET
    public Response get() {
        return methodNotAllowed();
    }

    private Response methodNotAllowed() {
        return status(HttpStatus.SC_METHOD_NOT_ALLOWED).build();
    }


}

