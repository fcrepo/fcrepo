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

package org.fcrepo.http.commons.session;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.fcrepo.kernel.services.TransactionService;
import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for generating sessions for HTTP requests, taking
 * into account transactions, workspaces, and authentication.
 */
public class SessionFactory {

    private static final Logger logger = getLogger(SessionFactory.class);

    @Autowired
    private Repository repo;

    @Autowired
    private TransactionService transactionService;

    /**
     * initialize an empty session factory
     */
    public SessionFactory() {

    }

    /**
     * Initialize a session factory for the given Repository
     * 
     * @param repo
     * @param transactionService
     */
    public SessionFactory(final Repository repo,
            final TransactionService transactionService) {
        this.repo = repo;
        this.transactionService = transactionService;
    }

    /**
     * Validate the spring wiring
     */
    @PostConstruct
    public void init() {
        if (repo == null) {
            logger.error("SessionFactory requires a Repository instance!");
            throw new IllegalStateException();
        }
    }

    /**
     * Get a new JCR Session
     * 
     * @return
     * @throws RepositoryException
     */
    public Session getSession() throws RepositoryException {
        return repo.login();
    }

    /**
     * Get a new JCR session in the given workspace
     * 
     * @param workspace
     * @return
     * @throws RepositoryException
     */
    public Session getSession(final String workspace)
        throws RepositoryException {
        return repo.login(workspace);
    }

    /**
     * Get a JCR session for the given HTTP servlet request (within the right
     * transaction or workspace)
     * 
     * @param servletRequest
     * @return
     * @throws RepositoryException
     */
    public Session getSession(final HttpServletRequest servletRequest)
        throws RepositoryException {

        final String workspace = getEmbeddedWorkspace(servletRequest);
        final Transaction transaction = getEmbeddedTransaction(servletRequest);

        final Session session;

        if (transaction != null) {
            logger.debug("Returning a session in the transaction {}",
                    transaction);
            session = transaction.getSession();
        } else if (workspace != null) {
            logger.debug("Returning a session in the workspace {}", workspace);
            session = repo.login(workspace);
        } else {
            logger.debug("Returning a session in the default workspace");
            session = repo.login();
        }

        return session;
    }

    /**
     * Get a JCR session for the given HTTP servlet request with a
     * SecurityContext attached
     * 
     * @param securityContext
     * @param servletRequest
     * @return
     */
    public Session getSession(final SecurityContext securityContext,
            final HttpServletRequest servletRequest) {

        try {
            final ServletCredentials creds =
                    getCredentials(securityContext, servletRequest);

            final Transaction transaction =
                    getEmbeddedTransaction(servletRequest);

            final Session session;

            if (transaction != null && creds != null) {
                logger.debug(
                        "Returning a session in the transaction {} impersonating {}",
                        transaction, creds);
                session = transaction.getSession().impersonate(creds);
            } else if (creds != null) {

                final String workspace = getEmbeddedWorkspace(servletRequest);

                if (workspace != null) {
                    logger.debug(
                            "Returning an authenticated session in the workspace {}",
                            workspace);
                    session = repo.login(creds, workspace);
                } else {
                    logger.debug("Returning an authenticated session in the default workspace");
                    session = repo.login(creds);
                }
            } else {
                logger.debug("Falling back on a unauthenticated session");
                session = getSession(servletRequest);
            }

            return session;
        } catch (final RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the configured Session Provider
     * 
     * @param securityContext
     * @param servletRequest
     * @return
     */
    public AuthenticatedSessionProvider getSessionProvider(
            final SecurityContext securityContext,
            final HttpServletRequest servletRequest) {

        final ServletCredentials creds =
                getCredentials(securityContext, servletRequest);
        return new AuthenticatedSessionProviderImpl(repo, creds);
    }

    /**
     * Extract the workspace id embedded at the beginning of a request
     * 
     * @param request
     * @return
     */
    private String getEmbeddedWorkspace(final HttpServletRequest request) {
        final String requestPath = request.getPathInfo();

        if (requestPath == null) {
            return null;
        }

        final String[] part = requestPath.split("/");

        if (part.length > 1 && part[1].startsWith("workspace:")) {
            return part[1].substring("workspace:".length());
        } else {
            return null;
        }

    }

    /**
     * Extract the transaction id embedded at the beginning of a request
     * 
     * @param servletRequest
     * @return
     * @throws TransactionMissingException
     */
    private Transaction getEmbeddedTransaction(
            final HttpServletRequest servletRequest)
        throws TransactionMissingException {
        final String requestPath = servletRequest.getPathInfo();

        if (requestPath == null) {
            return null;
        }

        final String[] part = requestPath.split("/");

        if (part.length > 1 && part[1].startsWith("tx:")) {
            String txid = part[1].substring("tx:".length());
            return transactionService.getTransaction(txid);
        } else {
            return null;
        }
    }

    /**
     * Get the credentials for an authenticated session
     * 
     * @param securityContext
     * @param servletRequest
     * @return
     */
    private static ServletCredentials getCredentials(
            final SecurityContext securityContext,
            final HttpServletRequest servletRequest) {
        if (securityContext.getUserPrincipal() != null) {
            logger.debug("Authenticated user: " +
                    securityContext.getUserPrincipal().getName());
            return new ServletCredentials(servletRequest);
        } else {
            logger.debug("No authenticated user found!");
            return null;
        }
    }

}
