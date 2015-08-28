/**
 * Copyright 2015 DuraSpace, Inc.
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

import javax.jcr.Session;

import org.modeshape.jcr.value.Path;

/**
 * An interface that can authorize access to specific resources within
 * repositories.
 * <p>
 * An implementation has the opportunity to inspect nodes and the session, which
 * may have additional information assigned as session attributes, such as the
 * associated servlet request. This interface defines the Fedora-specific
 * attributes which may be added.
 * </p>
 *
 * @author Gregory Jansen
 */
public interface FedoraAuthorizationDelegate {

    /**
     * The name of the session attribute containing the servlet request (an
     * instance of javax.servlet.http.HttpServletRequest).
     */
    public static final String FEDORA_SERVLET_REQUEST =
            "fedora-servlet-request";

    /**
     * The name of the session attribute containing an instance of Principal
     * representing the current authenticated user.
     */
    public static final String FEDORA_USER_PRINCIPAL = "fedora-user-principal";

    /**
     * The name of the session attribute containing a set of instances of
     * Principal, representing the current user's credentials, including the
     * value of the FEDORA_USER_PRINCIPAL session attribute.
     */
    public static final String FEDORA_ALL_PRINCIPALS = "fedora-all-principals";

    /**
     * Determine if the supplied session has permission at absPath for all of
     * the actions.
     * <p>
     * The authentication provider may have added session attributes, which can
     * be accessed in implementations by calling session#getAttribute. If an
     * attribute is not available in session attributes and would be required to
     * establish that the session has permission for any action given, an
     * implementation should usually return false.
     * </p>
     * <p>
     * Note that accessing nodes using the provided session will result in
     * additional calls to this method and thus an infinite loop. Instead,
     * obtain a new session instance if your implementation requires access to
     * nodes. See AbstractRolesAuthorizationDelegate for an example.
     * </p>
     *
     * @param session the session
     * @param absPath the abspath
     * @param actions the actions
     * @return true if the given session has permission at absPath for all of
     *         the given actions, or false otherwise
     */
    boolean hasPermission(Session session, Path absPath, String[] actions);

    /**
     * The principal that this delegate uses to represent the public "EVERYONE" user.
     *
     * @return principal
     */
    public Principal getEveryonePrincipal();

}
