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

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.value.Path;

import javax.jcr.RepositoryException;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Mike Daines
 */
public class BasicRolesPEPTest {

    private BasicRolesPEP pep;

    @Mock
    private AccessRolesProvider accessRolesProvider;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private JcrSession mockSession;

    @Mock
    private Principal principal;

    private Set<Principal> allPrincipals;

    @Mock
    private Path adminablePath;

    private Map<String, List<String>> adminableAcl;

    @Mock
    private Path writablePath;

    private Map<String, List<String>> writableAcl;

    @Mock
    private Path readablePath;

    private Map<String, List<String>> readableAcl;

    @Mock
    private Path unreadablePath;

    private Map<String, List<String>> unreadableAcl;

    @Mock
    private Path unrecognizablePath;

    private Map<String, List<String>> unrecognizableAcl;

    @Before
    public void setUp() throws NoSuchFieldException, RepositoryException {
        initMocks(this);

        pep = new BasicRolesPEP();
        setField(pep, "accessRolesProvider", accessRolesProvider);
        setField(pep, "sessionFactory", sessionFactory);

        when(sessionFactory.getInternalSession()).thenReturn(mockSession);

        when(principal.getName()).thenReturn("user");
        allPrincipals = Collections.singleton(principal);

        adminableAcl = Collections.singletonMap("user", Arrays.asList("admin"));
        writableAcl = Collections.singletonMap("user", Arrays.asList("writer"));
        readableAcl = Collections.singletonMap("user", Arrays.asList("reader"));
        unreadableAcl =
                Collections.singletonMap("user", Collections
                        .<String> emptyList());
        unrecognizableAcl =
                Collections.singletonMap("user", Arrays
                        .asList("something_else"));

        when(accessRolesProvider.findRolesForPath(adminablePath, mockSession))
                .thenReturn(adminableAcl);
        when(accessRolesProvider.findRolesForPath(writablePath, mockSession))
                .thenReturn(writableAcl);
        when(accessRolesProvider.findRolesForPath(readablePath, mockSession))
                .thenReturn(readableAcl);
        when(accessRolesProvider.findRolesForPath(unreadablePath, mockSession))
                .thenReturn(unreadableAcl);
        when(
                accessRolesProvider.findRolesForPath(unrecognizablePath,
                        mockSession)).thenReturn(unrecognizableAcl);
    }

    @Test
    public void testPermitRemoveChildNodesForRemoveChildNodesAction() {
        assertTrue(pep.hasModeShapePermission(unreadablePath,
                new String[] {"remove_child_nodes"}, allPrincipals, principal));
    }

    @Test
    public void testPermitAnythingForAdminablePath() {
        assertTrue(pep.hasModeShapePermission(adminablePath,
                new String[] {"write"}, allPrincipals, principal));
        assertTrue(pep.hasModeShapePermission(adminablePath,
                new String[] {"read"}, allPrincipals, principal));
        assertTrue(pep.hasModeShapePermission(adminablePath,
                new String[] {"whatever"}, allPrincipals, principal));
    }

    @Test
    public void testPermitReadAndWriteForWritablePath() {
        assertTrue(pep.hasModeShapePermission(writablePath,
                new String[] {"write"}, allPrincipals, principal));
        assertTrue(pep.hasModeShapePermission(writablePath,
                new String[] {"read"}, allPrincipals, principal));
    }

    @Test
    public void testDenyWriteForReadablePath() {
        assertFalse(pep.hasModeShapePermission(readablePath,
                new String[] {"write"}, allPrincipals, principal));
        assertTrue(pep.hasModeShapePermission(readablePath,
                new String[] {"read"}, allPrincipals, principal));
    }

    @Test
    public void testDenyReadAndWriteForUnreadablePath() {
        assertFalse(pep.hasModeShapePermission(unreadablePath,
                new String[] {"write"}, allPrincipals, principal));
        assertFalse(pep.hasModeShapePermission(unreadablePath,
                new String[] {"read"}, allPrincipals, principal));
    }

    @Test
    public void testDenyAllForUnrecognizableRole() {
        assertFalse(pep.hasModeShapePermission(unrecognizablePath,
                new String[] {"write"}, allPrincipals, principal));
        assertFalse(pep.hasModeShapePermission(unrecognizablePath,
                new String[] {"read"}, allPrincipals, principal));
    }

}
