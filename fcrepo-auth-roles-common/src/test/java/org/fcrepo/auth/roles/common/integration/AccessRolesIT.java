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
package org.fcrepo.auth.roles.common.integration;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.junit.Test;

/**
 * @author Gregory Jansen
 * @author Scott Prater
 */
public class AccessRolesIT extends AbstractCommonRolesIT {

    @Override
    protected List<RolesFadTestObjectBean> getTestObjs() {
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
    public void testCRUDRoles() throws ClientProtocolException, IOException {

        // add the roles
        assertEquals(CREATED.getStatusCode(), postRoles("testcommonobj1",
                test_json_roles));

        // Get the roles
        assertEquals("Cannot get role list from object with roles!", OK
                .getStatusCode(),
                canGetRoles(null, "testcommonobj1", false));

        assertEquals("Result does not equal test data!", t_roles,
                getRoles("testcommonobj1"));

        // delete the roles
        assertEquals("Cannot delete role list from object with roles!",
                NO_CONTENT
                .getStatusCode(), canDeleteRoles(null, "testcommonobj1", false));
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
        final String path = "testrole";
        assertEquals("System role object not created", CREATED.getStatusCode(), postRoles(path, test_json_roles));

        assertEquals("Result does not equal test data!", t_roles,
                getEffectiveRoles("/testrole"));
    }

    /**
     * Test method for testing referenced roles
     *
     * @throws IOException
     * @throws ClientProtocolException
     */
    @Test
    public void testReferencedRoles() throws ClientProtocolException, IOException, Exception {
        final String path = "testrole1";

        //post roles
        assertEquals("System role object not created", CREATED.getStatusCode(), postRoles(path, test_json_roles));

        //get roles
        assertEquals("Result does not equal test data!", t_roles,
                getRoles(path));

        //delete roles -deprecated

        final String uriID = "http://localhost:" + SERVER_PORT + "/rest/fedora:system/fedora:accessroles/testrole1";

        final String objPath = "testObjWithReference";
        ingestObjectWithReference(objPath,uriID);

        //access OK
        assertEquals("Can't read when should have access",OK.getStatusCode(),canRead("examplereader",objPath,true));

        //can't delete due to referential integrity
        final String roleObjectPath = "/fedora:system/fedora:accessroles/testrole1";
        assertEquals("Object deleted when should be protected by RI",PRECONDITION_FAILED.getStatusCode(),deleteTestObjectByPath(roleObjectPath));
    }
}
