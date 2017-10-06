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
package org.fcrepo.integration.kernel.modeshape;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.auth.BasicUserPrincipal;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.modeshape.utils.FedoraSessionUserUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.modeshape.jcr.api.ServletCredentials;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>FedoraSessionImplIT class.</p>
 *
 * @author lsitu
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraSessionImplIT extends AbstractIT {

    private static final String TEST_USER_AGENT_BASE_URI = "http://example.com/person#";

    private static final String FEDORA_USER = "fedoraUser";

    @Inject
    FedoraRepository repo;

    private HttpServletRequest request;


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws RepositoryException {
        request = mock(HttpServletRequest.class);
    }

    @After
    public void tearDown() throws RepositoryException {
        System.setProperty(FedoraSessionUserUtil.USER_AGENT_BASE_URI_PROPERTY, "");
    }

    @Test
    public void testGetIdExceptionWithUserIdNonURI() throws RepositoryException {
        when(request.getRemoteUser()).thenReturn(FEDORA_USER);
        when(request.getUserPrincipal()).thenReturn(new BasicUserPrincipal(FEDORA_USER));
        when(request.isUserInRole(eq("admin"))).thenReturn(true);

        final ServletCredentials credentials = new ServletCredentials(request);
        final FedoraSession session = repo.login(credentials);

        // should be the default local user agent URI
        assertEquals("User agent URI invalid.",
        URI.create(FedoraSessionUserUtil.DEFAULT_USER_AGENT_BASE_URI + FEDORA_USER), session.getUserURI());
    }

    @Test
    public void testGetIdWithUserIdNonURI() throws RepositoryException {
        // Set basic URI for user agent with environment variable: fcrepo.auth.webac.userAgent.baseUri
        System.setProperty(FedoraSessionUserUtil.USER_AGENT_BASE_URI_PROPERTY, TEST_USER_AGENT_BASE_URI);

        when(request.getRemoteUser()).thenReturn(FEDORA_USER);
        when(request.getUserPrincipal()).thenReturn(new BasicUserPrincipal(FEDORA_USER));
        when(request.isUserInRole(eq("admin"))).thenReturn(true);

        final ServletCredentials credentials = new ServletCredentials(request);
        final FedoraSession session = repo.login(credentials);

        assertEquals("User agent URI invalid.",
                URI.create(TEST_USER_AGENT_BASE_URI + FEDORA_USER), session.getUserURI());
    }

    @Test
    public void testGetIdWithUserIdURI() throws RepositoryException {

        // test with an absolute user uri
        final String userUri = TEST_USER_AGENT_BASE_URI + FEDORA_USER;
        when(request.getRemoteUser()).thenReturn(userUri);
        when(request.getUserPrincipal()).thenReturn(new BasicUserPrincipal(userUri));
        when(request.isUserInRole(eq("admin"))).thenReturn(true);

        ServletCredentials credentials = new ServletCredentials(request);
        FedoraSession session = repo.login(credentials);

        assertEquals("User agent URI invalid.", URI.create(userUri), session.getUserURI());

        // test with an Opaque user uri
        final String opaqueUserUri = "user:info:" + FEDORA_USER;
        when(request.getRemoteUser()).thenReturn(opaqueUserUri);
        when(request.getUserPrincipal()).thenReturn(new BasicUserPrincipal(opaqueUserUri));
        when(request.isUserInRole(eq("admin"))).thenReturn(true);

        credentials = new ServletCredentials(request);
        session = repo.login(credentials);

        assertEquals("User agent URI invalid.", URI.create(opaqueUserUri), session.getUserURI());
    }
}
