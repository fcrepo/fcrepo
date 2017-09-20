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
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE_VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.ModeShapePermissions.ADD_NODE;
import static org.modeshape.jcr.ModeShapePermissions.MODIFY_ACCESS_CONTROL;
import static org.modeshape.jcr.ModeShapePermissions.READ;
import static org.modeshape.jcr.ModeShapePermissions.READ_ACCESS_CONTROL;
import static org.modeshape.jcr.ModeShapePermissions.REGISTER_NAMESPACE;
import static org.modeshape.jcr.ModeShapePermissions.REMOVE;
import static org.modeshape.jcr.ModeShapePermissions.REMOVE_CHILD_NODES;
import static org.modeshape.jcr.ModeShapePermissions.SET_PROPERTY;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.api.Session;

/**
 * Unit test for the WebAC Authorization Delegate.
 *
 * @author Peter Eichman
 * @since Aug 24, 2015
 */
@RunWith(MockitoJUnitRunner.class)
public class WebACAuthorizationDelegateTest {

    private WebACAuthorizationDelegate webacAD;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        webacAD = new WebACAuthorizationDelegate();
    }

    @Test
    public void testCanRead1() {
        final String[] actions = {READ};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_READ_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanRead2() {
        final String[] actions = {READ};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_READ_VALUE);
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCannotRead1() {
        final String[] actions = {READ, SET_PROPERTY};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertFalse(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCannotRead2() {
        final String[] actions = {READ};
        final Set<String> roles = new HashSet<>();

        assertFalse(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanWrite1() {
        final String[] actions = {ADD_NODE};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanWrite3() {
        final String[] actions = {REMOVE};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanWrite4() {
        final String[] actions = {REMOVE_CHILD_NODES};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanWrite5() {
        final String[] actions = {SET_PROPERTY};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanWrite6() {
        final String[] actions = {SET_PROPERTY, ADD_NODE, REMOVE, REMOVE_CHILD_NODES};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCannotWrite1() {
        final String[] actions = {READ, SET_PROPERTY, ADD_NODE, REGISTER_NAMESPACE, REMOVE, REMOVE_CHILD_NODES};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertFalse(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCannotWrite2() {
        final String[] actions = {SET_PROPERTY};
        final Set<String> roles = new HashSet<>();

        assertFalse(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanReadAcl1() {
        final String[] actions = {READ_ACCESS_CONTROL};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_CONTROL_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCannotReadAcl1() {
        final String[] actions = {READ_ACCESS_CONTROL};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_READ_VALUE);

        assertFalse(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCanWriteAcl1() {
        final String[] actions = {MODIFY_ACCESS_CONTROL};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_CONTROL_VALUE);

        assertTrue(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }

    @Test
    public void testCannotWriteAcl1() {
        final String[] actions = {MODIFY_ACCESS_CONTROL};
        final Set<String> roles = new HashSet<>();
        roles.add(WEBAC_MODE_WRITE_VALUE);

        assertFalse(webacAD.rolesHavePermission(mockSession, "/fake/path", actions, roles));
    }
}
