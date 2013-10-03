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

package org.fcrepo.auth;

import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

import org.modeshape.jcr.value.Path;

/**
 * Policy Enforcement Points implement the various authorization decisions
 * needed by Fedora. Implementations will translate enforcement calls into
 * their given policy framework.
 *
 * @author Gregory Jansen
 */
public interface FedoraPolicyEnforcementPoint {

    /**
     * Is the action permitted to the user or other any other principal on the
     * given node path?
     *
     * @param context
     * @param absPath
     * @param actions
     * @param userPrincipal
     * @param allPrincipals
     * @return
     */
    public boolean hasModeShapePermission(Path absPath, String[] actions,
            Set<Principal> allPrincipals, Principal userPrincipal);

    /**
     * Filter the collection of JCR paths, selecting those the user has
     * permission to read.
     * 
     * @param paths an iterator of paths
     * @param allPrincipals all the authenticated principals
     * @param userPrincipal the user principal
     * @return an iterator of permitted paths
     */
    public Iterator<Path> filterPathsForReading(Iterator<Path> paths,
            Set<Principal> allPrincipals, Principal userPrincipal);

}
