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

import java.util.Map;

import javax.jcr.Credentials;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.modeshape.jcr.security.SecurityContext;

/**
 * This authentication provider will always authenticate, giving
 * complete access privileges to the session.
 *
 * @author Gregory Jansen
 */
public class BypassSecurityServletAuthenticationProvider implements
        AuthenticationProvider {

    /*
     * (non-Javadoc)
     * @see
     * org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.
     * jcr.Credentials, java.lang.String, java.lang.String,
     * org.modeshape.jcr.ExecutionContext, java.util.Map)
     */
    @Override
    public ExecutionContext authenticate(final Credentials credentials,
            final String repositoryName, final String workspaceName,
            final ExecutionContext repositoryContext,
            final Map<String, Object> sessionAttributes) {
        if (credentials instanceof ServletCredentials) {
            return repositoryContext
                    .with(new AnonymousAdminSecurityContext("bypassAdmin"));
        }
        return null;

    }

    /**
     * Security context with complete
     */
    public static class AnonymousAdminSecurityContext implements
            SecurityContext {

        private String userName;

        /**
         * Create a new security context with the given user name
         * @param userName User name to assign to the anonymous admin
         */
        public AnonymousAdminSecurityContext(final String userName) {
            this.userName = userName;
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
            return userName;
        }

        /*
         * (non-Javadoc)
         * @see
         * org.modeshape.jcr.security.SecurityContext#hasRole(java.lang.String)
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
            /*NOOP*/
        }
    }
}
