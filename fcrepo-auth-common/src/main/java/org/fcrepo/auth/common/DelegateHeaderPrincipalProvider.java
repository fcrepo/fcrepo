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

import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Set;

import org.fcrepo.kernel.api.exception.RepositoryConfigurationException;

/**
 * An example principal provider that extracts principals from request headers.
 *
 * @author awoods
 * @since 2015-10-31
 */
public class DelegateHeaderPrincipalProvider extends HttpHeaderPrincipalProvider {

    private static final String SEP = "no-separator";
    protected static final String DELEGATE_HEADER = "On-Behalf-Of";

    /**
     * Default Constructor
     */
    public DelegateHeaderPrincipalProvider() {
        super();
        setHeaderName(DELEGATE_HEADER);
        setSeparator(SEP);
    }

    /**
     * @param request from which the principal header is extracted
     * @return null if no delegate found, and the delegate if one found
     * @throws RepositoryConfigurationException if more than one delegate found
     */
    public Principal getDelegate(final HttpServletRequest request) {
        final Set<Principal> principals = getPrincipals(request);
        // No delegate
        if (principals.size() == 0) {
            return null;
        }

        // One delegate
        if (principals.size() == 1) {
            return principals.iterator().next();
        }

        throw new RepositoryConfigurationException("Too many delegates! " + principals);
    }

}
