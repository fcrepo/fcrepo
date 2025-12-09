/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * Auth related configuration properties
 *
 * @author pwinckles
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AuthPropsConfig extends BasePropsConfig {

    public static final String FCREPO_AUTH_ENABLED = "fcrepo.auth.enabled";
    public static final String FCREPO_AUTH_PRINCIPAL_HEADER_ENABLED = "fcrepo.auth.principal.header.enabled";
    private static final String FCREPO_AUTH_PRINCIPAL_HEADER_NAME = "fcrepo.auth.principal.header.name";
    private static final String FCREPO_AUTH_PRINCIPAL_HEADER_SEPARATOR = "fcrepo.auth.principal.header.separator";
    public static final String FCREPO_AUTH_PRINCIPAL_ROLES_ENABLED = "fcrepo.auth.principal.roles.enabled";
    private static final String FCREPO_AUTH_PRINCIPAL_ROLES_LIST = "fcrepo.auth.principal.roles.list";
    public static final String FCREPO_AUTH_PRINCIPAL_DELEGATE_ENABLED = "fcrepo.auth.principal.delegate.enabled";
    private static final String FCREPO_GROUP_AGENT_BASE_URI = "fcrepo.auth.webac.groupAgent.baseUri";
    private static final String FCREPO_USER_AGENT_BASE_URI = "fcrepo.auth.webac.userAgent.baseUri";
    private static final String FCREPO_ROOT_AUTH_ACL = "fcrepo.auth.webac.authorization";

    @Value("${" + FCREPO_ROOT_AUTH_ACL + ":#{null}}")
    private Path rootAuthAclPath;

    @Value("${" + FCREPO_USER_AGENT_BASE_URI + ":#{null}}")
    private String userAgentBaseUri;
    @Value("${" + FCREPO_GROUP_AGENT_BASE_URI + ":#{null}}")
    private String groupAgentBaseUri;

    @Value("${" + FCREPO_AUTH_PRINCIPAL_DELEGATE_ENABLED + ":true}")
    private boolean authPrincipalDelegateEnabled;

    @Value("${" + FCREPO_AUTH_PRINCIPAL_HEADER_ENABLED + ":false}")
    private boolean authPrincipalHeaderEnabled;
    @Value("${" + FCREPO_AUTH_PRINCIPAL_HEADER_NAME + ":some-header}")
    private String authPrincipalHeaderName;
    @Value("${" + FCREPO_AUTH_PRINCIPAL_HEADER_SEPARATOR + ":,}")
    private String authPrincipalHeaderSeparator;

    @Value("${" + FCREPO_AUTH_PRINCIPAL_ROLES_ENABLED + ":false}")
    private boolean authPrincipalRolesEnabled;
    @Value("#{'${" + FCREPO_AUTH_PRINCIPAL_ROLES_LIST + ":tomcat-role-1,tomcat-role-2}'.split(',')}")
    private List<String> authPrincipalRolesList;

    /**
     * @return the path to the root auth acl to use instead of the default
     */
    public Path getRootAuthAclPath() {
        return rootAuthAclPath;
    }

    /**
     * @param rootAuthAclPath path to custom root auth acl
     */
    public void setRootAuthAclPath(final Path rootAuthAclPath) {
        this.rootAuthAclPath = rootAuthAclPath;
    }

    /**
     * @return the user agent base uri, if specified
     */
    public String getUserAgentBaseUri() {
        return userAgentBaseUri;
    }

    /**
     * @return the user agent base uri, if specified
     */
    public String getGroupAgentBaseUri() {
        return groupAgentBaseUri;
    }

    /**
     * @return the header name for the auth principal header provider
     */
    public String getAuthPrincipalHeaderName() {
        return authPrincipalHeaderName;
    }

    /**
     * @return the separator for the auth principal header provider
     */
    public String getAuthPrincipalHeaderSeparator() {
        return authPrincipalHeaderSeparator;
    }

    /**
     * @return the list of auth roles
     */
    public List<String> getAuthPrincipalRolesList() {
        return authPrincipalRolesList;
    }

    /**
     * @return header principal provider enabled
     */
    public boolean isAuthPrincipalHeaderEnabled() {
        return authPrincipalHeaderEnabled;
    }

    /**
     * @return roles principal provider enabled
     */
    public boolean isAuthPrincipalRolesEnabled() {
        return authPrincipalRolesEnabled;
    }

    /**
     * @return delegate principal provider enabled
     */
    public boolean isAuthPrincipalDelegateEnabled() {
        return authPrincipalDelegateEnabled;
    }

}
