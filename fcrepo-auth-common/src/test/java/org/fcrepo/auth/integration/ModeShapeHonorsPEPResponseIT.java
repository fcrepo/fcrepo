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

package org.fcrepo.auth.integration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.auth.BasicUserPrincipal;
import org.fcrepo.auth.FedoraPolicyEnforcementPoint;
import org.fcrepo.auth.ServletContainerAuthenticationProvider;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gregory Jansen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-test/mocked-pep-repo.xml"})
public class ModeShapeHonorsPEPResponseIT {

    private static Logger logger =
            getLogger(ModeShapeHonorsPEPResponseIT.class);

    @Autowired
    Repository repo;

    @Autowired
    FedoraPolicyEnforcementPoint pep;

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

    @Before
    public void setUp() throws RepositoryException, IOException {
        // final Map<String, String> config = new HashMap<String, String>();
        // config.put(JcrRepositoryFactory.URL,
        // "file:src/test/resources/repository.json");
        // config.put(JcrRepositoryFactory.REPOSITORY_NAME,
        // "fcrepo-secured-repo");
        // repo = new JcrRepositoryFactory().getRepository(config);
    }

    @Test
    public void testPEPFactory() {
        Assert.assertNotNull(
                "AuthenticationProvider must return a AuthenticationProvider",
                ServletContainerAuthenticationProvider.getInstance());
    }

    @Test
    public void testPermissivePEP() throws RepositoryException {
        when(request.getRemoteUser()).thenReturn("fred");
        when(request.getUserPrincipal()).thenReturn(
                new BasicUserPrincipal("fred"));
        when(
                request.isUserInRole(Mockito
                        .eq(ServletContainerAuthenticationProvider.FEDORA_USER_ROLE)))
                .thenReturn(true);
        Mockito.reset(pep);
        when(
                pep.hasModeShapePermission(any(Path.class),
                        any(String[].class), Matchers
                                .<Set<Principal>> any(),
                        any(Principal.class))).thenReturn(true);

        final ServletCredentials credentials =
                new ServletCredentials(request);
        final Session session = repo.login(credentials);
        final Privilege[] rootPrivs =
                session.getAccessControlManager().getPrivileges("/");
        for (final Privilege p : rootPrivs) {
            logger.debug("got priv: " + p.getName());
        }
        final ObjectService os = new ObjectService();
        final FedoraObject fo = os.createObject(session, "/myobject");
        verify(pep, times(5)).hasModeShapePermission(any(Path.class),
                any(String[].class), Matchers.<Set<Principal>> any(),
                any(Principal.class));
    }

    @Test(expected = AccessControlException.class)
    public void testRestrictivePEP() throws RepositoryException {
        when(request.getRemoteUser()).thenReturn("fred");
        when(request.getUserPrincipal()).thenReturn(
                new BasicUserPrincipal("fred"));
        when(
                request.isUserInRole(Mockito
                        .eq(ServletContainerAuthenticationProvider.FEDORA_USER_ROLE)))
                .thenReturn(true);

        // first permission check is for login
        Mockito.reset(pep);
        when(
                pep.hasModeShapePermission(any(Path.class),
                        any(String[].class), Matchers
                                .<Set<Principal>> any(),
                        any(Principal.class))).thenReturn(true, false);

        final ServletCredentials credentials = new ServletCredentials(request);
        final Session session = repo.login(credentials);
        final ObjectService os = new ObjectService();
        final FedoraObject fo = os.createObject(session, "/myobject");
        verify(pep, times(5)).hasModeShapePermission(any(Path.class),
                any(String[].class), Matchers.<Set<Principal>> any(),
                any(Principal.class));
    }
}
