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

import org.apache.shiro.SecurityUtils;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthenticationProvider;

/**
 * Modeshape authentication provider that gets its security context from the current Shiro Subject.
 *
 * @author peichman
 */
public class ShiroAuthenticationProvider implements AuthenticationProvider {

    @Override
    public ExecutionContext authenticate(final Credentials credentials, final String repositoryName,
            final String workspaceName, final ExecutionContext repositoryContext,
            final Map<String, Object> sessionAttributes) {

        if (credentials == null) {
            return null;
        }

        return repositoryContext.with(new ShiroSecurityContext(SecurityUtils.getSubject()));
    }

}
