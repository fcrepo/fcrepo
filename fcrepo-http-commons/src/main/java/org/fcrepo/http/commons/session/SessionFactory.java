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

import org.fcrepo.kernel.LockReleasingSession;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.fcrepo.kernel.services.TransactionService;
import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Factory for generating sessions for HTTP requests, taking
 * into account transactions, workspaces, and authentication.
 *
 * @author awoods
 * @author gregjan
 * @author kaisternad
 */
public class SessionFactory {

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
     * @param workspace
     * @return
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
     * @return
     */
    public Session getSession(final HttpServletRequest servletRequest) {

        try {
            final ServletCredentials creds =
                    new ServletCredentials(servletRequest);

            final Transaction transaction =
                    getEmbeddedTransaction(servletRequest);

            final Session session;
            final Principal userPrincipal = servletRequest.getUserPrincipal();

            boolean isUserAuthorizedForTransaction = false;

            if (transaction != null) {

                if (userPrincipal != null) {
                    String userName = userPrincipal.getName();
                    isUserAuthorizedForTransaction = transactionService.isAssociatedWithUser(
                            transaction.getId(), userName);
                } else {
                    // Anonymous case
                    isUserAuthorizedForTransaction = true;
                }
            }


            if (transaction != null && isUserAuthorizedForTransaction) {
                LOGGER.debug(
                                "Returning a session in the transaction {}",
                                transaction);
                session = transaction.getSession();
            } else {

                final String workspace =
                        getEmbeddedWorkspace(servletRequest);

                if (workspace != null) {
                    LOGGER.debug(
                                    "Returning an authenticated session in the workspace {}",
                                    workspace);
                    session = repo.login(creds, workspace);
                } else {
                    LOGGER.debug("Returning an authenticated session in the default workspace");
                    session = repo.login(creds);
                }
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
        }
        return null;

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
            final String txid = part[1].substring("tx:".length());
            return transactionService.getTransaction(txid);
        }
        return null;
    }

}
