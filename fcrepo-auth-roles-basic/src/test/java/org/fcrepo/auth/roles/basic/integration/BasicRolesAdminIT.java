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
package org.fcrepo.auth.roles.basic.integration;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import org.apache.http.client.ClientProtocolException;
import org.fcrepo.auth.roles.common.integration.RolesFadTestObjectBean;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Verifies that role for admins is properly enforced.
 *
 * @author Scott Prater
 * @author Gregory Jansen
 */
public class BasicRolesAdminIT extends AbstractBasicRolesIT {

    private final static String TESTDS = "admintestds";

    @Override
    protected List<RolesFadTestObjectBean> getTestObjs() {
        return test_objs;
    }

    /* Public object, one open datastream */
    @Test
    public void testAdminCanReadOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals("Admin cannot read testparent1!", OK.getStatusCode(),
                canRead(
                        "exampleadmin", "testparent1", true));
    }

    @Test
    public void testAdminCanWriteDatastreamOnOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot write datastream to testparent1!", CREATED
                .getStatusCode(), canAddDS("exampleadmin", "testparent1",
                        TESTDS, true));
    }

    @Test
    public void testAdminCanAddACLToOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals("Admin cannot add an ACL to testparent1!", CREATED
                .getStatusCode(),
                canAddACL("exampleadmin", "testparent1",
                        "EVERYONE", "admin", true));
    }

    /* Public object, one open datastream, one restricted datastream */
    /* object */
    @Test
    public void
    testAdminCanReadOpenObjWithRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals("Admin cannot read testparent2!", OK.getStatusCode(),
                canRead(
                        "exampleadmin", "testparent2", true));
    }

    /* open datastream */
    @Test
    public void testAdminCanReadOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent2/tsp1_data!", OK
                .getStatusCode(), canRead("exampleadmin",
                        "testparent2/tsp1_data",
                        true));
    }

    @Test
    public void
    testAdminCanUpdateOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent2/tsp1_data!",
                NO_CONTENT
                .getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent2",
                        "tsp1_data", true));
    }

    @Test
    public void testAdminCanAddACLToOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent2/tsp1_data!",
                CREATED
                .getStatusCode(), canAddACL("exampleadmin",
                        "testparent2/tsp1_data", "EVERYONE", "admin", true));
    }

    /* restricted datastream */
    @Test
    public void testAdminCanReadOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read restricted datastream testparent2/tsp2_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent2/tsp2_data", true));
    }

    @Test
    public void testAdminCanUpdateOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update restricted datastream testparent2/tsp2_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent2",
                        "tsp2_data", true));
    }

    @Test
    public void testAdminCanAddACLToOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to restricted datastream testparent2/tsp2_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent2/tsp2_data", "EVERYONE", "admin", true));
    }

    /* Child object (inherits ACL), one open datastream */
    @Test
    public void testAdminCanReadInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read testparent1/testchild1NoACL!", OK
                .getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild1NoACL",
                        true));
    }

    @Test
    public void testAdminCanWriteDatastreamOnInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot write datastream to testparent1/testchild1NoACL!",
                CREATED
                .getStatusCode(), canAddDS("exampleadmin",
                        "testparent1/testchild1NoACL", TESTDS, true));
    }

    @Test
    public void testAdminCanAddACLToInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to testparent1/testchild1NoACL!",
                CREATED
                .getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild1NoACL", "EVERYONE", "admin",
                        true));
    }

    @Test
    public void testAdminCanReadInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent1/testchild1NoACL/tsc1_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild1NoACL/tsc1_data", true));
    }

    @Test
    public void testAdminCanUpdateInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent1/testchild1NoACL/tsc1_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent1/testchild1NoACL", "tsc1_data", true));
    }

    @Test
    public
    void testAdminCanAddACLToInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent1/testchild1NoACL/tsc1_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild1NoACL/tsc1_data", "EVERYONE",
                        "admin", true));
    }

    /* Restricted child object with own ACL, two restricted datastreams */
    @Test
    public void testAdminCanReadRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read testparent1/testchild2WithACL!", OK
                .getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild2WithACL", true));
    }

    @Test
    public void testAdminCanWriteDatastreamOnRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot write datastream to testparent1/testchild2WithACL!",
                CREATED.getStatusCode(), canAddDS("exampleadmin",
                        "testparent1/testchild2WithACL", TESTDS, true));
    }

    @Test
    public void testAdminCanAddACLToRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to testparent1/testchild2WithACL!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild2WithACL", "EVERYONE", "admin",
                        true));
    }

    @Test
    public void testAdminCanReadRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent1/testchild2WithACL/tsc1_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild2WithACL/tsc1_data", true));
    }

    @Test
    public void testAdminCanUpdateRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent1/testchild2WithACL/tsc1_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent1/testchild2WithACL", "tsc1_data", true));
    }

    @Test
    public void
    testAdminCanAddACLToRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent1/testchild2WithACL/tsc1_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild2WithACL/tsc1_data", "EVERYONE",
                        "admin", true));
    }

    /* Even more restricted datastream */
    @Test
    public void
    testAdminCanReadRestrictedChildObjReallyRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent1/testchild2WithACL/tsc2_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild2WithACL/tsc2_data", true));
    }

    @Test
    public
    void
    testAdminCanUpdateRestrictedChildObjReallyRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent1/testchild2WithACL/tsc2_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent1/testchild2WithACL", "tsc2_data", true));
    }

    @Test
    public
    void
    testAdminCanAddACLToRestrictedChildObjReallyRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent1/testchild2WithACL/tsc2_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild2WithACL/tsc2_data", "EVERYONE",
                        "admin", true));
    }

    /* Writer/Admin child object with own ACL, two restricted datastreams */
    @Test
    public void testAdminCanReadWriterRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals("Admin cannot read testparent1/testchild4WithACL!", OK
                .getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild4WithACL", true));
    }

    @Test
    public void testAdminCanWriteDatastreamOnWriterRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot write datastream to testparent1/testchild4WithACL!",
                CREATED.getStatusCode(), canAddDS("exampleadmin",
                        "testparent1/testchild4WithACL", TESTDS, true));
    }

    @Test
    public void testAdminCanAddACLToWriterRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to testparent1/testchild4WithACL!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild4WithACL", "EVERYONE", "admin",
                        true));
    }

    @Test
    public
    void
    testAdminCanReadWriterRestrictedChildObjWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent1/testchild4WithACL/tsc1_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild4WithACL/tsc1_data", true));
    }

    @Test
    public
    void
    testAdminCanUpdateWriterRestrictedChildObjWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent1/testchild4WithACL/tsc1_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent1/testchild4WithACL", "tsc1_data", true));
    }

    @Test
    public
    void
    testAdminCanAddACLToWriterRestrictedChildObjWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent1/testchild4WithACL/tsc1_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild4WithACL/tsc1_data", "EVERYONE",
                        "admin", true));
    }

    /* Even more restricted datastream */
    @Test
    public
    void
    testAdminCanReadWriterRestrictedChildObjReallyWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent1/testchild4WithACL/tsc2_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent1/testchild4WithACL/tsc2_data", true));
    }

    @Test
    public
    void
    testAdminCanUpdateWriterRestrictedChildObjReallyWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent1/testchild4WithACL/tsc2_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent1/testchild4WithACL", "tsc2_data", true));
    }

    @Test
    public
    void
    testAdminCanAddACLToWriterRestrictedChildObjReallyWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent1/testchild4WithACL/tsc2_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent1/testchild4WithACL/tsc2_data", "EVERYONE",
                        "admin", true));
    }

    /* Admin object with public datastream */
    @Test
    public void testAdminCanReadAdminObj() throws ClientProtocolException,
    IOException {
        assertEquals("Admin cannot read testparent2/testchild5WithACL!", OK
                .getStatusCode(), canRead("exampleadmin",
                        "testparent2/testchild5WithACL", true));
    }

    @Test
    public void testAdminCanWriteDatastreamOnAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot write datastream to testparent2/testchild5WithACL!",
                CREATED.getStatusCode(), canAddDS("exampleadmin",
                        "testparent2/testchild5WithACL", TESTDS, true));
    }

    @Test
    public void testAdminCanAddACLToAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to testparent2/testchild5WithACL!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent2/testchild5WithACL", "EVERYONE", "admin",
                        true));
    }

    @Test
    public void testAdminCanReadAdminObjAdminRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent2/testchild5WithACL/tsc1_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent2/testchild5WithACL/tsc1_data", true));
    }

    @Test
    public void testAdminCanUpdateAdminObjAdminRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot update datastream testparent2/testchild5WithACL/tsc1_data!",
                NO_CONTENT.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent2/testchild5WithACL", "tsc1_data", true));
    }

    @Test
    public void testAdminCanAddACLToAdminObjAdminRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot add an ACL to datastream testparent2/testchild5WithACL/tsc1_data!",
                CREATED.getStatusCode(), canAddACL("exampleadmin",
                        "testparent2/testchild5WithACL/tsc1_data", "EVERYONE",
                        "admin", true));
    }

    @Test
    public void testAdminCanReadAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin cannot read datastream testparent2/testchild5WithACL/tsc2_data!",
                OK.getStatusCode(), canRead("exampleadmin",
                        "testparent2/tsp1_data", true));
    }

    @Test
    public void testAdminCannotUpdateAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin should not be allowed to update datastream testparent2/testchild5WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("exampleadmin",
                        "testparent2/testchild5WithACL", "tsc2_data", true));
    }

    @Test
    public void testAdminCannotAddACLToAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin should not be allowed to add an ACL to datastream testparent2/testchild5WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canAddACL("exampleadmin",
                        "testparent2/testchild5WithACL/tsc2_data", "EVERYONE",
                        "admin", true));
    }

    /* Deletions */
    @Test
    public void testAdminCanDeleteOpenObjAndItsDescendants()
            throws ClientProtocolException, IOException {
        assertEquals("Admin cannot delete object testparent3!", NO_CONTENT
                .getStatusCode(),
                canDelete("exampleadmin", "testparent3", true));

        assertEquals(
                "Admin should not have permission to try to read deleted datastream testparent3/tsp1_data!",
                FORBIDDEN.getStatusCode(), canDelete("exampleadmin",
                        "testparent3/tsp1_data", true));

        assertEquals(
                "Admin should not have permission to try to read deleted datastream testparent3/tsp2_data!",
                FORBIDDEN.getStatusCode(), canDelete("exampleadmin",
                        "testparent3/tsp2_data", true));

        assertEquals(
                "Admin should not have permission to try to read deleted object testparent3/testchild3a!",
                FORBIDDEN.getStatusCode(), canDelete("exampleadmin",
                        "testparent3/testchild3a", true));

        assertEquals(
                "Admin should not have permission to try to read deleted object testparent3/testchild3b!",
                FORBIDDEN.getStatusCode(), canDelete("exampleadmin",
                        "testparent3/testchild3b", true));

        assertEquals(
                "Fedora Admin should not be able to read deleted datastream testparent3/tsp1_data!",
                NOT_FOUND.getStatusCode(), canDelete("fedoraAdmin",
                        "testparent3/tsp1_data", true));

        assertEquals(
                "Fedora Admin should not be able to read deleted datastream testparent3/tsp2_data!",
                NOT_FOUND.getStatusCode(), canDelete("fedoraAdmin",
                        "testparent3/tsp2_data", true));

        assertEquals(
                "Fedora Admin should not be able to read deleted object testparent3/testchild3a!",
                NOT_FOUND.getStatusCode(), canDelete("fedoraAdmin",
                        "testparent3/testchild3a", true));

        assertEquals(
                "Fedora Admin should not be able to read deleted object testparent3/testchild3b!",
                NOT_FOUND.getStatusCode(), canDelete("fedoraAdmin",
                        "testparent3/testchild3b", true));
    }

    /* root node */
    @Test
    public void testAdminCannotReadRootNode()
            throws ClientProtocolException, IOException {
        assertEquals("Admin should not be allowed to read root node!",
                FORBIDDEN
                .getStatusCode(),
                canRead("exampleadmin", "/", true));
    }

    @Test
    public void testAdminCannotWriteDatastreamOnRootNode()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Admin should not be allowed to write datastream to root node!",
                FORBIDDEN
                .getStatusCode(), canAddDS("exampleadmin", "/", TESTDS, true));
    }

    @Test
    public void testAdminCannotAddACLToRootNode()
            throws ClientProtocolException, IOException {
        assertEquals("Admin should not be allowed to add an ACL to root node!",
                FORBIDDEN
                .getStatusCode(), canAddACL("exampleadmin", "/", "EVERYONE",
                        "admin", true));
    }
}
