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

import static org.fcrepo.auth.roles.common.Constants.JcrName.Rbacl;
import static org.fcrepo.auth.roles.common.Constants.JcrName.principal;
import static org.fcrepo.auth.roles.common.Constants.JcrName.rbacl;
import static org.fcrepo.auth.roles.common.Constants.JcrName.rbaclAssignable;
import static org.fcrepo.auth.roles.common.Constants.JcrName.role;
import static org.fcrepo.kernel.impl.testutilities.TestNodeIterator.nodeIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.auth.roles.common.Constants.JcrName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.Path;

/**
 * @author bbpennel
 * @since Feb 14, 2014
 */
public class AccessRolesProviderTest {

    @Mock
    private Session session;

    @Mock
    private Node node;

    @Mock
    private Node rbaclNode;

    @Mock
    private Node principalNode1;

    @Mock
    private Property principalProperty1;

    private NodeIterator rbaclIterator;

    private AccessRolesProvider provider;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(node.getSession()).thenReturn(session);

        provider = new AccessRolesProvider();

        setupPrincipalNode(principalNode1, principalProperty1, "principal",
                "role");

        // Set up parent node acl node
        rbaclIterator = nodeIterator(principalNode1);
        when(rbaclNode.getNodes()).thenReturn(rbaclIterator);

