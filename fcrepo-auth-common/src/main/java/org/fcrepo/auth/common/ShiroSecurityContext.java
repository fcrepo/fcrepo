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

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * Security context that is simply a thin wrapper around a Shiro Subject.
 * 
 * @author peichman
 */
public class ShiroSecurityContext  {

    private Subject user;

    private String userName;

    /**
     * Create a new security context using the given Shiro subject. That subject will typically be the value returned
     * by a call to {@code SecurityUtils.getSubject()}.
     *
     * @param user subject to create the security context for
     */
    public ShiroSecurityContext(final Subject user) {
        if (user != null) {
            this.user = user;
            final PrincipalCollection principals = user.getPrincipals();
            if (principals != null) {
                final BasicUserPrincipal userPrincipal = principals.oneByType(BasicUserPrincipal.class);
                if (userPrincipal != null) {
                    this.userName = userPrincipal.getName();
                } else {
                    this.userName = null;
                }
            }
        }
    }
}