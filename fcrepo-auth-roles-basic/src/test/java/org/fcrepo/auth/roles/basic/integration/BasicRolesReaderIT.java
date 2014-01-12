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

package org.fcrepo.auth.roles.basic.integration;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.fcrepo.auth.roles.common.integration.RolesPepTestObjectBean;
import org.junit.Test;

/**
 * Verifies that role for readers is properly enforced.
 *
 * @author Scott Prater
 * @author Gregory Jansen
 */
public class BasicRolesReaderIT extends AbstractBasicRolesIT {

    private final static String TESTDS = "readertestds";

    @Override
    protected List<RolesPepTestObjectBean> getTestObjs() {
        return test_objs;
    }

    /* Public object, one open datastream */
    @Test
    public void testReaderCanReadOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals("Reader cannot read testparent1!", OK.getStatusCode(),
                canRead("examplereader", "testparent1", true));
    }

    @Test
    public void testReaderCannotWriteDatastreamOnOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to write datastream to testparent1!",
                FORBIDDEN
                .getStatusCode(), canAddDS("examplereader", "testparent1",
                        TESTDS, true));
    }

    @Test
    public void testReaderCannotAddACLToOpenObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to testparent1!",
                FORBIDDEN
                .getStatusCode(), canAddACL("examplereader", "testparent1",
                        "everyone", "admin", true));
    }

    /* Public object, one open datastream, one restricted datastream */
    /* object */
    @Test
    public void
    testReaderCanReadOpenObjWithRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals("Reader cannot read testparent2!", OK.getStatusCode(),
                canRead("examplereader", "testparent2", true));
    }

    /* open datastream */
    @Test
    public void testReaderCanReadOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read datastream testparent2/tsp1_data!", OK
                .getStatusCode(), canRead("examplereader",
                        "testparent2/tsp1_data",
                        true));
    }

    @Test
    public void
    testReaderCannotUpdateOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent2/tsp1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent2",
                        "tsp1_data", true));
    }

    @Test
    public void testReaderCannotAddACLToOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent2/tsp1_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent2/tsp1_data", "everyone", "admin", true));
    }

    /* restricted datastream */
    @Test
    public void testReaderCanReadOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read restricted datastream testparent2/tsp2_data!",
                OK.getStatusCode(), canRead("examplereader",
                        "testparent2/tsp2_data", true));
    }

    @Test
    public void testReaderCannotUpdateOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update restricted datastream testparent2/tsp2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent2",
                        "tsp2_data", true));
    }

    @Test
    public void testReaderCannotAddACLToOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to restricted datastream testparent2/tsp2_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent2/tsp2_data", "everyone", "admin", true));
    }

    /* Child object (inherits ACL), one open datastream */
    @Test
    public void testReaderCanReadInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read testparent1/testchild1NoACL!", OK
                .getStatusCode(), canRead("examplereader",
                        "testparent1/testchild1NoACL",
                        true));
    }

    @Test
    public void testReaderCannotWriteDatastreamOnInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to write datastream to testparent1/testchild1NoACL!",
                FORBIDDEN.getStatusCode(), canAddDS("examplereader",
                        "testparent1/testchild1NoACL", TESTDS, true));
    }

    @Test
    public void testReaderCannotAddACLToInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to testparent1/testchild1NoACL!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild1NoACL", "everyone", "admin",
                        true));
    }

    @Test
    public void testReaderCanReadInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read datastream testparent1/testchild1NoACL/tsc1_data!",
                OK.getStatusCode(), canRead("examplereader",
                        "testparent1/testchild1NoACL/tsc1_data", true));
    }

    @Test
    public void testReaderCannotUpdateInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent1/testchild1NoACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent1/testchild1NoACL", "tsc1_data", true));
    }

    @Test
    public
    void testReaderCannotAddACLToInheritedACLChildObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent1/testchild1NoACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild1NoACL/tsc1_data", "everyone",
                        "admin", true));
    }

    /* Restricted child object with own ACL, two restricted datastreams */
    @Test
    public void testReaderCanReadRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read testparent1/testchild2WithACL!", OK
                .getStatusCode(), canRead("examplereader",
                        "testparent1/testchild2WithACL", true));
    }

    @Test
    public void testReaderCannotWriteDatastreamOnRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to write a datastream to testparent1/testchild2WithACL!",
                FORBIDDEN.getStatusCode(), canAddDS("examplereader",
                        "testparent1/testchild2WithACL", TESTDS, true));
    }

    @Test
    public void testReaderCannotAddACLToRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to testparent1/testchild2WithACL!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild2WithACL", "everyone", "admin",
                        true));
    }

    @Test
    public void testReaderCanReadRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read datastream testparent1/testchild2WithACL/tsc1_data!",
                OK.getStatusCode(), canRead("examplereader",
                        "testparent1/testchild2WithACL/tsc1_data", true));
    }

    @Test
    public void testReaderCannotUpdateRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent1/testchild2WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent1/testchild2WithACL", "tsc1_data", true));
    }

    @Test
    public void testReaderCannotAddACLToRestrictedChildObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent1/testchild2WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild2WithACL/tsc1_data", "everyone",
                        "admin", true));
    }

    /* Even more restricted datastream */
    @Test
    public void testReaderCannotReadRestrictedChildObjReallyRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to read datastream testparent1/testchild2WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canRead("examplereader",
                        "testparent1/testchild2WithACL/tsc2_data", true));
    }

    @Test
    public void testReaderCannotUpdateRestrictedChildObjReallyRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent1/testchild2WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent1/testchild2WithACL", "tsc2_data", true));
    }

    @Test
    public void testReaderCannotAddACLToRestrictedChildObjReallyRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent1/testchild2WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild2WithACL/tsc2_data", "everyone",
                        "admin", true));
    }

    /* Writer/Admin child object with own ACL, two restricted datastreams */
    @Test
    public void testReaderCannotReadWriterRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to read testparent1/testchild4WithACL!",
                FORBIDDEN.getStatusCode(), canRead("examplereader",
                        "testparent1/testchild4WithACL", true));
    }

    @Test
    public void testReaderCannotWriteDatastreamOnWriterRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to write a datastream to testparent1/testchild4WithACL!",
                FORBIDDEN.getStatusCode(), canAddDS("examplereader",
                        "testparent1/testchild4WithACL", TESTDS, true));
    }

    @Test
    public void testReaderCannotAddACLToWriterRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to testparent1/testchild4WithACL!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild4WithACL", "everyone", "admin",
                        true));
    }

    @Test
    public void testReaderCannotReadWriterRestrictedChildObjWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to read datastream testparent1/testchild4WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canRead("examplereader",
                        "testparent1/testchild4WithACL/tsc1_data", true));
    }

    @Test
    public void testReaderCannotUpdateWriterRestrictedChildObjWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent1/testchild4WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent1/testchild4WithACL", "tsc1_data", true));
    }

    @Test
    public void testReaderCannotAddACLToWriterRestrictedChildObjWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent1/testchild4WithACL/tsc1_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild4WithACL/tsc1_data", "everyone",
                        "admin", true));
    }

    /* Even more restricted datastream */
    @Test
    public void testReaderCannotReadWriterRestrictedChildObjReallyWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to read datastream testparent1/testchild4WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canRead("examplereader",
                        "testparent1/testchild4WithACL/tsc2_data", true));
    }

    @Test
    public void testReaderCannotUpdateWriterRestrictedChildObjReallyWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent1/testchild4WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent1/testchild4WithACL", "tsc2_data", true));
    }

    @Test
    public void testReaderCannotAddACLToWriterRestrictedChildObjReallyWriterRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent1/testchild4WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent1/testchild4WithACL/tsc2_data", "everyone",
                        "admin", true));
    }

    /* Admin object with public datastream */
    @Test
    public void testReaderCannotReadAdminObj() throws ClientProtocolException,
    IOException {
        assertEquals(
                "Reader should not be allowed to read testparent2/testchild5WithACL!",
                FORBIDDEN.getStatusCode(), canRead("examplereader",
                        "testparent2/testchild5WithACL", true));
    }

    @Test
    public void testReaderCannotWriteDatastreamOnAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to write a datastream to testparent2/testchild5WithACL!",
                FORBIDDEN.getStatusCode(), canAddDS("examplereader",
                        "testparent2/testchild5WithACL", TESTDS, true));
    }

    @Test
    public void testReaderCannotAddACLToAdminObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to testparent2/testchild5WithACL!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent2/testchild5WithACL", "everyone", "admin",
                        true));
    }

    @Test
    public void testReaderCanReadAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader cannot read datastream testparent2/testchild5WithACL/tsc2_data!",
                OK.getStatusCode(), canRead("examplereader",
                        "testparent2/tsp1_data", true));
    }

    @Test
    public void testReaderCannotUpdateAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to update datastream testparent2/testchild5WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canUpdateDS("examplereader",
                        "testparent2/testchild5WithACL", "tsc2_data", true));
    }

    @Test
    public void testReaderCannotAddACLToAdminObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to datastream testparent2/testchild5WithACL/tsc2_data!",
                FORBIDDEN.getStatusCode(), canAddACL("examplereader",
                        "testparent2/testchild5WithACL/tsc2_data", "everyone",
                        "admin", true));
    }

    /* Deletions */
    @Test
    public void testReaderCannotDeleteOpenObj() throws ClientProtocolException,
    IOException {
        assertEquals(
                "Reader should not be allowed to delete object testparent3!",
                FORBIDDEN
                .getStatusCode(), canDelete("examplereader", "testparent3",
                        true));
    }

    @Test
    public void testReaderCannotDeleteOpenObjPublicDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to delete datastream testparent3/tsp1_data!",
                FORBIDDEN.getStatusCode(), canDelete("examplereader",
                        "testparent3/tsp1_data", true));
    }

    @Test
    public void testReaderCannotDeleteOpenObjRestrictedDatastream()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to delete datastream testparent3/tsp2_data!",
                FORBIDDEN.getStatusCode(), canDelete("examplereader",
                        "testparent3/tsp2_data", true));
    }

    @Test
    public void testReaderCannotDeleteRestrictedChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to delete object testparent3/testchild3a!",
                FORBIDDEN.getStatusCode(), canDelete("examplereader",
                        "testparent3/testchild3a", true));
    }

    @Test
    public void testReaderCannotDeleteInheritedACLChildObj()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to delete object testparent3/testchild3b!",
                FORBIDDEN.getStatusCode(), canDelete("examplereader",
                        "testparent3/testchild3b", true));
    }

    /* root node */
    @Test
    public void testReaderCannotReadRootNode()
            throws ClientProtocolException, IOException {
        assertEquals("Reader should not be allowed to read root node!",
                FORBIDDEN
                .getStatusCode(),
                canRead("examplereader", "/", true));
    }

    @Test
    public void testReaderCannotWriteDatastreamOnRootNode()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to write a datastream to root node!",
                FORBIDDEN
                .getStatusCode(), canAddDS("examplereader", "/", TESTDS, true));
    }

    @Test
    public void testReaderCannotAddACLToRootNode()
            throws ClientProtocolException, IOException {
        assertEquals(
                "Reader should not be allowed to add an ACL to root node!",
                FORBIDDEN
                .getStatusCode(), canAddACL("examplereader", "/", "everyone",
                        "admin", true));
    }
}