        when(node.isNodeType(anyString())).thenReturn(false);
    }

    private void setupPrincipalNode(final Node principalNode,
            final Property principalProperty, final String principalName,
            final String roleName) throws RepositoryException {
        // Set up principal for parent node
        when(principalProperty.getString()).thenReturn(principalName);
        when(principalNode.getProperty(eq(principal.getQualified())))
                .thenReturn(principalProperty);

        // Roles for parent
        final Property roleProperty = mock(Property.class);
        final Value roleValue = mock(Value.class);
        when(roleValue.toString()).thenReturn(roleName);
        when(roleProperty.getValues()).thenReturn(new Value[] {roleValue});
        when(principalNode.getProperty(eq(role.getQualified())))
                .thenReturn(roleProperty);
    }

    @Test
    public void testGetRolesNoRBACLs() throws RepositoryException {
        when(node.isNodeType(anyString())).thenReturn(true);
        when(node.getNode(anyString())).thenReturn(rbaclNode);

        rbaclIterator = nodeIterator();
        when(rbaclNode.getNodes()).thenReturn(rbaclIterator);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertTrue(
                "Role response for node with no rbacl nodes should be empty",
                data.isEmpty());
    }

    @Test
    public void testGetRolesRBACLsNullPrincipalName()
            throws RepositoryException {
        when(node.isNodeType(anyString())).thenReturn(true);
        when(node.getNode(anyString())).thenReturn(rbaclNode);

        when(principalProperty1.getString()).thenReturn(null);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertTrue(
                "Role response for node with an rbacl nodes containing no principal names should be empty",
                data.isEmpty());
    }

    @Test
    public void testGetRolesRBACLsNoPrincipalName() throws RepositoryException {
        when(node.isNodeType(anyString())).thenReturn(true);
        when(node.getNode(anyString())).thenReturn(rbaclNode);

        when(principalProperty1.getString()).thenReturn("");

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertTrue(
                "Role response for node with an rbacl nodes containing no principal names should be empty",
                data.isEmpty());
    }

    @Test
    public void testGetRolesRBACLPathNotFound() throws RepositoryException {
        when(node.isNodeType(anyString())).thenReturn(true);

        when(node.getNode(anyString())).thenThrow(new PathNotFoundException());

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertTrue("Roles data must be empty when rbacl path is not found",
                data.isEmpty());
    }

    @Test
    public void testGetRolesNotAssignableNotEffective()
            throws RepositoryException {

        final Map<String, List<String>> data = provider.getRoles(node, false);

        assertNull(
                "Role data should be null when retrieving from a non-assignable node",
                data);
    }

    @Test
    public void testGetRolesEffectiveNoParent() throws RepositoryException {

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertNull(
                "Role data should be null when retrieving from non-assignable node with no parent",
                data);
    }

    @Test
    public void testGetRolesEffectiveParentNotAssignable()
            throws RepositoryException {

        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(false);

        when(node.getParent()).thenReturn(parentNode1);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertNull(
                "Role data should be null when a node and all its ancestors are not assignable",
                data);
    }

    @Test
    public void testGetRolesEffectiveParentNotFound()
            throws RepositoryException {
        when(node.isNodeType(anyString())).thenReturn(false);

        when(node.getParent()).thenThrow(new ItemNotFoundException());

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertTrue(
                "Result role data should be the default access roles object",
                AccessRolesProvider.DEFAULT_ACCESS_ROLES == data);
    }

    @Test
    public void testGetRolesEffectiveImmediateParent()
            throws RepositoryException {

        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        when(node.getParent()).thenReturn(parentNode1);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("One principal should be retrieved", 1, data.size());
        assertTrue("Data did not contain principal", data
                .containsKey("principal"));
        assertEquals("Role for principal did not match", "role", data.get(
                "principal").get(0));
    }

    @Test
    public void testGetRolesEffectiveAncestorParent()
            throws RepositoryException {

        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        // Set up immediate parent node
        final Node parentNode2 = mock(Node.class);
        when(parentNode2.isNodeType(anyString())).thenReturn(false);
        when(parentNode2.getParent()).thenReturn(parentNode1);

        when(node.getParent()).thenReturn(parentNode2);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("One principal should be retrieved", 1, data.size());
        assertTrue("Data did not contain principal", data
                .containsKey("principal"));
        assertEquals("Role for principal did not match", "role", data.get(
                "principal").get(0));
    }

    @Test
    public void testGetRolesEffectiveAssignableParentsSamePrincipal()
            throws RepositoryException {

        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(eq(rbaclAssignable.getQualified())))
                .thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        // Set up immediate parent node
        final Node parentNode2 = mock(Node.class);
        when(parentNode2.isNodeType(eq(rbaclAssignable.getQualified())))
                .thenReturn(true);
        when(parentNode2.getNode(anyString())).thenReturn(rbaclNode);
        when(parentNode2.getParent()).thenReturn(parentNode1);

        when(node.getParent()).thenReturn(parentNode2);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("One principal should be retrieved", 1, data.size());
        assertTrue("Data did not contain principal", data
                .containsKey("principal"));
        assertEquals(
                "Principal should only contain role from immediate parent", 1,
                data.get("principal").size());

        // Verify that the distant parent never attempted to get assignments
        verify(parentNode1, never()).getNode(anyString());
    }

    @Test
    public void testGetRolesEffectiveMultipleRoles()
            throws RepositoryException {

        final Node principalNode2 = mock(Node.class);
        final Property principalProperty2 = mock(Property.class);

        setupPrincipalNode(principalNode2, principalProperty2, "principal",
                "role2");

        // Set up parent node acl node
        rbaclIterator = nodeIterator(principalNode1, principalNode2);
        when(rbaclNode.getNodes()).thenReturn(rbaclIterator);

        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        when(node.getParent()).thenReturn(parentNode1);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("One principal should be retrieved", 1, data.size());
        assertTrue("Data did not contain principal", data
                .containsKey("principal"));
        assertEquals("Principal should contain two roles", 2, data.get(
                "principal").size());
        assertTrue("Principal should be assigned 'role'", data.get("principal")
                .contains("role"));
        assertTrue("Principal should be assigned 'role2'", data
                .get("principal").contains("role2"));
    }

    @Test
    public void testGetRolesEffectiveMultiplePrincipals()
            throws RepositoryException {

        final Node principalNode2 = mock(Node.class);
        final Property principalProperty2 = mock(Property.class);

        setupPrincipalNode(principalNode2, principalProperty2, "principal2",
                "role");

        // Set up parent node acl node
        rbaclIterator = nodeIterator(principalNode1, principalNode2);
        when(rbaclNode.getNodes()).thenReturn(rbaclIterator);

        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        when(node.getParent()).thenReturn(parentNode1);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("One principal should be retrieved", 2, data.size());
        assertTrue("Data did not contain principal", data
                .containsKey("principal"));
        assertTrue("Data did not contain principal2", data
                .containsKey("principal2"));
        assertEquals("Principal should contain only one role", 1, data.get(
                "principal").size());
        assertEquals("Principal2 should contain only one role", 1, data.get(
                "principal2").size());
        assertTrue(data.get("principal").contains("role"));
        assertTrue(data.get("principal2").contains("role"));
    }

    @Test
    public void testGetRolesNullRoleValue() throws RepositoryException {
        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        when(node.getParent()).thenReturn(parentNode1);

        // Roles for parent
        final Property roleProperty = mock(Property.class);
        when(roleProperty.getValues()).thenReturn(new Value[] {null});
        when(principalNode1.getProperty(eq(role.getQualified())))
                .thenReturn(roleProperty);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("Data should contain one principal", 1, data.size());
        assertEquals("Principal should not contain any roles", 0, data.get(
                "principal").size());
    }

    @Test
    public void testGetRolesEmptyRoleValue() throws RepositoryException {
        // Set up parent node
        final Node parentNode1 = mock(Node.class);
        when(parentNode1.isNodeType(anyString())).thenReturn(true);
        when(parentNode1.getNode(anyString())).thenReturn(rbaclNode);

        when(node.getParent()).thenReturn(parentNode1);

        // Roles for parent
        final Property roleProperty = mock(Property.class);
        final Value roleValue = mock(Value.class);
        when(roleValue.toString()).thenReturn("");
        when(roleProperty.getValues()).thenReturn(new Value[] {roleValue});
        when(principalNode1.getProperty(eq(JcrName.role.getQualified())))
                .thenReturn(roleProperty);

        final Map<String, List<String>> data = provider.getRoles(node, true);

        assertEquals("Data should contain one principal", 1, data.size());
        assertEquals("Principal should not contain any roles", 0, data.get(
                "principal").size());
    }

    @Test
    public void testPostRolesNonassignableEmptyData()
            throws RepositoryException {

        final Node aclNode = mock(Node.class);
        when(node.addNode(anyString(), anyString())).thenReturn(aclNode);

        final Map<String, Set<String>> data =
                new HashMap<>();
        provider.postRoles(node, data);

        // Node should be given node types to make it assignable
        verify(node).addMixin(eq(JcrName.rbaclAssignable.getQualified()));
        verify(node).addNode(eq(JcrName.rbacl.getQualified()),
                eq(JcrName.Rbacl.getQualified()));
        // No acl assignments should be made since data was empty
        verify(aclNode, never()).addNode(anyString());
    }

    @Test
    public void testPostRolesEmptyDataToRBACLAssignable()
            throws RepositoryException {
        final Map<String, Set<String>> data =
                new HashMap<>();

        when(node.isNodeType(rbaclAssignable.getQualified()))
                .thenReturn(true);

        provider.postRoles(node, data);

        // Mixin should not be added
        verify(node, never()).addMixin(
                eq(rbaclAssignable.getQualified()));
        verify(node).addNode(eq(rbacl.getQualified()),
                eq(Rbacl.getQualified()));
    }

    @Test
    public void testPostRolesToNonRBACLNode() throws RepositoryException {
        final Map<String, Set<String>> data = new HashMap<>();
        final Set<String> roles = new HashSet<>();
        roles.add("role");
        data.put("principal", roles);

        final Node aclNode = mock(Node.class);
        when(node.addNode(anyString(), anyString())).thenReturn(aclNode);
        final Node assignNode = mock(Node.class);
        when(aclNode.addNode(anyString(), anyString())).thenReturn(assignNode);

        provider.postRoles(node, data);

        // Verify that node was setup to be assignable
        verify(node).addMixin(eq(JcrName.rbaclAssignable.getQualified()));
        verify(node).addNode(eq(JcrName.rbacl.getQualified()),
                eq(JcrName.Rbacl.getQualified()));

        // Verify that the new principals and roles were added
        verify(aclNode).addNode(eq(JcrName.assignment.getQualified()),
                eq(JcrName.Assignment.getQualified()));
        verify(assignNode).setProperty(eq(JcrName.principal.getQualified()),
                eq("principal"));
        verify(assignNode).setProperty(eq(JcrName.role.getQualified()),
                any(String[].class));
    }

    @Test
    public void testPostRolesToRBACLNode() throws RepositoryException {

        final Map<String, Set<String>> data = new HashMap<>();

        when(node.hasNode(eq(rbacl.getQualified()))).thenReturn(true);

        final Node aclNode = mock(Node.class);
        when(aclNode.getNodes()).thenReturn(rbaclIterator);
        when(node.getNode(eq(rbacl.getQualified())))
                .thenReturn(aclNode);

        provider.postRoles(node, data);

        verify(node).addMixin(eq(rbaclAssignable.getQualified()));

        // Check that it attempted to remove existing principals
        verify(principalNode1).remove();

        // Verify that no new nodes were added since data is empty
        verify(node, never()).addNode(anyString(), anyString());
    }

    @Test
    public void testDeleteRolesNonAssignable() throws RepositoryException {

        when(node.isNodeType(eq(JcrName.rbaclAssignable.getQualified())))
                .thenReturn(false);

        provider.deleteRoles(node);

        // No work occurs since there are no roles to delete
        verify(node, never()).removeMixin(
                eq(JcrName.rbaclAssignable.getQualified()));
        verify(node, never()).getNode(anyString());
    }

    @Test
    public void testDeleteRolesRBACLPathNotFound() throws RepositoryException {

        when(node.isNodeType(eq(JcrName.rbaclAssignable.getQualified())))
                .thenReturn(true);

        when(node.getNode(eq(JcrName.rbacl.getQualified()))).thenThrow(new PathNotFoundException());

        provider.deleteRoles(node);

        // Verify that mixin still gets removed
        verify(node).removeMixin(eq(JcrName.rbaclAssignable.getQualified()));
        // Verify that it attempted to get the rbacl node, and threw exception
        verify(node).getNode(eq(JcrName.rbacl.getQualified()));
    }

    @Test
    public void testDeleteRoles() throws RepositoryException {

        when(node.isNodeType(eq(JcrName.rbaclAssignable.getQualified())))
                .thenReturn(true);

        when(node.getNode(eq(JcrName.rbacl.getQualified()))).thenReturn(
                rbaclNode);

        provider.deleteRoles(node);

        // Check that mixin and rbacl node were removed
        verify(rbaclNode).remove();
        verify(node).removeMixin(eq(JcrName.rbaclAssignable.getQualified()));
    }

    @Test
    public void testFindRolesForPathRootNotAssignable()
            throws RepositoryException {

        final Path path = mock(Path.class);
        when(path.isRoot()).thenReturn(true);

        when(session.getRootNode()).thenReturn(node);

        // Not assignable and with no parents
        when(node.isNodeType(eq(JcrName.rbaclAssignable.getQualified())))
                .thenReturn(false);
        when(node.getParent()).thenReturn(null);

        final Map<String, List<String>> data =
                provider.findRolesForPath(path, session);

        assertNull("Unassignable root should return no role data", data);
        // Check that it retrieved root node and looked for parent
        verify(session).getRootNode();
        verify(node).getParent();
    }

    @Test
    public void testFindRolesForPathNotRoot()
            throws RepositoryException {

        final Path path = mock(Path.class);
        when(path.isRoot()).thenReturn(false);
        final String pathString = "path";
        when(path.getString()).thenReturn(pathString);

        when(session.getNode(eq(pathString))).thenReturn(node);

        // Not assignable, but with parent that is assignable
        when(node.isNodeType(eq(JcrName.rbaclAssignable.getQualified())))
                .thenReturn(false);

        final Node parentNode = mock(Node.class);
        when(parentNode.isNodeType(anyString())).thenReturn(true);
        when(parentNode.getNode(anyString())).thenReturn(rbaclNode);

        when(node.getParent()).thenReturn(parentNode);

        final Map<String, List<String>> data =
                provider.findRolesForPath(path, session);

        // Verify lookup of node by path
        verify(session).getNode(eq(pathString));

        assertEquals("One principal should be retrieved", 1, data.size());
    }

    @Test
    public void testFindRolesForPathFirstParentPathNotFound()
            throws RepositoryException {

        // Path itself is not found
        final Path path = mock(Path.class);
        when(path.isRoot()).thenReturn(false);
        final String pathString = "path";
        when(path.getString()).thenReturn(pathString);

        when(session.getNode(eq(pathString))).thenThrow(new PathNotFoundException());

        // Paths parent, the root, is found and is assignable
        final Path rootPath = mock(Path.class);
        when(rootPath.isRoot()).thenReturn(true);
        when(path.getParent()).thenReturn(rootPath);

        when(session.getRootNode()).thenReturn(node);

        when(node.isNodeType(eq(JcrName.rbaclAssignable.getQualified())))
                .thenReturn(true);

        when(node.getNode(anyString())).thenReturn(rbaclNode);

        final Map<String, List<String>> data =
                provider.findRolesForPath(path, session);

        // Verify lookup of node by path
        verify(session).getRootNode();
        verify(session).getNode(eq(pathString));

        assertEquals("One principal should be retrieved", 1, data.size());
    }

    @Test(expected = NullPointerException.class)
    public void testFindRolesForPathNullPath() throws RepositoryException {

        try {
            // This will throw an NPE because getRoles doesn't expect a null
            // node, but findRolesForPath can pass it one
            provider.findRolesForPath(null, session);
        } finally {
            verify(session, never()).getRootNode();
            verify(node, never()).getParent();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testFindRolesForPathAllNotFound() throws RepositoryException {

        when(session.getRootNode()).thenThrow(new PathNotFoundException());
        when(session.getNode(anyString())).thenThrow(
                new PathNotFoundException());

        final Path path1 = mock(Path.class);
        final Path path2 = mock(Path.class);

        final Path rootPath = mock(Path.class);
        when(rootPath.isRoot()).thenReturn(true);
        when(path1.getParent()).thenReturn(rootPath);
        when(path2.getParent()).thenReturn(path1);

        try {
            provider.findRolesForPath(path2, session);
        } finally {
            // It should have checked each node before NPE
            verify(session).getRootNode();
            verify(session, times(2)).getNode(anyString());
        }
    }
}
