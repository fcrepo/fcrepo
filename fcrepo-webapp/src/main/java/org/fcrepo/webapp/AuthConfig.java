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

package org.fcrepo.webapp;

import java.util.HashSet;
import java.util.List;

import javax.servlet.Filter;

import org.fcrepo.auth.common.ContainerRolesPrincipalProvider;
import org.fcrepo.auth.common.DelegateHeaderPrincipalProvider;
import org.fcrepo.auth.common.HttpHeaderPrincipalProvider;
import org.fcrepo.auth.common.PrincipalProvider;
import org.fcrepo.auth.common.ServletContainerAuthFilter;
import org.fcrepo.auth.common.ServletContainerAuthenticatingRealm;
import org.fcrepo.auth.webac.WebACAuthorizingRealm;
import org.fcrepo.auth.webac.WebACFilter;
import org.fcrepo.config.AuthPropsConfig;
import org.fcrepo.config.ConditionOnPropertyBoolean;

import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Spring config for auth
 *
 * @author pwinckles
 */
@Configuration
@Conditional(AuthConfig.AuthEnabled.class)
public class AuthConfig {

    static class AuthEnabled extends ConditionOnPropertyBoolean {
        AuthEnabled() {
            super(AuthPropsConfig.FCREPO_AUTH_ENABLED, true);
        }
    }
    static class HeaderPrincipalEnabled extends ConditionOnPropertyBoolean {
        HeaderPrincipalEnabled() {
            super(AuthPropsConfig.FCREPO_AUTH_PRINCIPAL_HEADER_ENABLED, false);
        }
    }
    static class RolesPrincipalEnabled extends ConditionOnPropertyBoolean {
        RolesPrincipalEnabled() {
            super(AuthPropsConfig.FCREPO_AUTH_PRINCIPAL_ROLES_ENABLED, false);
        }
    }
    static class DelegatePrincipalEnabled extends ConditionOnPropertyBoolean {
        DelegatePrincipalEnabled() {
            super(AuthPropsConfig.FCREPO_AUTH_PRINCIPAL_DELEGATE_ENABLED, true);
        }
    }

    /**
     * Optional PrincipalProvider filter that will inspect the request header, "some-header", for user role values
     *
     * @param propsConfig config properties
     * @return header principal provider
     */
    @Bean
    @Order(1)
    @Conditional(AuthConfig.HeaderPrincipalEnabled.class)
    public PrincipalProvider headerProvider(final AuthPropsConfig propsConfig) {
        final var provider = new HttpHeaderPrincipalProvider();
        provider.setHeaderName(propsConfig.getAuthPrincipalHeaderName());
        provider.setSeparator(propsConfig.getAuthPrincipalHeaderSeparator());
        return provider;
    }

    /**
     * Optional PrincipalProvider filter that will use container configured roles as principals
     *
     * @param propsConfig config properties
     * @return roles principal provider
     */
    @Bean
    @Order(2)
    @Conditional(AuthConfig.RolesPrincipalEnabled.class)
    public PrincipalProvider containerRolesProvider(final AuthPropsConfig propsConfig) {
        final var provider = new ContainerRolesPrincipalProvider();
        provider.setRoleNames(new HashSet<>(propsConfig.getAuthPrincipalRolesList()));
        return provider;
    }

    /**
     * delegatedPrincipleProvider filter allows a single user to be passed in the header "On-Behalf-Of",
     *            this is to be used as the actor making the request when authenticating.
     *            NOTE: On users with the role fedoraAdmin can delegate to another user.
     *            NOTE: Only supported in WebAC authentication
     *
     * @return delegate principal provider
     */
    @Bean
    @Order(3)
    @Conditional(AuthConfig.DelegatePrincipalEnabled.class)
    public PrincipalProvider delegatedPrincipalProvider() {
        return new DelegateHeaderPrincipalProvider();
    }

    /**
     * Define the Shiro Realm implementation you want to use to connect to your back-end
     *
     * @return authroization realm
     */
    @Bean
    public AuthorizingRealm webACAuthorizingRealm() {
        return new WebACAuthorizingRealm();
    }

    /**
     * Servlet Container Authentication Realm
     *
     * @return authentication realm
     */
    @Bean
    public AuthenticatingRealm servletContainerAuthenticatingRealm() {
        return new ServletContainerAuthenticatingRealm();
    }

    /**
     * @return Security Manager
     */
    @Bean
    public WebSecurityManager securityManager() {
        final var manager = new DefaultWebSecurityManager();
        manager.setRealms(List.of(webACAuthorizingRealm(), servletContainerAuthenticatingRealm()));
        return manager;
    }

    /**
     * Post processor that automatically invokes init() and destroy() methods
     *
     * @return post processor
     */
    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    /**
     * @return Authentication Filter
     */
    @Bean
    public Filter servletContainerAuthFilter() {
        return new ServletContainerAuthFilter();
    }

    /**
     * @return Authorization Filter
     */
    @Bean
    public Filter webACFilter() {
        return new WebACFilter();
    }

    /**
     * Shiro filter. When defining the filter chain, the Auth filter should come first, followed by 0 or more of the
     * principal provider filters, and finally the webACFilter
     *
     * @param propsConfig config properties
     * @return shiro filter
     */
    @Bean
    @Order(100)
    public ShiroFilterFactoryBean shiroFilter(final AuthPropsConfig propsConfig) {
        final var filter = new ShiroFilterFactoryBean();
        filter.setSecurityManager(securityManager());
        filter.setFilterChainDefinitions("/** = servletContainerAuthFilter,"
                + principalProviderChain(propsConfig) + "webACFilter");
        return filter;
    }

    private String principalProviderChain(final AuthPropsConfig propsConfig) {
        final var builder = new StringBuilder();

        if (propsConfig.isAuthPrincipalHeaderEnabled()) {
            builder.append("headerProvider,");
        }
        if (propsConfig.isAuthPrincipalRolesEnabled()) {
            builder.append("containerRolesProvider,");
        }
        if (propsConfig.isAuthPrincipalDelegateEnabled()) {
            builder.append("delegatedPrincipalProvider,");
        }

        return builder.toString();
    }

}
