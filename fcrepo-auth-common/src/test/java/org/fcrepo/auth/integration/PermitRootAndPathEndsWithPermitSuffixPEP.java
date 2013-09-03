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

package org.fcrepo.auth.integration;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import org.fcrepo.auth.FedoraPolicyEnforcementPoint;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 */
public class PermitRootAndPathEndsWithPermitSuffixPEP implements
        FedoraPolicyEnforcementPoint {

    Logger logger = LoggerFactory
            .getLogger(PermitRootAndPathEndsWithPermitSuffixPEP.class);

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.FedoraPolicyEnforcementPoint#hasModeShapePermission(org
     * .modeshape.jcr.value.Path, java.lang.String[], java.util.Set,
     * java.security.Principal)
     */
    @Override
    public boolean hasModeShapePermission(final Path absPath,
            final String[] actions, final Set<Principal> allPrincipals,
            final Principal userPrincipal) {
        // allow operations at the root, for test convenience
        if (absPath.isRoot()) {
            return true;
        }

        // allow anywhere the path ends with "permit"
        if (absPath.getLastSegment().getName().getLocalName()
                .toLowerCase().endsWith("permit")) {
            return true;
        }

        // allow properties to be set under parent nodes that end with "permit"
        if (actions.length == 1 && "set_property".equals(actions[0])) {
            return absPath.getParent().getLastSegment().getName()
                    .getLocalName().toLowerCase().endsWith("permit");
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.FedoraPolicyEnforcementPoint#filterPathsForReading(java
     * .util.Collection, java.util.Set, java.security.Principal)
     */
    @Override
    public Set<Path> filterPathsForReading(final Collection<Path> paths,
            final Set<Principal> allPrincipals,
            final Principal userPrincipal) {
        return null;
    }

}
