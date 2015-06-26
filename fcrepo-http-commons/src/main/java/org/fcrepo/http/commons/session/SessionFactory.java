/**
 * Copyright 2015 DuraSpace, Inc.
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

import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.Response.Status.GONE;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.services.TransactionService;

import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;

/**
 * Factory for generating sessions for HTTP requests, taking
 * into account transactions and authentication.
 *
 * @author awoods
 * @author gregjan
 * @author kaisternad
 */
public class SessionFactory {

    protected static enum Prefix{
        TX("tx:");

        private final String prefix;

        Prefix(final String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private static final Logger LOGGER = getLogger(SessionFactory.class);

    @Inject
    private Repository repo;

    @Inject
    private TransactionService transactionService;

    /**
     * Default constructor
     */
    public SessionFactory() {
    }

    /**
     * Initialize a session factory for the given Repository
     *
     * @param repo the repository
     * @param transactionService the transaction service
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
        requireNonNull(repo, "SessionFactory requires a Repository instance!");
    }

    /**
     * Get a new JCR Session
     *
     * @return an internal session
     */
    public Session getInternalSession() {
        try {
            return repo.login();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get a JCR session for the given HTTP servlet request with a
     * SecurityContext attached
     *
     * @param servletRequest the servlet request
     * @return the Session
     * @throws RuntimeException if the transaction could not be found
     */
    public Session getSession(final HttpServletRequest servletRequest) {
        final Session session;
        final String txId = getEmbeddedId(servletRequest, Prefix.TX);

        try {
            if (txId == null) {
                session = createSession(servletRequest);
            } else {
                session = getSessionFromTransaction(servletRequest, txId);
            }
        } catch (final TransactionMissingException e) {
            throw new ClientErrorException(GONE, e);
        } catch (final RepositoryException e) {
            throw new BadRequestException(e);
        }

        return session;
    }

    /**
     * Create a JCR session for the given HTTP servlet request with a
     * SecurityContext attached.
     *
     * @param servletRequest the servlet request
     * @return a newly created JCR session
     * @throws RepositoryException if the session could not be created
     */
    protected Session createSession(final HttpServletRequest servletRequest) throws RepositoryException {

        final ServletCredentials creds =
                new ServletCredentials(servletRequest);

        LOGGER.debug("Returning an authenticated session in the default workspace");
        return  repo.login(creds);
    }

    /**
     * Retrieve a JCR session from an active transaction
     *
     * @param servletRequest the servlet request
     * @param txId the transaction id
     * @return a JCR session that is associated with the transaction
     */
    protected Session getSessionFromTransaction(final HttpServletRequest servletRequest, final String txId) {

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
     * @param servletRequest the servlet request
     * @param prefix the prefix for the id
     * @return the found id or null
     */
    protected String getEmbeddedId(
            final HttpServletRequest servletRequest, final Prefix prefix) {
        String requestPath = servletRequest.getPathInfo();

        // http://stackoverflow.com/questions/18963562/grizzlys-request-getpathinfo-returns-always-null
        if (requestPath == null && servletRequest.getContextPath().isEmpty()) {
            requestPath = servletRequest.getRequestURI();
        }

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
