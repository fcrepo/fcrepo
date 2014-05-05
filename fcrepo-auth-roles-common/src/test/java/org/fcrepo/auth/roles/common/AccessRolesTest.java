/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.auth.roles.common;

import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.services.NodeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

/**
 * @author bbpennel
 * @date Feb 12, 2014
 */
public class AccessRolesTest {

    @Mock
    private AccessRolesProvider accessRolesProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private NodeService nodeService;

    @Mock
    private FedoraResource fedoraResource;

    @Mock
    private Map<String, List<String>> rolesData;

    private List<PathSegment> paths;

    @Mock
    private PathSegment rootPath;

    private Session session;

    private AccessRoles accessRoles;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        accessRoles = new AccessRoles();
        setField(accessRoles, "accessRolesProvider", accessRolesProvider);
        setField(accessRoles, "request", request);
        setField(accessRoles, "nodeService", nodeService);
        session = mockSession(accessRoles);
        setField(accessRoles, "session", session);

        when(nodeService.getObject(any(Session.class), anyString()))
                .thenReturn(fedoraResource);

        paths = new ArrayList<>();
        when(rootPath.getPath()).thenReturn("");
        paths.add(rootPath);
    }

    @Test
    public void testGetNoData() throws RepositoryException {

        when(accessRolesProvider.getRoles(any(Node.class), anyBoolean()))
                .thenReturn(null);

        final Response response = accessRoles.get(paths, null);

        assertEquals("NoContent response expected when no data found", Response
                .noContent().build().getStatus(), response.getStatus());

        assertNull("Response entity should not have been set", response
                .getEntity());

        verify(session).logout();

        assertEquals("NoContent response expected when no data found", Response
                .noContent().build().getStatus(), response.getStatus());
    }

    @Test
    public void testGetData() throws RepositoryException {

        when(accessRolesProvider.getRoles(any(Node.class), anyBoolean()))
                .thenReturn(rolesData);

        final Response response = accessRoles.get(paths, "");

        assertEquals("Expecting OK response",
                Response.ok().build().getStatus(), response.getStatus());

        assertEquals(
                "Response entity should match the roles data assigned to the node",
                response.getEntity(), rolesData);

        verify(session).logout();

        // Ensure that it attempted to retrieve roles
        verify(accessRolesProvider).getRoles(any(Node.class), anyBoolean());
    }

    @Test(expected = RepositoryException.class)
    public void testGetException() throws RepositoryException {

        when(nodeService.getObject(any(Session.class), anyString())).thenThrow(
                new RepositoryException());

        try {
            accessRoles.get(paths, "");
        } finally {
            // Verify that session logout occurred and no work happened
            verify(session).logout();
            verify(accessRolesProvider, never()).getRoles(any(Node.class),
                    anyBoolean());
        }
    }

    @Test(expected = WebApplicationException.class)
    public void testPostEmptyRoleData() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();

        invalidPost(data);
    }

    @Test(expected = WebApplicationException.class)
    public void testPostEmptyRoleSet() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        data.put("principalName", Collections.<String> emptySet());

        invalidPost(data);
    }

    @Test(expected = WebApplicationException.class)
    public void testPostNullRoleSet() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        data.put("principalName", null);

        invalidPost(data);
    }

    @Test(expected = WebApplicationException.class)
    public void testPostEmptyRole() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        final Set<String> roles = new HashSet<>();
        roles.add(" ");

        data.put("principalName", roles);

        invalidPost(data);
    }

    @Test(expected = WebApplicationException.class)
    public void testPostEmptyPrincipalName() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        final Set<String> roles = new HashSet<>();
        roles.add("role");
        data.put(" ", roles);

        invalidPost(data);
    }

    @Test(expected = WebApplicationException.class)
    public void testPostNullPrincipalName() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        data.put(null, null);

        invalidPost(data);
    }

    private void invalidPost(final Map<String, Set<String>> data)
            throws RepositoryException {

        try {
            accessRoles.post(paths, data);
        } finally {
            // Verify that no work with the provider happened
            verify(accessRolesProvider, never()).postRoles(any(Node.class),
                    Matchers.<Map<String, Set<String>>> any());
            // Verify no changes saved
            verify(session, never()).save();
            verify(session).logout();
        }
    }

    @Test
    public void testApplyNewRoles() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        final Set<String> roles = new HashSet<>();
        roles.add("role1");
        roles.add("role2");

        data.put("principalName", roles);

        final Response response = accessRoles.post(paths, data);

        // Check that work was called
        verify(accessRolesProvider).postRoles(any(Node.class),
                Matchers.<Map<String, Set<String>>> any());
        verify(session).save();
        verify(session).logout();

        assertEquals("Status code must be CREATED", 201, response.getStatus());
        assertEquals("Response path should reference accessroles",
                "/fcrepo/fcr:accessroles", ((URI) response.getMetadata()
                        .getFirst("Location")).getPath());
    }

    @Test(expected = RepositoryException.class)
    public void testApplyRolesException() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();
        final Set<String> roles = new HashSet<>();
        roles.add("role");
        data.put("principalName", roles);

        doThrow(new RepositoryException()).when(accessRolesProvider).postRoles(
                any(Node.class), Matchers.<Map<String, Set<String>>> any());

        try {
            accessRoles.post(paths, data);
        } finally {
            verify(accessRolesProvider).postRoles(any(Node.class),
                    Matchers.<Map<String, Set<String>>> any());
            verify(session, never()).save();
            verify(session).logout();
        }
    }

    @Test
    public void testDeleteRolesAtNode() throws RepositoryException {
        final Response response = accessRoles.deleteNodeType(paths);

        assertEquals("Delete response must be NO CONTENT", 204, response
                .getStatus());

        verify(accessRolesProvider).deleteRoles(any(Node.class));

        verify(session).save();
        verify(session).logout();
    }

    @Test(expected = RepositoryException.class)
    public void testDeleteRolesException() throws RepositoryException {
        doThrow(new RepositoryException()).when(accessRolesProvider)
                .deleteRoles(any(Node.class));

        try {
            accessRoles.deleteNodeType(paths);
        } finally {
            verify(accessRolesProvider).deleteRoles(any(Node.class));

            verify(session, never()).save();
            verify(session).logout();
        }
    }
}
