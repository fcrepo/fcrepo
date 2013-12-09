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

package org.fcrepo.auth.roles.common.integration;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 * @author Scott Prater
 */
public class AccessRolesIT extends AbstractCommonRolesIT {

    private static final Logger logger = LoggerFactory
            .getLogger(AccessRolesIT.class);

    @Override
    protected List<RolesPepTestObjectBean> getTestObjs() {
        return test_objs;
    }

    /**
     * Test method for
     * {@link org.fcrepo.auth.roles.common.AccessRoles#get(java.util.List)}.
     *
     * @throws IOException
     * @throws ClientProtocolException
     */
    @Test
    public void testGetEmptyRoles() throws ClientProtocolException, IOException {
        assertEquals("Can get empty role list from object with no roles",
                NO_CONTENT.getStatusCode(), canGetRoles(null, "testcommonobj1",
                        false));
    }

    /**
     * Test method for
     * {@link org.fcrepo.auth.roles.common.AccessRoles#get(java.util.List)}.
     *
     * @throws IOException
     * @throws ClientProtocolException
     */
    @Test
    public void testCRUDRoles() throws ClientProtocolException, IOException {

        // add the roles
        assertEquals(CREATED.getStatusCode(), postRoles("testcommonobj1",
                test_json_roles));

        // Get the roles
        assertEquals("Can get role list from object with roles", OK
                .getStatusCode(),
                canGetRoles(null, "testcommonobj1", false));

        assertEquals("result must equal test data", t_roles,
                getRoles("testcommonobj1"));

        // delete the roles
        assertEquals("Can delete role list from object with roles", NO_CONTENT
                .getStatusCode(), canDeleteRoles(null, "testcommonobj1", false));

        // verify that roles are gone
        assertEquals("Can get empty role list from object with no roles",
                NO_CONTENT.getStatusCode(), canGetRoles(null, "testcommonobj1",
                        false));

    }

    /**
     * Test method for
     * {@link org.fcrepo.auth.roles.common.AccessRoles#get(java.util.List, java.util.List)}
     * .
     *
     * @throws IOException
     * @throws ClientProtocolException
     */
    @Test
    public void testGetEffectiveRoles() throws ClientProtocolException,
    IOException {
        // verify that default roles are returned
        // Get the roles
        assertEquals("Can get effective role list from object", OK
                .getStatusCode(), canGetEffectiveRoles(null, "testcommonobj1",
                        false));

        assertEquals("result must equal test data",
                AccessRolesProvider.DEFAULT_ACCESS_ROLES,
                getEffectiveRoles("testcommonobj1"));

        // post some roles on parent
        assertEquals(CREATED.getStatusCode(), postRoles("testcommonobj1",
                test_json_roles));

        // see that parent roles are effective for child
        assertEquals(
                "Can get effective role list from child object with inherited roles",
                OK
                .getStatusCode(), canGetEffectiveRoles(null,
                        "testcommonobj1/testchildobj1", false));

        assertEquals("result must equal test data", t_roles,
                getEffectiveRoles("testcommonobj1/testchildobj1"));


        // post different acl with fewer roles on the child
        assertEquals("Can post admin ACL on child object", CREATED
                .getStatusCode(), postRoles("testcommonobj1/testchildobj1",
                        admin_json_role));

        // see that only child roles are effective for child
        assertEquals(
                "Can get effective role list from child object with own role",
                OK.getStatusCode(), canGetEffectiveRoles(null,
                        "testcommonobj1/testchildobj1", false));

        assertEquals("result must equal test data", admin_role,
                getEffectiveRoles("testcommonobj1/testchildobj1"));

    }
}
