/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.auth.oauth.filter;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_CLIENT_ID;
import static org.apache.oltu.oauth2.common.OAuth.HeaderType.WWW_AUTHENTICATE;
import static org.apache.oltu.oauth2.common.error.OAuthError.CodeResponse.INVALID_REQUEST;
import static org.apache.oltu.oauth2.common.error.OAuthError.ResourceResponse.INSUFFICIENT_SCOPE;
import static org.apache.oltu.oauth2.common.message.OAuthResponse.errorResponse;
import static org.apache.oltu.oauth2.rsfilter.OAuthUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rsfilter.OAuthDecision;
import org.apache.oltu.oauth2.rsfilter.OAuthRSProvider;
import org.slf4j.Logger;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
public class OAuthFilter implements Filter {

    private static final Logger LOGGER = getLogger(OAuthFilter.class);

    private String realm;

    private OAuthRSProvider provider;

    private Set<ParameterStyle> parameterStyles;

    @PostConstruct
    public void init() {
        LOGGER.debug("Initializing {}", getClass().getName());

    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        init();
    }

    @Override
    public void doFilter(ServletRequest request,
            final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        LOGGER.debug("Filtering {}", ((HttpServletRequest) request)
                .getRequestURI());
        try {

            // Make an OAuth Request out of this servlet request
            final OAuthAccessResourceRequest oauthRequest =
                    new OAuthAccessResourceRequest(req, parameterStyles
                            .toArray(new ParameterStyle[0]));

            // Get the access token
            final String accessToken = oauthRequest.getAccessToken();

            LOGGER.debug("Validating {}", accessToken);
            final OAuthDecision decision =
                    provider.validateRequest(realm, accessToken, req);

            final Principal principal = decision.getPrincipal();

            request =
                    new HttpServletRequestWrapper((HttpServletRequest) request) {

                        @Override
                        public String getRemoteUser() {
                            return principal != null ? principal.getName()
                                    : null;
                        }

                        @Override
                        public Principal getUserPrincipal() {
                            return principal;
                        }

                    };

            request.setAttribute(OAUTH_CLIENT_ID, decision.getOAuthClient()
                    .getClientId());

            chain.doFilter(request, response);
            return;

        } catch (final OAuthSystemException e1) {
            throw new ServletException(e1);
        } catch (final OAuthProblemException e) {
            respondWithError(res, e);
            return;
        }

    }

    @Override
    public void destroy() {

    }

    /**
     * Constructs an OAuth-supported HTTP error response.
     * 
     * @param resp
     * @param error
     * @throws IOException
     * @throws ServletException
     */
    private void respondWithError(final HttpServletResponse resp,
            final OAuthProblemException error) throws IOException,
        ServletException {

        OAuthResponse oauthResponse = null;

        try {
            if (isEmpty(error.getError())) {
                oauthResponse =
                        errorResponse(SC_UNAUTHORIZED).setRealm(realm)
                                .buildHeaderMessage();

            } else {

                int responseCode = SC_UNAUTHORIZED;
                if (error.getError().equals(INVALID_REQUEST)) {
                    responseCode = SC_BAD_REQUEST;
                } else if (error.getError().equals(INSUFFICIENT_SCOPE)) {
                    responseCode = SC_FORBIDDEN;
                }

                oauthResponse =
                        errorResponse(responseCode).setRealm(realm).setError(
                                error.getError()).setErrorDescription(
                                error.getDescription()).setErrorUri(
                                error.getUri()).buildHeaderMessage();
            }
            resp.addHeader(WWW_AUTHENTICATE, oauthResponse
                    .getHeader(WWW_AUTHENTICATE));
            resp.sendError(oauthResponse.getResponseStatus());
        } catch (final OAuthSystemException e) {
            throw new ServletException(e);
        }
    }

    /**
     * @param realm
     */
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    /**
     * @param provider
     */
    public void setProvider(final OAuthRSProvider provider) {
        this.provider = provider;
    }

    /**
     * @param parameterStylesSet
     */
    public void
            setParameterStyles(final Set<ParameterStyle> parameterStylesSet) {
        this.parameterStyles = parameterStylesSet;
    }

}
