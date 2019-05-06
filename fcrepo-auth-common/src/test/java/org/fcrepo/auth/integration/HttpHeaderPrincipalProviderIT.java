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

import org.apache.http.auth.BasicUserPrincipal;

import org.fcrepo.auth.common.FedoraAuthorizationDelegate;
import org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;
import org.fcrepo.auth.common.ServletContainerAuthenticationProvider;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
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

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Set;

import static org.fcrepo.auth.common.FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS;
import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.FEDORA_USER_ROLE;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author osmandin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-test/mocked-fad-repo-1.xml"})
public class HttpHeaderPrincipalProviderIT {

    private static Logger logger =
            getLogger(HttpHeaderPrincipalProviderIT.class);

    @Inject
    private FedoraRepository repo;

    @Inject
    private FedoraAuthorizationDelegate fad;

    @Test
    public void testFactory() {
        Assert.assertNotNull(
                "AuthenticationProvider must return a AuthenticationProvider",
                ServletContainerAuthenticationProvider.getInstance());
    }

    private HttpServletRequest createMockRequest(final String username, final HashMap<String, String> headers) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteUser()).thenReturn(username);
        if (username != null) {
            when(request.getUserPrincipal()).thenReturn(new BasicUserPrincipal(username));
        }
        when(request.isUserInRole(Mockito.eq(FEDORA_USER_ROLE))).thenReturn(true);
        if (headers != null) {
            for (final String headerName : headers.keySet()) {
                when(request.getHeader(headerName)).thenReturn(headers.get(headerName));
            }
        }

        return request;
    }

    private FedoraSession processRequest(final HttpServletRequest request) {
        Mockito.reset(fad);
        when(fad.hasPermission(any(Session.class), any(Path.class), any(String[].class))).thenReturn(true);
        when(fad.getEveryonePrincipal()).thenReturn(new BasicUserPrincipal("EVERYONE"));

        final ServletCredentials credentials = new ServletCredentials(request);
        return repo.login(credentials);
    }

    @Test
    public void testEmptyPrincipalProvider() throws RepositoryException {
        final HttpServletRequest request = createMockRequest("fred", null);
        final FedoraSession session = processRequest(request);

        final Privilege[] rootPrivs = getJcrSession(session).getAccessControlManager().getPrivileges("/");
        for (final Privilege p : rootPrivs) {
            logger.debug("got priv: " + p.getName());
        }
        final ContainerService os = new ContainerServiceImpl();
        os.findOrCreate(session, "/myobject");
        verify(fad, atLeastOnce()).hasPermission(any(Session.class), any(Path.class), any(String[].class));
    }

    @Test
    public void testHeadersWithUserPrincipal() {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("test", "otherPrincipal");
        final HttpServletRequest request = createMockRequest("fred", headers);
        final Session jcrSession = getJcrSession(processRequest(request));
        final Set allPrincipals = (Set) jcrSession.getAttribute(FEDORA_ALL_PRINCIPALS);

        assertEquals(3, allPrincipals.size());
        assertTrue(allPrincipals.contains(new BasicUserPrincipal("fred")));
        assertTrue(allPrincipals.contains(new HttpHeaderPrincipal("otherPrincipal")));
        assertTrue(allPrincipals.contains(fad.getEveryonePrincipal()));
    }

    @Test
    public void testHeadersWithoutUserPrincipal() {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("test", "otherPrincipal");
        final HttpServletRequest request = createMockRequest(null, headers);
        final Session jcrSession = getJcrSession(processRequest(request));
        final Set allPrincipals = (Set) jcrSession.getAttribute(FEDORA_ALL_PRINCIPALS);

        assertEquals(2, allPrincipals.size());
        assertTrue(allPrincipals.contains(new HttpHeaderPrincipal("otherPrincipal")));
        assertTrue(allPrincipals.contains(fad.getEveryonePrincipal()));
    }

}
