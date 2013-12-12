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

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * An example principal factory that extracts groups principals from request
 * headers.
 * 
 * @author Gregory Jansen
 */
public class HTTPHeaderPrincipalFactory implements HTTPPrincipalFactory {

    private Map<String, Map<String, String>> principalConfigs;

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.GroupPrincipalFactory#getGroupPrincipals(javax.servlet
     * .http.HttpServletRequest)
     */
    @Override
    public Set<Principal>
    getGroupPrincipals(final HttpServletRequest request) {
        return Collections.EMPTY_SET;
    }

}
