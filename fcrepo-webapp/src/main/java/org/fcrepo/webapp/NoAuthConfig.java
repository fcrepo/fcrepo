/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.webapp;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.fcrepo.config.AuthPropsConfig;
import org.fcrepo.config.ConditionOnPropertyFalse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring auth config when authorization is disabled
 *
 * @author pwinckles
 */
@Configuration
@Conditional(NoAuthConfig.AuthorizationDisabled.class)
public class NoAuthConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoAuthConfig.class);

    static class AuthorizationDisabled extends ConditionOnPropertyFalse {
        AuthorizationDisabled() {
            super(AuthPropsConfig.FCREPO_AUTH_ENABLED, true);
        }
    }

    /**
     * This bean returns a no-op shiro filter when authorization is disabled.
     *
     * @return no-op shiro filter
     */
    @Bean
    public Filter shiroFilter() {
        LOGGER.info("Authorization is disabled");
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(final HttpServletRequest httpServletRequest,
                                            final HttpServletResponse httpServletResponse,
                                            final FilterChain filterChain) throws ServletException, IOException {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            }
        };
    }

}
