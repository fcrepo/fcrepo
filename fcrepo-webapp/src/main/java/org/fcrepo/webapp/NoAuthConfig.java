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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
