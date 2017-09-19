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

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE_VALUE;
import static org.modeshape.jcr.ModeShapePermissions.ADD_NODE;
import static org.modeshape.jcr.ModeShapePermissions.MODIFY_ACCESS_CONTROL;
import static org.modeshape.jcr.ModeShapePermissions.READ;
import static org.modeshape.jcr.ModeShapePermissions.READ_ACCESS_CONTROL;
import static org.modeshape.jcr.ModeShapePermissions.REMOVE;
import static org.modeshape.jcr.ModeShapePermissions.REMOVE_CHILD_NODES;
import static org.modeshape.jcr.ModeShapePermissions.SET_PROPERTY;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authorization Delegate responsible for resolving Fedora's permissions using Web Access Control (WebAC) access
 * control lists.
 *
 * @author Peter Eichman
 * @since Aug 24, 2015
 */
public class WebACAuthorizationDelegate extends AbstractRolesAuthorizationDelegate {

    /**
     * Class-level logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebACAuthorizationDelegate.class);

    private static final Map<String, String> actionMap;

    static {
        final Map<String, String> map = new HashMap<>();
        // WEBAC_MODE_READ Permissions
        map.put(READ, WEBAC_MODE_READ_VALUE);
        // WEBAC_MODE_WRITE Permissions
        map.put(ADD_NODE, WEBAC_MODE_WRITE_VALUE);
        map.put(REMOVE, WEBAC_MODE_WRITE_VALUE);
        map.put(REMOVE_CHILD_NODES, WEBAC_MODE_WRITE_VALUE);
        map.put(SET_PROPERTY, WEBAC_MODE_WRITE_VALUE);
        // WEBAC_MODE_CONTROL Permissions
        map.put(MODIFY_ACCESS_CONTROL, WEBAC_MODE_CONTROL_VALUE);
        map.put(READ_ACCESS_CONTROL, WEBAC_MODE_CONTROL_VALUE);
        actionMap = unmodifiableMap(map);
    }

    /**
     * The security principal for every request, that represents the foaf:Agent agent class that is used to designate
     * "everyone".
     */
    private static final Principal EVERYONE = new Principal() {

        @Override
        public String getName() {
            return FOAF_AGENT_VALUE;
        }

        @Override
        public String toString() {
            return getName();
        }

    };

    @Override
    public boolean rolesHavePermission(final Session userSession, final String absPath,
            final String[] actions, final Set<String> roles) {

        /*
         * If any value in the actions Array is NOT also in the roles Set, the request should be denied.
         * Otherwise, e.g. all of the actions values are contained in the roles set, the request is approved.
         *
         * The logic here may not be immediately obvious. The process is thus:
         *   map: map the modeshape action to a webac action
         *   allMatch: verify that ALL actions MUST exist in the roles Set
         */
        final boolean permit = stream(actions).map(actionMap::get).allMatch(roles::contains);

        LOGGER.debug("Request for actions: {}, on path: {}, with roles: {}. Permission={}",
                actions, absPath, roles, permit);

        return permit;
    }

    @Override
    public Principal getEveryonePrincipal() {
        return EVERYONE;
    }

}
