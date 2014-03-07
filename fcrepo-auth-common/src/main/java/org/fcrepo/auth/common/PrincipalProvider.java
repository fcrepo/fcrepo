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

package org.fcrepo.auth.common;

import javax.jcr.Credentials;

import java.security.Principal;
import java.util.Set;

/**
 * Implementations of this interface extract principals from credentials, e.g.
 * from HTTP request headers. Principals that will be assigned roles or
 * privileges must have a unique name.
 *
 * @author Gregory Jansen
 */
public interface PrincipalProvider {

    /**
     * Extract principals from credentials. If no principals can be extracted,
     * for example because the credentials are of a different type than
     * expected, implementations of this method should return the empty set
     * rather than null.
     *
     * @param credentials the credentials
     * @return a set of security principals
     */
    Set<Principal> getPrincipals(Credentials credentials);

}
