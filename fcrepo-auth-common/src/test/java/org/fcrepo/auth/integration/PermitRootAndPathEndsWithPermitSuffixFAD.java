/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.auth.integration;

import java.security.Principal;

import javax.jcr.Session;

import org.fcrepo.auth.common.FedoraAuthorizationDelegate;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 */
public class PermitRootAndPathEndsWithPermitSuffixFAD implements
        FedoraAuthorizationDelegate {

    Logger logger = LoggerFactory
            .getLogger(PermitRootAndPathEndsWithPermitSuffixFAD.class);

    /**
     * The security principal for every request.
     */
    private static final Principal EVERYONE = new Principal() {

        @Override
        public String getName() {
            return "EVERYONE";
        }

        @Override
        public String toString() {
            return getName();
        }

    };

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.FedoraPolicyEnforcementPoint#hasModeShapePermission(org
     * .modeshape.jcr.value.Path, java.lang.String[], java.util.Set,
     * java.security.Principal)
     */
    @Override
    public boolean hasPermission(final Session session, final Path absPath, final String[] actions) {
        // allow operations at the root, for test convenience
        if (absPath.isRoot()) {
            return true;
        }

        // allow anywhere the path ends with "permit"
        if (absPath.getLastSegment().getName().getLocalName()
                .toLowerCase().endsWith("permit")) {
            return true;
        }

        // allow anywhere the last path segment is "jcr:content"
        if (absPath.getLastSegment().getName().getLocalName().toLowerCase()
                .equals("content")) {
            return true;
        }

        // allow properties to be set under parent nodes that end with "permit"
        if (actions.length == 1 && "set_property".equals(actions[0])) {
            return absPath.getParent().getLastSegment().getName()
                    .getLocalName().toLowerCase().endsWith("permit");
        }

        // due to the fact that versioning creates version nodes under the
        // created node, for the test implementation we should allow actions
        // on nodes whose parents end with "permit".
        return (!absPath.getParent().isRoot() && absPath.getParent()
                .getLastSegment().getName().getLocalName().toLowerCase()
                .endsWith("permit"));

    }

    @Override
    public Principal getEveryonePrincipal() {
        return EVERYONE;
    }

}
