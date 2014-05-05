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

import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.EVERYONE_NAME;
import java.util.ArrayList;
import java.util.List;

import org.fcrepo.auth.roles.common.integration.AbstractRolesIT;
import org.fcrepo.auth.roles.common.integration.RolesFadTestObjectBean;

/**
 * @author Scott Prater
 */
public abstract class AbstractBasicRolesIT extends AbstractRolesIT {

    protected final static List<RolesFadTestObjectBean> test_objs =
            defineTestObjects();

    private static List<RolesFadTestObjectBean> defineTestObjects() {
        final List<RolesFadTestObjectBean> test_objs = new ArrayList<>();
        /* public object with public datastream */
        final RolesFadTestObjectBean objA = new RolesFadTestObjectBean();
        objA.setPath("testparent1");
        objA.addACL(EVERYONE_NAME, "reader");
        objA.addACL("examplereader", "reader");
        objA.addACL("examplewriter", "writer");
        objA.addACL("exampleadmin", "admin");
        objA.addDatastream("tsp1_data", "Test Parent 1, datastream 1,  Hello!");
        test_objs.add(objA);

        /* public object with one public datastream, one restricted datastream */
        final RolesFadTestObjectBean objB = new RolesFadTestObjectBean();
        objB.setPath("testparent2");
        objB.addACL(EVERYONE_NAME, "reader");
        objB.addACL("examplereader", "reader");
        objB.addACL("examplewriter", "writer");
        objB.addACL("exampleadmin", "admin");
        objB.addDatastream("tsp1_data", "Test Parent 2, datastream 1,  Hello!");
        objB.addDatastream("tsp2_data",
                "Test Parent 2, datastream 2,  secret stuff");
        objB.addDatastreamACL("tsp2_data", "examplereader", "reader");
        objB.addDatastreamACL("tsp2_data", "examplewriter", "writer");
        objB.addDatastreamACL("tsp2_data", "exampleadmin", "admin");
        test_objs.add(objB);

        /* public child object with datastream, no ACLs */
        final RolesFadTestObjectBean objC = new RolesFadTestObjectBean();
        objC.setPath("testparent1/testchild1NoACL");
        objC.addDatastream("tsc1_data", "Test Child 1, datastream 1,  Hello!");
        test_objs.add(objC);

        /* restricted child object with restricted datastreams */
        final RolesFadTestObjectBean objD = new RolesFadTestObjectBean();
        objD.setPath("testparent1/testchild2WithACL");
        objD.addACL("examplereader", "reader");
        objD.addACL("examplewriter", "writer");
        objD.addACL("exampleadmin", "admin");
        objD.addDatastream("tsc1_data",
                "Test Child 2, datastream 1,  really secret stuff");
        objD.addDatastream("tsc2_data",
                "Test Child 2, datastream 2,  really really secret stuff");
        objD.addDatastreamACL("tsc2_data", "examplewriter", "writer");
        objD.addDatastreamACL("tsc2_data", "exampleadmin", "admin");
        test_objs.add(objD);

        /*
         * even more restricted child object, with even more restricted
         * datastreams
         */
        final RolesFadTestObjectBean objE = new RolesFadTestObjectBean();
        objE.setPath("testparent1/testchild4WithACL");
        objE.addACL("examplewriter", "writer");
        objE.addACL("exampleadmin", "admin");
        objE.addDatastream("tsc1_data",
                "Test Child 3, datastream 1,  really secret stuff");
        objE.addDatastream("tsc2_data",
                "Test Child 3, datastream 2,  really really secret stuff");
        objE.addDatastreamACL("tsc2_data", "exampleadmin", "admin");
        test_objs.add(objE);

        /* private child object with 1 private datastream, 1 public datastream */
        final RolesFadTestObjectBean objF = new RolesFadTestObjectBean();
        objF.setPath("testparent2/testchild5WithACL");
        objF.addACL("exampleadmin", "admin");
        objF.addDatastream("tsc1_data",
                "Test Child 5, datastream 1, burn before reading");
        objF.addDatastream("tsc2_data", "Test Child 5, datastream 2, Hello!");
        objF.addDatastreamACL("tsc2_data", EVERYONE_NAME, "reader");
        test_objs.add(objF);

        /* Public object, restricted datastream */
        final RolesFadTestObjectBean objG = new RolesFadTestObjectBean();
        objG.setPath("testparent3");
        objG.addACL(EVERYONE_NAME, "reader");
        objG.addACL("examplereader", "reader");
        objG.addACL("examplewriter", "writer");
        objG.addACL("exampleadmin", "admin");
        objG.addDatastream("tsp1_data", "Test Parent 3, datastream 1, hello!");
        objG.addDatastream("tsp2_data",
                "Test Parent 3, datastream 2, private stuff");
        objG.addDatastreamACL("tsp2_data", "exampleadmin", "admin");
        test_objs.add(objG);

        final RolesFadTestObjectBean objH = new RolesFadTestObjectBean();
        objH.setPath("testparent3/testchild3a");
        objH.addACL("exampleadmin", "admin");
        test_objs.add(objH);

        final RolesFadTestObjectBean objI = new RolesFadTestObjectBean();
        objI.setPath("testparent3/testchild3b");
        test_objs.add(objI);

        final RolesFadTestObjectBean objJ = new RolesFadTestObjectBean();
        objJ.setPath("testparent4");
        objJ.addACL("exampleWriterReader", "reader");
        objJ.addACL("exampleWriterReader", "writer");
        test_objs.add(objJ);

        return test_objs;

    }
}
