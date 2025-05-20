/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.http.commons.session.TransactionProvider;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHelper;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/5/14
 */
abstract public class FedoraBaseResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraBaseResource.class);

    @Context
    protected SecurityContext securityContext;

    @Inject
    protected ResourceFactory resourceFactory;

    @Inject
    protected ResourceHelper resourceHelper;

    @Context protected HttpServletRequest servletRequest;

    @Inject
    protected TransactionManager txManager;

    @Inject
    protected DbTransactionExecutor dbTransactionExecutor;

    private TransactionProvider txProvider;

    private Transaction transaction;

    protected HttpIdentifierConverter identifierConverter;

    protected HttpIdentifierConverter identifierConverter() {
        if (identifierConverter == null) {
            identifierConverter = new HttpIdentifierConverter(
                    uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return identifierConverter;
    }

    /**
     * Gets a fedora resource by id. Uses the provided transaction if it is uncommitted,
     * or uses a new transaction.
     *
     * @param transaction the fedora transaction
     * @param fedoraId    identifier of the resource
     * @return the requested FedoraResource
     */
    protected FedoraResource getFedoraResource(final Transaction transaction, final FedoraId fedoraId)
            throws PathNotFoundException {
        return resourceFactory.getResource(transaction, fedoraId);
    }

    /**
     * @param transaction the transaction in which to check
     * @param fedoraId identifier of the object to check
     * @param includeDeleted Whether to check for deleted resources too.
     * @return Returns true if an object with the provided id exists
     */
    protected boolean doesResourceExist(final Transaction transaction, final FedoraId fedoraId,
                                        final boolean includeDeleted) {
        return resourceHelper.doesResourceExist(transaction, fedoraId, includeDeleted);
    }

    /**
     *
     * @param transaction the transaction in which to check
     * @param fedoraId identifier of the object to check
     * @return Returns true if object does not exist but whose ID starts other resources that do exist.
     */
    protected boolean isGhostNode(final Transaction transaction, final FedoraId fedoraId) {
        return resourceHelper.isGhostNode(transaction, fedoraId);
    }

    protected String getUserPrincipal() {
        final Principal p = securityContext.getUserPrincipal();
        return p == null ? null : p.getName();
    }

    protected Transaction transaction() {
        if (transaction == null) {
            txProvider = new TransactionProvider(txManager, servletRequest,
                    uriInfo.getBaseUri(), fedoraPropsConfig.getJmsBaseUrl());
            transaction = txProvider.provide();
        }
        return transaction;
    }

    /**
     * Executes the runnable within a DB transaction that will retry entire block on MySQL deadlock exceptions.
     * If the runnable throws an exception, after completing any retires, then the Fedora transaction is marked
     * as failed.
     *
     * @param action the code to execute
     */
    protected void doInDbTxWithRetry(final Runnable action) {
        try {
            dbTransactionExecutor.doInTxWithRetry(action);
        } catch (RuntimeException e) {
            transaction().fail();
            throw e;
        }
    }

    /**
     * Executes the runnable within a DB transaction. The block is NOT retried on MySQL deadlock exceptions.
     * If the runnable throws an exception, then the Fedora transaction is marked as failed.
     *
     * @param action the code to execute
     */
    protected void doInDbTx(final Runnable action) {
        try {
            dbTransactionExecutor.doInTx(action);
        } catch (RuntimeException e) {
            transaction().fail();
            throw e;
        }
    }

}
