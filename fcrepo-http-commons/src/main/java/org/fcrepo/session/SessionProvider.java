
package org.fcrepo.session;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;

@Provider
public class SessionProvider extends
        PerRequestTypeInjectableProvider<InjectedSession, Session> {

    @Autowired
    @InjectParam
    SessionFactory sessionFactory;

    @Context
    private SecurityContext secContext;

    @Context
    private HttpServletRequest request;

    private static final Logger logger = getLogger(SessionProvider.class);

    public SessionProvider() {
        super(Session.class);
    }

    @Override
    public Injectable<Session> getInjectable(final ComponentContext ic,
            final InjectedSession a) {
        logger.trace("Returning new InjectableSession...");
        return new InjectableSession(sessionFactory, secContext, request);
    }

    public void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setSecContext(final SecurityContext secContext) {
        this.secContext = secContext;
    }

    public void setRequest(final HttpServletRequest request) {
        this.request = request;
    }
}
