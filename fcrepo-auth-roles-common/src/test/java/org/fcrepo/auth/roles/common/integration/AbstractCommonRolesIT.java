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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * @author Scott Prater
 */
public abstract class AbstractCommonRolesIT extends AbstractRolesIT {

    private static Logger logger = getLogger(AbstractCommonRolesIT.class);

    protected final static List<RolesPepTestObjectBean> test_objs =
            defineTestObjects();

    protected static Map<String, List<String>> t_roles =
            new HashMap<String, List<String>>();

    protected static Map<String, List<String>> admin_role =
            new HashMap<String, List<String>>();

    static {
        t_roles.put("exampleadmin", Collections.singletonList("admin"));
        t_roles.put("examplereader", Collections.singletonList("reader"));
        t_roles.put("examplewriter", Collections.singletonList("writer"));
        admin_role.put("exampleadmin", Collections.singletonList("admin"));
    }

    protected String test_json_roles = makeJson(t_roles);

    protected String admin_json_role = makeJson(admin_role);

    private static List<RolesPepTestObjectBean> defineTestObjects() {
        final List<RolesPepTestObjectBean> test_objs =
                new ArrayList<RolesPepTestObjectBean>();
        final RolesPepTestObjectBean objA = new RolesPepTestObjectBean();
        /* parent object */
        objA.setPath("testcommonobj1");
        test_objs.add(objA);

        final RolesPepTestObjectBean objB = new RolesPepTestObjectBean();
        /* child object */
        objA.setPath("testcommonobj1/testchildobj1");
        test_objs.add(objB);

        return test_objs;

    }
}
