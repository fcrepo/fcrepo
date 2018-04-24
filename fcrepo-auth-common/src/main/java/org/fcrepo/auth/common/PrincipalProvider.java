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

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Set;

/**
 * This interface provides a way for authentication code to communicate generic
 * credentials to authorization delegates. An implementation of this interface
 * could perform a query to determine group membership, for example.
 * <p>
 * The ServletContainerAuthenticationProvider's principalProviders set may be
 * configured with zero or more instances of implementations of this interface,
 * which it will consult during authentication. The union of the results will be
 * assigned to the FEDORA_ALL_PRINCIPALS session attribute.
 * </p>
 *
 * @author Gregory Jansen
 * @see HttpHeaderPrincipalProvider
 */
public interface PrincipalProvider extends Filter {

    /**
     * Extract principals from the provided HttpServletRequest.
     * <p>
     * If no principals can be extracted, implementations of this method
     * should return the empty set rather than null.
     * </p>
     *
     * @param request the request
     * @return a set of security principals
     */
    Set<Principal> getPrincipals(HttpServletRequest request);

}
