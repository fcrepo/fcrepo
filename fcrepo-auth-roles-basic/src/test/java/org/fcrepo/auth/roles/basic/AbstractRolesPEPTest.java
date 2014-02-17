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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.auth.roles.common.AbstractRolesPEP;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RepositoryException;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Daines
 */
public class AbstractRolesPEPTest {

    @Mock
    private Principal principalA;

    @Mock
    private Principal principalB;

    @Before
    public void setUp() throws NoSuchFieldException, RepositoryException {
        initMocks(this);
        when(principalA.getName()).thenReturn("a");
        when(principalB.getName()).thenReturn("b");
    }

    @Test
    public void shouldGatherEffectiveRolesFromMultiplePrincipals()
            throws RepositoryException {
        final Map<String, List<String>> acl =
                new HashMap<String, List<String>>();
        acl.put("a", Arrays.asList("reader", "writer"));
        acl.put("b", Arrays.asList("admin"));

        final Set<Principal> principals = new HashSet<Principal>();
        principals.add(principalA);
        principals.add(principalB);

        final Set<String> roles =
                AbstractRolesPEP.resolveUserRoles(acl, principals);
        assertEquals(3, roles.size());
        assertTrue(roles.contains("reader"));
        assertTrue(roles.contains("writer"));
        assertTrue(roles.contains("admin"));
    }

    @Test
    public void shouldHandleUnmatchedRoles() throws RepositoryException {
        final Map<String, List<String>> acl =
                new HashMap<String, List<String>>();

        final Set<Principal> principals = new HashSet<Principal>();
        principals.add(principalA);

        final Set<String> roles =
                AbstractRolesPEP.resolveUserRoles(acl, principals);

        assertEquals(0, roles.size());
    }

}
