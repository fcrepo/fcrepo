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
package org.fcrepo.auth.common;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthorizationProvider;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;

/**
 * This is a pass-through security context for authenticated Fedora
 * administrators.
 *
 * @author Gregory Jansen
 */
public class FedoraAdminSecurityContext implements AuthorizationProvider,
        SecurityContext {

    private String username = null;

    private boolean loggedIn = true;

    /**
     * @param username the user name
     */
    public FedoraAdminSecurityContext(final String username) {
        super();
        this.username = username;
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#isAnonymous()
     */
    @Override
    public boolean isAnonymous() {
        return username != null;
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#getUserName()
     */
    @Override
    public String getUserName() {
        return username;
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#hasRole(java.lang.String)
     */
    @Override
    public boolean hasRole(final String roleName) {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.modeshape.jcr.security.SecurityContext#logout()
     */
    @Override
    public void logout() {
        this.loggedIn = false;
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
        return this.loggedIn;
    }

}
