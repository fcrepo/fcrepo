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
package org.fcrepo.auth.integration;

import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.Principal;

import javax.inject.Inject;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.auth.BasicUserPrincipal;
import org.fcrepo.auth.common.FedoraAuthorizationDelegate;
import org.fcrepo.auth.common.ServletContainerAuthenticationProvider;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.services.ContainerServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Peter Eichman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-test/mocked-fad-repo-3.xml" })
public class DelegatedUserIT {

    private static Logger logger =
            getLogger(DelegatedUserIT.class);

    @Inject
    private FedoraRepository repo;

    @Inject
    private FedoraAuthorizationDelegate fad;

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    public void testFactory() {
        Assert.assertNotNull(
                "AuthenticationProvider must return a AuthenticationProvider",
                ServletContainerAuthenticationProvider.getInstance());
    }

    @Test
    public void testDelegatedUserAccess() throws RepositoryException {

        // mock request by an admin user, on behalf of a regular user
        when(request.getRemoteUser()).thenReturn("admin1");
        when(request.getUserPrincipal()).thenReturn(new BasicUserPrincipal("admin1"));
        when(request.isUserInRole(eq(ServletContainerAuthenticationProvider.FEDORA_ADMIN_ROLE))).thenReturn(true);
        when(request.getHeader("On-Behalf-Of")).thenReturn("user1");

        Mockito.reset(fad);
        // set up a restrictive mock FAD, which should deny non-admin users
        when(fad.hasPermission(any(Session.class), any(Path.class), any(String[].class))).thenReturn(false);

        final ServletCredentials credentials = new ServletCredentials(request);
        final FedoraSession session = repo.login(credentials);
        final Session jcrSession = getJcrSession(session);
        assertEquals("Session user principal is user1",
                "user1",
                ((Principal) jcrSession.getAttribute(FedoraAuthorizationDelegate.FEDORA_USER_PRINCIPAL)).getName());

        // try to create an object, this should fail because it is being executed as a non-admin user
        final ContainerService os = new ContainerServiceImpl();
        try {
            os.findOrCreate(session, "/myobject");
        } catch (final RepositoryRuntimeException e) {
            final Throwable cause = e.getCause();
            if (cause != null && cause instanceof AccessDeniedException) {
                logger.debug("caught expected access denied exception");
            } else {
                throw e;
            }
        }
        verify(fad, atLeastOnce()).hasPermission(any(Session.class), any(Path.class), any(String[].class));
    }

}
