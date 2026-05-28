/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static java.util.Date.from;
import static jakarta.ws.rs.core.Response.created;
import static jakarta.ws.rs.core.Response.noContent;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_EXPIRES_HEADER;
import static org.fcrepo.http.commons.session.TransactionConstants.EXPIRES_RFC_1123_FORMATTER;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_COMMIT_REL;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_PREFIX;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import io.micrometer.core.annotation.Timed;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.exception.TransactionNotFoundException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * The rest interface for transaction management. The interfaces
 * allows for creation, commit and rollback of transactions.
 *
 * @author awoods
 * @author gregjan
 * @author mohideen
 */
@Timed
@Scope("prototype")
@Path("/fcr:tx")
public class Transactions extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(Transactions.class);

    /**
     * Get the status of an existing transaction
     *
     * @param txId id of the transaction
     * @return 204 no content if status retrieved, 410 gone if transaction doesn't exist.
     */
    @GET
    @Path("{transactionId}")
    public Response getTransactionStatus(@PathParam("transactionId") final String txId) {
        // Retrieve the tx provided via the path
        final Transaction tx;
        try {
            tx = txManager.get(txId);
        } catch (final TransactionNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (final TransactionClosedException e) {
            return Response.status(Status.GONE)
                    .entity(e.getMessage())
                    .type(TEXT_PLAIN_WITH_CHARSET)
                    .build();
        }

        LOGGER.info("Checking transaction status'{}'", tx.getId());

        return Response.status(Status.NO_CONTENT)
                .header(ATOMIC_EXPIRES_HEADER, EXPIRES_RFC_1123_FORMATTER.format(tx.getExpires()))
                .build();
    }

    /**
     * Refresh an existing transaction
     *
     * @param txId id of the transaction
     * @return 204 no content if successfully refreshed, 410 gone if transaction doesn't exist.
     */
    @POST
    @Path("{transactionId}")
    public Response refreshTransaction(@PathParam("transactionId") final String txId) {
        // Retrieve the tx provided via the path
        final Transaction tx;
        try {
            tx = txManager.get(txId);
        } catch (final TransactionNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (final TransactionClosedException e) {
            return Response.status(Status.GONE)
                    .entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET)
                    .build();
        }

        tx.refresh();
        LOGGER.info("Refreshed transaction '{}'", tx.getId());

        return Response.status(Status.NO_CONTENT)
                .header(ATOMIC_EXPIRES_HEADER, EXPIRES_RFC_1123_FORMATTER.format(tx.getExpires()))
                .build();
    }

    /**
     * Create a new transaction resource and add it to the registry
     *
     * @return 201 with the transaction id and expiration date
     * @throws URISyntaxException if URI syntax exception occurred
     */
    @POST
    public Response createTransaction() throws URISyntaxException {
        final Transaction tx = transaction();
        tx.setShortLived(false);

        LOGGER.info("Created transaction '{}'", tx.getId());
        final var externalId = identifierConverter()
                .toExternalId(FEDORA_ID_PREFIX + "/" + TX_PREFIX + tx.getId());
        final var res = created(new URI(externalId));
        res.expires(from(tx.getExpires()));

        final var commitUri = URI.create(externalId);
        final var commitLink = Link.fromUri(commitUri).rel(TX_COMMIT_REL).build();
        res.links(commitLink);

        return res.build();
    }

    /**
     * Commit a transaction resource
     *
     * @param txId the transaction id
     * @return 204
     */
    @PUT
    @Path("{transactionId}")
    public Response commit(@PathParam("transactionId") final String txId) {
        try {
            final Transaction transaction = txManager.get(txId);
            LOGGER.info("Committing transaction '{}'", transaction.getId());
            transaction.commit();
            return noContent().build();
        } catch (final TransactionNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (final TransactionClosedException e) {
            return Response.status(Status.GONE)
                    .entity(e.getMessage())
                    .type(TEXT_PLAIN_WITH_CHARSET)
                    .build();
        } catch (final RepositoryRuntimeException e) {
            return Response.status(Status.CONFLICT)
                    .entity(e.getMessage())
                    .type(TEXT_PLAIN_WITH_CHARSET)
                    .build();
        }
    }

    /**
     * Rollback a transaction
     *
     * @param txId the transaction id
     * @return 204
     */
    @DELETE
    @Path("{transactionId}")
    public Response rollback(@PathParam("transactionId") final String txId) {
        try {
            final Transaction transaction = txManager.get(txId);
            LOGGER.info("Rollback transaction '{}'", transaction.getId());
            transaction.rollback();
            return noContent().build();
        } catch (final TransactionNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (final TransactionClosedException e) {
            return Response.status(Status.GONE)
                    .entity(e.getMessage())
                    .type(TEXT_PLAIN_WITH_CHARSET)
                    .build();
        } catch (final RepositoryRuntimeException e) {
            return Response.status(Status.CONFLICT)
                    .entity(e.getMessage())
                    .type(TEXT_PLAIN_WITH_CHARSET)
                    .build();
        }
    }

}
