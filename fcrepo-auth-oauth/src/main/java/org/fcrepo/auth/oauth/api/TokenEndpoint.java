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
package org.fcrepo.auth.oauth.api;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static org.apache.oltu.oauth2.as.response.OAuthASResponse.tokenResponse;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_GRANT_TYPE;
import static org.apache.oltu.oauth2.common.error.OAuthError.TokenResponse
    .INVALID_CLIENT;
import static org.apache.oltu.oauth2.common.error.OAuthError.TokenResponse
    .INVALID_GRANT;
import static org.apache.oltu.oauth2.common.error.OAuthError.TokenResponse
    .UNAUTHORIZED_CLIENT;
import static org.apache.oltu.oauth2.common.message.OAuthResponse.errorResponse;
import static org.apache.oltu.oauth2.common.message.types.GrantType
    .AUTHORIZATION_CODE;
import static org.fcrepo.auth.oauth.Constants.CLIENT_PROPERTY;
import static org.fcrepo.auth.oauth.Constants.OAUTH_WORKSPACE;
import static org.fcrepo.auth.oauth.Constants.PRINCIPAL_PROPERTY;
import static org.fcrepo.auth.oauth.api.Util.createOauthWorkspace;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.auth.oauth.TokenRequestValidations;
import org.fcrepo.auth.oauth.impl.DefaultTokenRequestValidations;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
@Component
@Path("/token")
public class TokenEndpoint extends AbstractResource {

    public static final String INVALID_CLIENT_DESCRIPTION =
        "Client authentication failed (e.g., unknown client, " +
        "no client authentication included, " +
        "or unsupported authentication method).";

    private static final Logger LOGGER = getLogger(TokenEndpoint.class);

    @Autowired(required = false)
    private TokenRequestValidations requestValidator;

    @PostConstruct
    void defaultValidations() {
        if (requestValidator == null) {
            requestValidator = new DefaultTokenRequestValidations(sessions);
        }
    }

    /**
     * @param request An HTTP request
     * @return A token-bearing HTTP response
     * @throws OAuthSystemException
     * @throws RepositoryException
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response getToken(@Context final HttpServletRequest request) throws
        OAuthSystemException, RepositoryException {
        LOGGER.debug("Received request for token carried on request: {}",
                request);
        OAuthTokenRequest oauthRequest = null;

        final OAuthIssuer oauthIssuerImpl =
                new OAuthIssuerImpl(new MD5Generator());

        try {
            oauthRequest = new OAuthTokenRequest(request);

            if (!requestValidator.isValidClient(oauthRequest)) {
                final OAuthResponse response =
                        OAuthASResponse.errorResponse(SC_BAD_REQUEST).setError(
                                INVALID_CLIENT).setErrorDescription(
                                INVALID_CLIENT_DESCRIPTION).buildJSONMessage();
                return status(response.getResponseStatus()).entity(
                        response.getBody()).build();
            }

            if (!requestValidator.isValidSecret(oauthRequest)) {
                final OAuthResponse response =
                        OAuthASResponse
                                .errorResponse(SC_UNAUTHORIZED)
                                .setError(UNAUTHORIZED_CLIENT)
                                .setErrorDescription(INVALID_CLIENT_DESCRIPTION)
                                .buildJSONMessage();
                return status(response.getResponseStatus()).entity(
                        response.getBody()).build();
            }

            if (oauthRequest.getParam(OAUTH_GRANT_TYPE).equals(
                    AUTHORIZATION_CODE.toString())) {
                if (!requestValidator.isValidAuthCode(oauthRequest)) {
                    final OAuthResponse response =
                            errorResponse(SC_BAD_REQUEST).setError(
                                    INVALID_GRANT).setErrorDescription(
                                    "invalid authorization code")
                                    .buildJSONMessage();
                    return status(response.getResponseStatus()).entity(
                            response.getBody()).build();
                }
            } else if (oauthRequest.getParam(OAUTH_GRANT_TYPE).equals(
                    GrantType.PASSWORD.toString())) {
                if (!requestValidator.isValidCredentials(oauthRequest)) {
                    final OAuthResponse response =
                            errorResponse(SC_BAD_REQUEST).setError(
                                    INVALID_GRANT).setErrorDescription(
                                    "invalid username or password")
                                    .buildJSONMessage();
                    return status(response.getResponseStatus()).entity(
                            response.getBody()).build();
                }
            } else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(
                    GrantType.REFRESH_TOKEN.toString())) {
                // refresh token is not supported in this implementation
                final OAuthResponse response =
                        errorResponse(SC_BAD_REQUEST).setError(INVALID_GRANT)
                                .setErrorDescription(
                                        "invalid username or password")
                                .buildJSONMessage();
                return status(response.getResponseStatus()).entity(
                        response.getBody()).build();
            }

            final String token = oauthIssuerImpl.accessToken();
            LOGGER.debug("Created token: {}", token);
            saveToken(token, oauthRequest.getClientId(), oauthRequest
                    .getUsername());
            final OAuthResponse response =
                    tokenResponse(SC_OK).setAccessToken(token).setExpiresIn(
                            "3600").buildJSONMessage();
            return status(response.getResponseStatus()).entity(
                    response.getBody()).build();

        } catch (final OAuthProblemException e) {
            final OAuthResponse res =
                    errorResponse(SC_BAD_REQUEST).error(e).buildJSONMessage();
            return status(res.getResponseStatus()).entity(res.getBody())
                    .build();
        }
    }

    /**
     * Stores a token for later use by the configured {@link OAuthRSProvider}
     * 
     * @param token
     * @param client
     * @param username
     * @throws RepositoryException
     */
    private void saveToken(final String token, final String client,
            final String username) throws RepositoryException {
        final Session session = sessions.getInternalSession(OAUTH_WORKSPACE);
        try {
            final Node tokenNode =
                    jcrTools.findOrCreateNode(session, "/tokens/" + token);
            tokenNode.setProperty(CLIENT_PROPERTY, client);
            tokenNode.setProperty(PRINCIPAL_PROPERTY, username);
            session.save();
        } finally {
            session.logout();
        }

    }

    /**
     * Create the OAuth workspace in JCR to store all our tokens
     * @throws RepositoryException
     */
    @PostConstruct
    public void init() throws RepositoryException {
        createOauthWorkspace(sessions);
    }

}
