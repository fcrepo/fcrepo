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
package org.fcrepo.http.commons.session;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.SessionMissingException;

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

    protected enum Prefix{
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
    private TransactionManager txManager;

    /**
     * Initialize a session factory
     */
    public SessionFactory() { }

    /**
     * Get a new fedora transaction
     *
     * @return an fedora transaction
     */
    public Transaction getNewTransaction() { // Should this be read-only?
        return txManager.create();
    }

    /**
     * Get a JCR session for the given HTTP servlet request with a
     * SecurityContext attached
     *
     * @param servletRequest the servlet request
     * @return the Session
     * @throws RuntimeException if the transaction could not be found
     */
    public HttpSession getSession(final HttpServletRequest servletRequest) {
        final HttpSession session;
        final String txId = getEmbeddedId(servletRequest, Prefix.TX);

        try {
            if (txId == null) {
                session = createSession(servletRequest);
            } else {
                session = getSessionFromTransaction(txId);
            }
        } catch (final SessionMissingException e) {
            LOGGER.warn("Transaction missing: {}", e.getMessage());
            return null;
        }

        return session;
    }

    /**
     * Create a JCR session for the given HTTP servlet request with a
     * SecurityContext attached.
     *
     * @param servletRequest the servlet request
     * @return a newly created JCR session
     */
    protected HttpSession createSession(final HttpServletRequest servletRequest) {

        LOGGER.debug("Returning an authenticated session in the default workspace");
        return  new HttpSession(txManager.create());
    }

    /**
     * Retrieve a JCR session from an active transaction
     *
     * @param txId the transaction id
     * @return a JCR session that is associated with the transaction
     */
    protected HttpSession getSessionFromTransaction(final String txId) {
        return new HttpSession(txManager.get(txId));
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
