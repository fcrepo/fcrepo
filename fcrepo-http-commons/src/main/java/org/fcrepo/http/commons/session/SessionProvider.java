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
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.services.BatchService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.slf4j.Logger;

/**
 * Provide a JCR session within the current request context
 *
 * @author awoods
 */
@Provider
@RequestScoped
public class SessionProvider implements Factory<HttpSession> {

    @Inject
    SessionFactory sessionFactory;

    @Inject
    BatchService batchService;

    private HttpServletRequest request;

    /**
     * Create a new session provider for a request
     * @param request the request
     */
    @Inject
    public SessionProvider(final HttpServletRequest request) {
        this.request = request;
    }

    private static final Logger LOGGER = getLogger(SessionProvider.class);

    @Override
    public HttpSession provide() {
        final HttpSession session = sessionFactory.getSession(request);
        LOGGER.trace("Providing new session {}", session);
        return session;
    }

    @Override
    public void dispose(final HttpSession session) {
        LOGGER.trace("Disposing session {}", session);
        session.expire();
    }
}
