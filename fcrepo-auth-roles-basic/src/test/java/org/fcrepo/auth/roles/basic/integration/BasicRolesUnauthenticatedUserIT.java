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

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import org.apache.http.client.ClientProtocolException;
import org.fcrepo.auth.roles.common.integration.RolesFadTestObjectBean;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Verifies that role for unauthenticated users is properly enforced.
 *
 * @author Scott Prater
 * @author Gregory Jansen
 */
public class BasicRolesUnauthenticatedUserIT extends AbstractBasicRolesIT {

    private final static String TESTDS = "uutestds";

    @Override
    protected List<RolesFadTestObjectBean> getTestObjs() {
        return test_objs;
    }

    /* Public object, one open datastream */
    @Test
    public void testUnauthenticatedReaderCanReadOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals("Unauthenticated user cannot read testparent1!", OK
                .getStatusCode(), canRead(null, "testparent1", false));
    }

    @Test
    public void testUnauthenticatedReaderCannotWriteDatastreamOnOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to write datastream to testparent1!",
                FORBIDDEN.getStatusCode(), canAddDS(null, "testparent1",
                        TESTDS, false));
    }

    @Test
    public void testUnauthenticatedReaderCannotAddACLToOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to testparent1!",
                FORBIDDEN.getStatusCode(), canAddACL(null, "testparent1",
                        "everyone", "admin", false));
    }

    /* Public object, one open datastream, one restricted datastream */
    /* object */
    @Test
    public void
    testUnauthenticatedReaderCanReadOpenObjWithRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals("Unauthenticated user cannot read testparent2!", OK
                .getStatusCode(), canRead(null, "testparent2", false));
    }

    /* open datastream */
    @Test
    public void testUnauthenticatedReaderCanReadOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user cannot read datastream testparent2/tsp1_data!",
                OK.getStatusCode(), canRead(null, "testparent2/tsp1_data",
                        false));
    }

    @Test
    public void
    testUnauthenticatedReaderCannotUpdateOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to update datastream testparent2/tsp1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS(null, "testparent2",
                        "tsp1_data", false));
    }

    @Test
    public void
    testUnauthenticatedReaderCannotAddACLToOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to datastream testparent2/tsp1_data!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent2/tsp1_data", "everyone", "admin", false));
    }

    /* restricted datastream */
    @Test
    public void
    testUnauthenticatedReaderCannotReadOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to read restricted datastream testparent2/tsp2_data!",
                FORBIDDEN.getStatusCode(), canRead(null,
                        "testparent2/tsp2_data", false));
    }

    @Test
    public void
    testUnauthenticatedReaderCannotUpdateOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to update restricted datastream testparent2/tsp2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS(null, "testparent2",
                        "tsp2_data", false));
    }

    @Test
    public void
    testUnauthenticatedReaderCannotAddACLToOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to restricted datastream testparent2/tsp2_data!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent2/tsp2_data", "everyone", "admin", false));
    }

    /* Child object (inherits ACL), one open datastream */
    @Test
    public void testUnauthenticatedReaderCanReadInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user cannot read testparent1/testchild1NoACL!",
                OK
                .getStatusCode(), canRead(null, "testparent1/testchild1NoACL",
                        false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotWriteDatastreamOnInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to write datastream to testparent1/testchild1NoACL!",
                FORBIDDEN.getStatusCode(), canAddDS(null,
                        "testparent1/testchild1NoACL", TESTDS, false));
    }

    @Test
    public void testUnauthenticatedReaderCannotAddACLToInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to testparent1/testchild1NoACL!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent1/testchild1NoACL", "everyone", "admin",
                        false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCanReadInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user cannot read datastream testparent1/testchild1NoACL/tsc1_data!",
                OK.getStatusCode(), canRead(null,
                        "testparent1/testchild1NoACL/tsc1_data", false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotUpdateInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to update datastream testparent1/testchild1NoACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS(null,
                        "testparent1/testchild1NoACL", "tsc1_data", false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotAddACLToInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to datastream testparent1/testchild1NoACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent1/testchild1NoACL/tsc1_data", "everyone",
                        "admin", false));
    }

    /* Restricted child object with own ACL, two restricted datastreams */
    @Test
    public void testUnauthenticatedReaderCannotReadRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to read testparent1/testchild2WithACL!",
                FORBIDDEN.getStatusCode(), canRead(null,
                        "testparent1/testchild2WithACL", false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotWriteDatastreamOnRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to write datastream to testparent1/testchild2WithACL!",
                FORBIDDEN.getStatusCode(), canAddDS(null,
                        "testparent1/testchild2WithACL", TESTDS, false));
    }

    @Test
    public void testUnauthenticatedReaderCannotAddACLToRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to testparent1/testchild2WithACL!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent1/testchild2WithACL", "everyone", "admin",
                        false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotReadRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to read datastream testparent1/testchild2WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canRead(null,
                        "testparent1/testchild2WithACL/tsc1_data", false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotUpdateRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to update datastream testparent1/testchild2WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS(null,
                        "testparent1/testchild2WithACL", "tsc1_data", false));
    }

    @Test
    public
    void
    testUnauthenticatedReaderCannotAddACLToRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to datastream testparent1/testchild2WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent1/testchild2WithACL/tsc1_data", "everyone",
                        "admin", false));
    }

    /* Admin object with public datastream */
    @Test
    public void testUnauthenticatedReaderCannotReadAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to read testparent2/testchild5WithACL!",
                FORBIDDEN.getStatusCode(), canRead(null,
                        "testparent2/testchild5WithACL", false));
    }

    @Test
    public void testUnauthenticatedReaderCannotWriteDatastreamOnAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to write datastream to testparent2/testchild5WithACL!",
                FORBIDDEN.getStatusCode(), canAddDS(null,
                        "testparent2/testchild5WithACL", TESTDS, false));
    }

    @Test
    public void testUnauthenticatedReaderCannotAddACLToAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to testparent2/testchild5WithACL!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent2/testchild5WithACL", "everyone", "admin",
                        false));
    }

    @Test
    public void testUnauthenticatedReaderCanReadAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user cannot read datastream testparent2/testchild5WithACL/tsc2_data!",
                OK.getStatusCode(), canRead(null, "testparent2/tsp1_data",
                        false));
    }

    @Test
    public void testUnauthenticatedReaderCannotUpdateAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to update datastream testparent2/testchild5WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS(null,
                        "testparent2/testchild5WithACL", "tsc2_data", false));
    }

    @Test
    public void
    testUnauthenticatedReaderCannotAddACLToAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to datastream testparent2/testchild5WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canAddACL(null,
                        "testparent2/testchild5WithACL/tsc2_data", "everyone",
                        "admin", false));
    }

    /* Deletions */
    @Test
    public void testUnauthenticatedReaderCannotDeleteOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to delete testparent3!",
                FORBIDDEN.getStatusCode(),
                canDelete(null, "testparent3", false));
    }

    @Test
    public void testUnauthenticatedReaderCannotDeleteOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to delete datastream testparent3/tsp1_data!",
                FORBIDDEN.getStatusCode(), canDelete(null,
                        "testparent3/tsp1_data", false));
    }

    @Test
    public void
    testUnauthenticatedReaderCannotDeleteOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to delete datastream testparent3/tsp2_data!",
                FORBIDDEN.getStatusCode(), canDelete(null,
                        "testparent3/tsp2_data", false));
    }

    @Test
    public void testUnauthenticatedReaderCannotDeleteRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to delete object testparent3/testchild3a!",
                FORBIDDEN.getStatusCode(), canDelete(null,
                        "testparent3/testchild3a", false));
    }

    @Test
    public void testUnauthenticatedReaderCannotDeleteInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to delete object testparent3/testchild3b!",
                FORBIDDEN.getStatusCode(), canDelete(null,
                        "testparent3/testchild3b", false));
    }

    /* root node */
    @Test
    public void testUnauthenticatedReaderCannotReadRootNode()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to read root node!",
                FORBIDDEN
                .getStatusCode(), canRead(null, "/", false));
    }

    @Test
    public void testUnauthenticatedReaderCannotWriteDatastreamOnRootNode()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to write datastream to root node!",
                FORBIDDEN.getStatusCode(), canAddDS(null, "/", TESTDS, false));
    }

    @Test
    public void testUnauthenticatedReaderCannotAddACLToRootNode()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Unauthenticated user should not be allowed to add an ACL to root node!",
                FORBIDDEN.getStatusCode(), canAddACL(null, "/", "everyone",
                        "admin", false));
    }
}
