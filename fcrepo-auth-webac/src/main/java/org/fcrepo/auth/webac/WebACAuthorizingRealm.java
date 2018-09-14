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
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AUTHENTICATED_AGENT_VALUE;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.jena.rdf.model.Resource;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.fcrepo.http.api.FedoraLdp;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.slf4j.Logger;
/**
 * Authorization-only realm that performs authorization checks using WebAC ACLs stored in a Fedora repository. It
 * locates the ACL for the currently requested resource and parses the ACL RDF into a set of {@link WebACPermission}
 * instances.
 *
 * @author peichman
 */
public class WebACAuthorizingRealm extends AuthorizingRealm {

    private static final Logger log = getLogger(WebACAuthorizingRealm.class);

    private static final ContainerRolesPrincipal adminPrincipal = new ContainerRolesPrincipal(FEDORA_ADMIN_ROLE);

    private static final ContainerRolesPrincipal userPrincipal = new ContainerRolesPrincipal(FEDORA_USER_ROLE);

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private HttpServletRequest request;

    @Inject
    private WebACRolesProvider rolesProvider;

    private HttpSession session;

    private HttpSession session() {
        if (session == null) {
            session = sessionFactory.getSession(request);
        }
        return session;
    }

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    private IdentifierConverter<Resource, FedoraResource> translator() {
        if (idTranslator == null) {
            idTranslator = new HttpResourceConverter(session(), UriBuilder.fromResource(FedoraLdp.class));
        }

        return idTranslator;
    }

    /**
     * Useful for constructing URLs
     */
    @Context
    private UriInfo uriInfo;


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        final SimpleAuthorizationInfo authzInfo = new SimpleAuthorizationInfo();

        // if the user was assigned the "fedoraAdmin" container role, they get the
        // "fedoraAdmin" application role
        if (principals.byType(ContainerRolesPrincipal.class).contains(adminPrincipal)) {
            authzInfo.addRole(FEDORA_ADMIN_ROLE);
        } else {
            // otherwise, they are a normal user
            authzInfo.addRole(FEDORA_USER_ROLE);

            // for non-admins, we must check the ACL for the requested resource
            // convert the request URI to a JCR node
            final FedoraResource fedoraResource = getResourceOrParentFromPath(request.getPathInfo());

            if (fedoraResource != null) {
                final Node node = ((FedoraResourceImpl) fedoraResource).getNode();

                // check ACL for the request URI and get a mapping of agent => modes
                final Map<String, Collection<String>> roles = rolesProvider.getRoles(node, true);

                for (Object o : principals.asList()) {
                    log.debug("User has principal with name: {}", ((Principal) o).getName());
                }
                final Principal userPrincipal = principals.oneByType(BasicUserPrincipal.class);
                if (userPrincipal != null) {
                    log.debug("Basic user principal username: {}", userPrincipal.getName());
                    final Collection<String> modesForUser = getModesForUser(roles, userPrincipal);
                    if (modesForUser != null) {
                        // add WebACPermission instance for each mode in the Authorization
                        final URI fullRequestURI = URI.create(request.getRequestURL().toString());
                        for (String mode : modesForUser) {
                            final WebACPermission perm = new WebACPermission(URI.create(mode), fullRequestURI);
                            authzInfo.addObjectPermission(perm);
                            log.debug("Added permission {}", perm);
                        }
                    }
                } else {
                    log.debug("No basic user principal found");
                }
            }
        }

        return authzInfo;
    }

    private Collection<String> getModesForUser(final Map<String, Collection<String>> roles,
                                               final Principal userPrincipal) {
        final Set<String> modes = new HashSet<>();
        final Collection<String> userModes = roles.get(userPrincipal.getName());
        if (userModes != null) {
            modes.addAll(userModes);
        }

        final Collection<String> foafAgentModes = roles.get(FOAF_AGENT_VALUE);
        if (foafAgentModes != null) {
            modes.addAll(foafAgentModes);
        }

        if (userPrincipal != null) {
            final Collection<String> authenticatedAgentRoles = roles.get(WEBAC_AUTHENTICATED_AGENT_VALUE);
            if (authenticatedAgentRoles != null) {
                modes.addAll(authenticatedAgentRoles);
            }
        }

        return modes;
    }

    /**
     * This realm is authorization-only.
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
            throws AuthenticationException {
        return null;
    }

    /**
     * This realm is authorization-only.
     */
    @Override
    public boolean supports(final AuthenticationToken token) {
        return false;
    }

    private FedoraResource getResourceOrParentFromPath(final String path) {
        FedoraResource resource = null;
        log.debug("Attempting to get FedoraResource for {}", path);
        try {
            resource = translator().convert(translator().toDomain(path));
            log.debug("Got FedoraResource for {}", path);
        } catch (RepositoryRuntimeException e) {
            if (e.getCause() instanceof PathNotFoundException) {
                log.debug("Path {} does not exist", path);
                // go up the path looking for a node that exists
                if (path.length() > 1) {
                    final int lastSlash = path.lastIndexOf("/");
                    final int end = lastSlash > 0 ? lastSlash : lastSlash + 1;
                    resource = getResourceOrParentFromPath(path.substring(0, end));
                }
            }
        }
        return resource;
    }

}
