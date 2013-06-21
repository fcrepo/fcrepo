
package org.fcrepo.session;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.Transaction;
import org.fcrepo.exception.TransactionMissingException;
import org.fcrepo.services.TransactionService;
import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


public class SessionFactory {

    private static final Logger logger = getLogger(SessionFactory.class);

    @Autowired
    private Repository repo;

    @Autowired
    private TransactionService transactionService;

    public SessionFactory() {

    }

    @PostConstruct
    public void init() {
        if (repo == null) {
            logger.error("SessionFactory requires a Repository instance!");
            throw new IllegalStateException();
        }
    }

    public void setRepository(final Repository repo) {
        this.repo = repo;
    }

    public Session getSession() throws RepositoryException {
        return repo.login();
    }

    public Session getSession(final String workspace)
            throws RepositoryException {
        return repo.login(workspace);
    }

    public Session getSession(final HttpServletRequest servletRequest)
            throws RepositoryException {

        final String workspace = getEmbeddedWorkspace(servletRequest);
        final Transaction transaction = getEmbeddedTransaction(servletRequest);

        final Session session;

        if (transaction != null) {
            logger.debug("Returning a session in the transaction {}", transaction);
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

    public Session getSession(final SecurityContext securityContext,
            final HttpServletRequest servletRequest) {

        try {
            final ServletCredentials creds =
                    getCredentials(securityContext, servletRequest);

            final Transaction transaction = getEmbeddedTransaction(servletRequest);

            final Session session;

            if (transaction != null && creds != null) {
                logger.debug("Returning a session in the transaction {} impersonating {}", transaction, creds);
                session = transaction.getSession().impersonate(creds);
            } else if (creds != null) {

                final String workspace = getEmbeddedWorkspace(servletRequest);

                if (workspace != null) {
                    logger.debug("Returning an authenticated session in the workspace {}", workspace);
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

    public AuthenticatedSessionProvider getSessionProvider(
            final SecurityContext securityContext,
            final HttpServletRequest servletRequest) {

        final ServletCredentials creds =
                getCredentials(securityContext, servletRequest);
        return new AuthenticatedSessionProviderImpl(repo, creds);
    }

    private String getEmbeddedWorkspace(final HttpServletRequest servletRequest) {
        final String requestPath = servletRequest.getPathInfo();

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

    private Transaction getEmbeddedTransaction(final HttpServletRequest servletRequest) throws TransactionMissingException {
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

    public void setTransactionService(final TransactionService transactionService) {
        this.transactionService = transactionService;
    }
}
