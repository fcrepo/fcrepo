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

/**
 * Provide a JCR session within the current request context
 */
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

    /**
     * Yes, this provider really provides sessions
     */
    public SessionProvider() {
        super(Session.class);
    }

    @Override
    public Injectable<Session> getInjectable(final ComponentContext ic,
            final InjectedSession a) {
        logger.trace("Returning new InjectableSession...");
        return new InjectableSession(sessionFactory, secContext, request);
    }
}
