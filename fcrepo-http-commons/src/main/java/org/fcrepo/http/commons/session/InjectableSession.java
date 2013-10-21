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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;

import com.sun.jersey.spi.inject.Injectable;

/**
 * Scary JAX-RS magic to inject a JCR Session as a field for every request
 */
public class InjectableSession implements Injectable<Session> {

    private SessionFactory sessionFactory;

    private SecurityContext secContext;

    private HttpServletRequest request;

    private static final Logger logger = getLogger(InjectableSession.class);

    /**
     * Construct our request-context and authorization-context aware session
     * factory
     *
     * @param sessionFactory
     * @param reqContext
     * @param request
     */
    public InjectableSession(final SessionFactory sessionFactory,
            final SecurityContext reqContext,
            final HttpServletRequest request) {
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
        return sessionFactory.getSession(secContext, request);
    }

}