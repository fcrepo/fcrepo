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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * These factories extract Group Principals from HTTP requests, usually from
 * request headers. Principals that you wish to assign roles to must implement
 * Serializable.
 * 
 * @author Gregory Jansen
 */
public interface HTTPPrincipalFactory {

    /**
     * Extract extra security principals from an HTTP request.
     * @param request the request
     * @return a set of security principals
     */
    public Set<Principal>
    getGroupPrincipals(HttpServletRequest request);
}
