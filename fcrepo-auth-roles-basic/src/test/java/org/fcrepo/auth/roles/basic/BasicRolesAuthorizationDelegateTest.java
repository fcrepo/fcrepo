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

package org.fcrepo.auth.roles.basic;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.fcrepo.auth.common.FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS;
import static org.fcrepo.auth.common.FedoraAuthorizationDelegate.FEDORA_USER_PRINCIPAL;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.fcrepo.auth.roles.common.Constants.JcrName;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.Path;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Daines
 */
public class BasicRolesAuthorizationDelegateTest {

    private static final String[] READ_ACTION = {"read"};

    private static final String[] WRITE_ACTION = {"write"};

    private BasicRolesAuthorizationDelegate authorizationDelegate;

    @Mock
    private AccessRolesProvider accessRolesProvider;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session mockSession;

    @Mock
    private Principal principal;

    private Set<Principal> allPrincipals;

    @Mock
    private Path adminablePath;

    @Mock
    private Path writablePath;

    @Mock
    private Path readablePath;

    @Mock
    private Path unreadablePath;

    @Mock
    private Path unrecognizablePath;

    @Mock
    private Path authzPath;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        authorizationDelegate = new BasicRolesAuthorizationDelegate();
        setField(authorizationDelegate, "accessRolesProvider",
                accessRolesProvider);
        setField(authorizationDelegate, "sessionFactory", sessionFactory);

        when(sessionFactory.getInternalSession()).thenReturn(mockSession);

        when(principal.getName()).thenReturn("user");
        allPrincipals = singleton(principal);

        when(mockSession.getAttribute(FEDORA_USER_PRINCIPAL)).thenReturn(
                principal);
        when(mockSession.getAttribute(FEDORA_ALL_PRINCIPALS)).thenReturn(
                allPrincipals);

        // ACLs for paths

        final Map<String, List<String>> adminAcl =
                singletonMap("user", asList("admin"));
        final Map<String, List<String>> writerAcl =
                singletonMap("user", asList("writer"));
        final Map<String, List<String>> readerAcl =
                singletonMap("user", asList("reader"));
        final Map<String, List<String>> emptyAcl =
                singletonMap("user", Collections.<String> emptyList());
        final Map<String, List<String>> unrecognizableAcl =
                singletonMap("user", asList("something_else"));

        when(accessRolesProvider.findRolesForPath(adminablePath, mockSession))
                .thenReturn(adminAcl);
        when(accessRolesProvider.findRolesForPath(writablePath, mockSession))
                .thenReturn(writerAcl);
        when(accessRolesProvider.findRolesForPath(readablePath, mockSession))
                .thenReturn(readerAcl);
        when(accessRolesProvider.findRolesForPath(unreadablePath, mockSession))
                .thenReturn(emptyAcl);
        when(
                accessRolesProvider.findRolesForPath(unrecognizablePath,
                        mockSession)).thenReturn(unrecognizableAcl);
        when(accessRolesProvider.findRolesForPath(authzPath, mockSession))
                .thenReturn(writerAcl);

        // Identify authzPath as an ACL node

        final String authzDetection = "/{" + JcrName.NS_URI + "}";
        when(authzPath.toString()).thenReturn("/blah" + authzDetection);
    }

    @Test
    public void testPermitRemoveChildNodesForRemoveChildNodesAction() {
        assertTrue(authorizationDelegate.hasPermission(mockSession,
                unreadablePath, new String[] {"remove_child_nodes"}));
    }

    @Test
    public void testPermitAnythingForAdminablePath() {
        assertTrue("Should permit write action for path with admin role",
                authorizationDelegate.hasPermission(mockSession, adminablePath,
                        WRITE_ACTION));
        assertTrue("Should permit read action for path with admin role",
                authorizationDelegate.hasPermission(mockSession, adminablePath,
                        READ_ACTION));
        assertTrue(
                "Should permit another arbitrary action for path with admin role",
                authorizationDelegate.hasPermission(mockSession, adminablePath,
                        new String[] {"whatever"}));
    }

    @Test
    public void testPermitReadAndWriteForWritablePath() {
        assertTrue("Should permit write for path with writer role",
                authorizationDelegate.hasPermission(mockSession, writablePath,
                        WRITE_ACTION));
        assertTrue("Should permit read for path with writer role",
                authorizationDelegate.hasPermission(mockSession, writablePath,
                        READ_ACTION));
    }

    @Test
    public void testDenyWriteForReadablePath() {
        assertFalse("Should deny write for path with reader role",
                authorizationDelegate.hasPermission(mockSession, readablePath,
                        WRITE_ACTION));
        assertTrue("Should permit read for path with reader role",
                authorizationDelegate.hasPermission(mockSession, readablePath,
                        READ_ACTION));
    }

    @Test
    public void testDenyReadAndWriteForUnreadablePath() {
        assertFalse("Should deny write for path with no roles",
                authorizationDelegate.hasPermission(mockSession,
                        unreadablePath, WRITE_ACTION));
        assertFalse("Should deny read for path with no roles",
                authorizationDelegate.hasPermission(mockSession,
                        unreadablePath, READ_ACTION));
    }

    @Test
    public void testDenyAllForUnrecognizableRole() {
        assertFalse("Should deny write for path with unrecognizable role",
                authorizationDelegate.hasPermission(mockSession,
                        unrecognizablePath, WRITE_ACTION));
        assertFalse("Should deny read for path with unrecognizable role",
                authorizationDelegate.hasPermission(mockSession,
                        unrecognizablePath, READ_ACTION));
    }

    @Test
    public void testDenyWriteToWriterForAuthzPath() {
        assertFalse("Should deny write for ACL path", authorizationDelegate
                .hasPermission(mockSession, authzPath, WRITE_ACTION));
    }

}
