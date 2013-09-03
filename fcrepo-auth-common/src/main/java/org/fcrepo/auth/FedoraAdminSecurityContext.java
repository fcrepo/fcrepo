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

import javax.servlet.http.HttpServletRequest;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthorizationProvider;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;

/**
 * @author Gregory Jansen
 */
public class FedoraAdminSecurityContext implements AuthorizationProvider,
        SecurityContext {

    HttpServletRequest request = null;

    /**
     * @param request
     */
    public FedoraAdminSecurityContext(final HttpServletRequest request) {
        super();
        this.request = request;
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#isAnonymous()
     */
    @Override
    public boolean isAnonymous() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#getUserName()
     */
    @Override
    public String getUserName() {
        return request.getRemoteUser();
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#hasRole(java.lang.String)
     */
    @Override
    public boolean hasRole(final String roleName) {
        return request.isUserInRole(roleName);
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#logout()
     */
    @Override
    public void logout() {
        // more to do?
        request = null;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.modeshape.jcr.security.AuthorizationProvider#hasPermission(
     * org.modeshape.jcr.ExecutionContext, java.lang.String,
     * java.lang.String,
     * java.lang.String, org.modeshape.jcr.value.Path, java.lang.String[])
     */
    @Override
    public boolean hasPermission(final ExecutionContext context,
            final String repositoryName,
            final String repositorySourceName, final String workspaceName,
            final Path absPath, final String... actions) {
        return request != null;
    }

}
