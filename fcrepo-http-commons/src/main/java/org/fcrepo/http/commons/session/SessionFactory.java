/**
 * Copyright 2014 DuraSpace, Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.kernel.LockReleasingSession;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.services.TransactionService;
import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for generating sessions for HTTP requests, taking
 * into account transactions, workspaces, and authentication.
 *
 * @author awoods
 * @author gregjan
 * @author kaisternad
 */
public class SessionFactory {

    protected static enum Prefix{
        WORKSPACE("workspace:"), TX("tx:");

        private final String prefix;

        Prefix(final String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private static final Logger LOGGER = getLogger(SessionFactory.class);

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
        checkNotNull(repo, "SessionFactory requires a Repository instance!");
    }

    /**
     * Get a new JCR Session
     *
     * @return
     * @throws RepositoryException
     */
    public Session getInternalSession() throws RepositoryException {
        return repo.login();
    }

    /**
     * Get a new JCR session in the given workspace
     *
     * @param workspace the String containing the workspace name
     * @return a Session for the workspace
     * @throws RepositoryException
     */
    public Session getInternalSession(final String workspace)
        throws RepositoryException {
        return repo.login(workspace);
    }

    /**
     * Get a JCR session for the given HTTP servlet request with a
     * SecurityContext attached
     *
     * @param servletRequest
     * @return the Session
     * @throws RuntimeException if the transaction could not be found
     */
    public Session getSession(final HttpServletRequest servletRequest) {
        try {
            final Session session;
            final String txId = getEmbeddedId(servletRequest, Prefix.TX);

            if (txId == null) {
                session = createSession(servletRequest);
            } else {
                session = getSessionFromTransaction(servletRequest, txId);
            }

            final String lockToken = servletRequest.getHeader("Lock-Token");
            if (lockToken != null) {
                session.getWorkspace().getLockManager().addLockToken(lockToken);
            }

            return LockReleasingSession.newInstance(session);

        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    /**
     * Create a JCR session for the given HTTP servlet request with a
     * SecurityContext attached. If a workspace id is part of the request,
     * it will be used for the session.
     *
     * @param servletRequest
     * @return a newly created JCR session
     * @throws RepositoryException if the session could not be created
     */
    protected Session createSession(final HttpServletRequest servletRequest) throws RepositoryException {

        final ServletCredentials creds =
                new ServletCredentials(servletRequest);

        final String workspace =
                getEmbeddedId(servletRequest, Prefix.WORKSPACE);

        final Session session;
        if (workspace != null) {
            LOGGER.debug(
                    "Returning an authenticated session in the workspace {}",
                    workspace);
            session = repo.login(creds, workspace);
        } else {
            LOGGER.debug("Returning an authenticated session in the default workspace");
            session = repo.login(creds);
        }
        return session;
    }

    /**
     * Retrieve a JCR session from an active transaction
     *
     * @param servletRequest
     * @return a JCR session that is associated with the transaction
     * @throws RepositoryException if the session could not be found for the given tx
     */
    protected Session getSessionFromTransaction(final HttpServletRequest servletRequest, final String txId)
        throws RepositoryException {

        final Principal userPrincipal = servletRequest.getUserPrincipal();

        String userName = null;
        if (userPrincipal != null) {
            userName = userPrincipal.getName();
        }

        final Transaction transaction =
                transactionService.getTransaction(txId, userName);
        LOGGER.debug(
                "Returning a session in the transaction {} for user {}",
                transaction, userName);
        return transaction.getSession();

    }

    /**
     * Extract the id embedded at the beginning of a request path
     *
     * @param servletRequest
     * @param prefix, the prefix for the id
     * @return the found id or null
     */
    protected String getEmbeddedId(
            final HttpServletRequest servletRequest, final Prefix prefix) {
        final String requestPath = servletRequest.getPathInfo();

        String id = null;
        if (requestPath != null) {
            final String pathPrefix = prefix.getPrefix();
            final String[] part = requestPath.split("/");
            if (part.length > 1 && part[1].startsWith(pathPrefix)) {
                id = part[1].substring(pathPrefix.length());
            }
        }
        return id;
    }

}
