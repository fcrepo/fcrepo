
package org.fcrepo.session;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;

import com.sun.jersey.spi.inject.Injectable;

public class InjectableSession implements Injectable<Session> {

    private SessionFactory sessionFactory;

    private SecurityContext secContext;

    private HttpServletRequest request;

    private static final Logger logger = getLogger(InjectableSession.class);

    public InjectableSession(final SessionFactory sessionFactory,
            final SecurityContext reqContext, final HttpServletRequest request) {
        checkNotNull(sessionFactory, "SessionFactory cannot be null!");
        checkNotNull(reqContext, "HttpRequestContext cannot be null!");
        checkNotNull(request, "HttpServletRequest cannot be null!");
        logger.debug(
                "Initializing an InjectableSession with SessionFactory {}.",
                sessionFactory);
        this.sessionFactory = sessionFactory;
        this.secContext = reqContext;
        this.request = request;

    }

    @Override
    public Session getValue() {
        if (secContext.getUserPrincipal() != null) {
            logger.debug("Returning authenticated Session.");
            return sessionFactory.getSession(secContext, request);
        } else {
            logger.debug("Returning unauthenticated Session.");
            try {
                return sessionFactory.getSession(request);
            } catch (final RepositoryException e) {
                propagate(e);
            }
        }
        throw new RuntimeException(
                "Couldn't generate an appropriate session from HTTP Request: " +
                        request);
    }

}