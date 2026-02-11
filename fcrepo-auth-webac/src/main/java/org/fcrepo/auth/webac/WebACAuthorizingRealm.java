/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.common.DelegateHeaderPrincipalProvider.DelegatedHeaderPrincipal;
import static org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AUTHENTICATED_AGENT_VALUE;
import static org.fcrepo.auth.webac.WebACFilter.getBaseUri;
import static org.fcrepo.auth.webac.WebACFilter.identifierConverter;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TX;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.subject.WebSubject;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.commons.session.TransactionProvider;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryConfigurationException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    public static final String URIS_TO_AUTHORIZE = "URIS_TO_AUTHORIZE";

    @Inject
    private FedoraPropsConfig fedoraPropsConfig;

    @Inject
    private WebACRolesProvider rolesProvider;

    @Inject
    private TransactionManager transactionManager;

    @Inject
    private ResourceFactory resourceFactory;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    private Transaction transaction() {
        ensureSpringDependencies();
        final HttpServletRequest request = currentRequest();
        if (request == null) {
            log.warn("Current request is null");
            return ReadOnlyTransaction.INSTANCE;
        }

        final String txId = request.getHeader(ATOMIC_ID_HEADER);
        if (txId == null) {
            return ReadOnlyTransaction.INSTANCE;
        }
        final var txProvider = new TransactionProvider(transactionManager, request,
                getBaseUri(request), fedoraPropsConfig.getJmsBaseUrl());
        return txProvider.provide();
    }

    /**
     * Helper method to retrieve Servlet Request rather than injecting it
     */
    private HttpServletRequest currentRequest() {
        final Subject subject = SecurityUtils.getSubject();
        if (subject instanceof WebSubject) {
            final ServletRequest req = ((WebSubject) subject).getServletRequest();
            if (req instanceof HttpServletRequest) {
                return (HttpServletRequest) req;
            }
        }
        return null;
    }

    /**
     * Ensures all Spring dependencies are available, even if this Realm was not constructed by Spring.
     */
    private void ensureSpringDependencies() {
        // If Shiro created this Realm (not Spring), @Inject/@Autowired fields may be null.
        if (resourceFactory != null && rolesProvider != null && transactionManager != null
                && fedoraPropsConfig != null && containmentIndex != null) {
            return;
        }

        final HttpServletRequest req = currentRequest();
        if (req == null) {
            return;
        }

        final WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(req.getServletContext());
        if (ctx == null) {
            return;
        }

        if (fedoraPropsConfig == null) {
            fedoraPropsConfig = ctx.getBean(FedoraPropsConfig.class);
        }
        if (rolesProvider == null) {
            rolesProvider = ctx.getBean(WebACRolesProvider.class);
        }
        if (transactionManager == null) {
            transactionManager = ctx.getBean(TransactionManager.class);
        }
        if (resourceFactory == null) {
            resourceFactory = ctx.getBean(ResourceFactory.class);
        }
        if (containmentIndex == null) {
            // match the existing qualifier name
            containmentIndex = (ContainmentIndex) ctx.getBean("containmentIndex");
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        ensureSpringDependencies();
        final SimpleAuthorizationInfo authzInfo = new SimpleAuthorizationInfo();
        final HttpServletRequest request = currentRequest();
        if (request == null) {
            // no servlet request -> no URIS_TO_AUTHORIZE attribute -> return empty authz (or deny)
            return authzInfo;
        }
        boolean isAdmin = false;

        final Collection<DelegatedHeaderPrincipal> delegatePrincipals =
                principals.byType(DelegatedHeaderPrincipal.class);

        // if the user was assigned the "fedoraAdmin" container role, they get the
        // "fedoraAdmin" application role
        if (principals.byType(ContainerRolesPrincipal.class).contains(adminPrincipal)) {
            if (delegatePrincipals.size() > 1) {
                throw new RepositoryConfigurationException("Too many delegates! " + delegatePrincipals);
            } else if (delegatePrincipals.isEmpty()) {
                authzInfo.addRole(FEDORA_ADMIN_ROLE);
                return authzInfo;
            }
            isAdmin = true;
            // if Admin is delegating, they are a normal user
            authzInfo.addRole(FEDORA_USER_ROLE);
        } else if (principals.byType(ContainerRolesPrincipal.class).contains(userPrincipal)) {
            authzInfo.addRole(FEDORA_USER_ROLE);
        }

        // for non-admins, we must check the ACL for the requested resource
        @SuppressWarnings("unchecked")
        Set<URI> targetURIs = (Set<URI>) request.getAttribute(URIS_TO_AUTHORIZE);
        if (targetURIs == null) {
            targetURIs = new HashSet<>();
        }
        final Map<URI, Map<String, Collection<String>>> rolesForURI = new HashMap<>();
        final String contextPath = request.getContextPath() + request.getServletPath();
        for (final URI uri : targetURIs) {
            if (identifierConverter(request).inInternalDomain(uri.toString())) {
                final FedoraId id = FedoraId.create(uri.toString());
                log.debug("Getting roles for id {}", id.getFullId());
                rolesForURI.put(uri, getRolesForId(id));
            } else {
                String path = uri.getPath();
                if (path.startsWith(contextPath)) {
                    path = path.replaceFirst(contextPath, "");
                }
                log.debug("Getting roles for path {}", path);
                rolesForURI.put(uri, getRolesForPath(path));
            }
        }

        for (final Object o : principals.asList()) {
            log.debug("User has principal with name: {}", ((Principal) o).getName());
        }
        final Principal userPrincipal = principals.oneByType(BasicUserPrincipal.class);
        final Collection<HttpHeaderPrincipal> headerPrincipals = principals.byType(HttpHeaderPrincipal.class);
        // Add permissions for user or delegated user principal
        if (isAdmin && delegatePrincipals.size() == 1) {
            final DelegatedHeaderPrincipal delegatedPrincipal = delegatePrincipals.iterator().next();
            log.debug("Admin user is delegating to {}", delegatedPrincipal);
            addPermissions(authzInfo, rolesForURI, delegatedPrincipal.getName());
            addPermissions(authzInfo, rolesForURI, WEBAC_AUTHENTICATED_AGENT_VALUE);
        } else if (userPrincipal != null) {
            log.debug("Basic user principal username: {}", userPrincipal.getName());
            addPermissions(authzInfo, rolesForURI, userPrincipal.getName());
            addPermissions(authzInfo, rolesForURI, WEBAC_AUTHENTICATED_AGENT_VALUE);
        } else {
            log.debug("No basic user principal found");
        }
        // Add permissions for header principals
        if (headerPrincipals.isEmpty()) {
            log.debug("No header principals found!");
        }
        headerPrincipals.forEach((headerPrincipal) -> {
            addPermissions(authzInfo, rolesForURI, headerPrincipal.getName());
        });

        // Added FOAF_AGENT permissions for both authenticated and unauthenticated users
        addPermissions(authzInfo, rolesForURI, FOAF_AGENT_VALUE);

        return authzInfo;

    }

    private Map<String, Collection<String>> getRolesForPath(final String path) {
        final HttpServletRequest request = currentRequest();
        if (request == null) {
            log.warn("No HttpServletRequest available to WebACAuthorizingRealm while resolving roles for path {}",
                    path);
            return null;
        }

        final FedoraId id = identifierConverter(request).pathToInternalId(path);
        return getRolesForId(id);
    }

    private Map<String, Collection<String>> getRolesForId(final FedoraId id) {
        ensureSpringDependencies();
        if (rolesProvider == null) {
            log.error("WebACAuthorizingRealm is not wired: rolesProvider is null");
            return null;
        }
        Map<String, Collection<String>> roles = null;

        final var txId = FEDORA_ID_PREFIX + "/" + FCR_TX;
        final FedoraResource fedoraResource = getResourceOrParentFromPath(id);
        if (id.getResourceId().startsWith(txId) && fedoraResource != null) {
            roles = rolesProvider.getRoles(id, fedoraResource, transaction());
        } else if (fedoraResource != null) {
            // check ACL for the request URI and get a mapping of agent => modes
            roles = rolesProvider.getRoles(fedoraResource, transaction());
        }
        return roles;
    }

    private void addPermissions(final SimpleAuthorizationInfo authzInfo,
            final Map<URI, Map<String, Collection<String>>> rolesForURI, final String agentName) {
        if (rolesForURI != null) {
            for (final URI uri : rolesForURI.keySet()) {
                log.debug("Adding permissions gathered for URI {}", uri);
                final Map<String, Collection<String>> roles = rolesForURI.get(uri);
                if (roles != null) {
                    final Collection<String> modesForUser = roles.get(agentName);
                    if (modesForUser != null) {
                        // add WebACPermission instance for each mode in the Authorization
                        for (final String mode : modesForUser) {
                            final WebACPermission perm = new WebACPermission(URI.create(mode), uri);
                            authzInfo.addObjectPermission(perm);
                        }
                    }
                }
            }
        }
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

    private FedoraResource getResourceOrParentFromPath(final FedoraId fedoraId) {
        ensureSpringDependencies();
        if (resourceFactory == null) {
            log.error("WebACAuthorizingRealm is not wired: resourceFactory is null");
            return null;
        }
        if (containmentIndex == null) {
            log.error("WebACAuthorizingRealm is not wired: containmentIndex is null");
            return null;
        }
        try {
            log.debug("Testing FedoraResource for {}", fedoraId.getFullIdPath());
            return this.resourceFactory.getResource(transaction(), fedoraId);
        } catch (final PathNotFoundException exc) {
            log.debug("Resource {} not found getting container", fedoraId.getFullIdPath());
            final FedoraId containerId =
                    containmentIndex.getContainerIdByPath(transaction(), fedoraId, false);
            log.debug("Attempting to get FedoraResource for {}", fedoraId.getFullIdPath());
            try {
                log.debug("Got FedoraResource for {}", containerId.getFullIdPath());
                return this.resourceFactory.getResource(transaction(), containerId);
            } catch (final PathNotFoundException exc2) {
                log.debug("Path {} does not exist, but we should never end up here.", containerId.getFullIdPath());
                return null;
            }
        }
    }

}
