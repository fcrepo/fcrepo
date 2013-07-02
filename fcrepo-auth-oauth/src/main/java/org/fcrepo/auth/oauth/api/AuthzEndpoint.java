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

import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static javax.ws.rs.core.Response.status;
import static org.apache.oltu.oauth2.as.response.OAuthASResponse.authorizationResponse;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_REDIRECT_URI;
import static org.apache.oltu.oauth2.common.OAuth.OAUTH_RESPONSE_TYPE;
import static org.apache.oltu.oauth2.common.message.types.ResponseType.CODE;
import static org.apache.oltu.oauth2.common.message.types.ResponseType.TOKEN;
import static org.apache.oltu.oauth2.common.utils.OAuthUtils.isEmpty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse.OAuthAuthorizationResponseBuilder;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.fcrepo.AbstractResource;
import org.fcrepo.auth.oauth.Constants;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.apache.oltu.oauth2.common.message.OAuthResponse.errorResponse;
import static org.fcrepo.auth.oauth.Constants.CLIENT_PROPERTY;
import static org.fcrepo.auth.oauth.Constants.EXPIRATION_TIMEOUT;
import static org.fcrepo.auth.oauth.Constants.OAUTH_WORKSPACE;
import static org.fcrepo.auth.oauth.api.Util.createOauthWorkspace;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
@Component
@Path("/authorization")
public class AuthzEndpoint extends AbstractResource {

    private static final Logger LOGGER = getLogger(AuthzEndpoint.class);

    /**
     * @param request An HTTP request
     * @return An authorization code for later use with the
     *         {@link TokenEndpoint}
     * @throws URISyntaxException
     * @throws OAuthSystemException
     * @throws RepositoryException
     */
    @GET
    public Response getAuthorization(@Context
    final HttpServletRequest request) throws URISyntaxException,
        OAuthSystemException, RepositoryException {

        OAuthAuthzRequest oauthRequest = null;

        final OAuthIssuerImpl oauthIssuerImpl =
                new OAuthIssuerImpl(new MD5Generator());

        try {
            oauthRequest = new OAuthAuthzRequest(request);

            // build response according to response_type
            final String responseType =
                    oauthRequest.getParam(OAUTH_RESPONSE_TYPE);

            final OAuthAuthorizationResponseBuilder builder =
                    authorizationResponse(request, SC_FOUND);

            if (responseType.equals(CODE.toString())) {
                final String authCode = oauthIssuerImpl.authorizationCode();
                LOGGER.debug("Created authorization code: {}", authCode);
                final String client = oauthRequest.getClientId();
                final Set<String> scopes = oauthRequest.getScopes();
                saveAuthCode(authCode, scopes, client);
                builder.setCode(authCode);
            }
            if (responseType.equals(TOKEN.toString())) {
                builder.setAccessToken(oauthIssuerImpl.accessToken());
                builder.setExpiresIn(EXPIRATION_TIMEOUT);
            }

            final String redirectURI =
                    oauthRequest.getParam(OAUTH_REDIRECT_URI);

            final OAuthResponse response =
                    builder.location(redirectURI).buildQueryMessage();
            final URI url = new URI(response.getLocationUri());

            return status(response.getResponseStatus()).location(url).build();

        } catch (final OAuthProblemException e) {

            final Response.ResponseBuilder responseBuilder = status(SC_FOUND);

            final String redirectUri = e.getRedirectUri();

            if (isEmpty(redirectUri)) {
                throw new WebApplicationException(responseBuilder.entity(
                        "OAuth callback url needs to be provided by client!")
                        .build());
            }
            final OAuthResponse response =
                    errorResponse(SC_FOUND).error(e).location(redirectUri)
                            .buildQueryMessage();
            final URI location = new URI(response.getLocationUri());
            return responseBuilder.location(location).build();
        }
    }

    /**
     * Saves an authorization code for later retrieval at the token endpoint.
     * 
     * @param authCode
     * @param scopes
     * @param client
     * @throws RepositoryException
     */
    private void saveAuthCode(final String authCode, final Set<String> scopes,
            final String client) throws RepositoryException {
        final Session session = sessions.getSession(OAUTH_WORKSPACE);
        try {
            final Node codeNode =
                    jcrTools.findOrCreateNode(session, "/authorization-codes/" +
                            authCode);
            codeNode.setProperty(CLIENT_PROPERTY, client);
            codeNode.setProperty(Constants.SCOPES_PROPERTY, scopes
                    .toArray(new String[0]));
            session.save();
        } finally {
            session.logout();
        }

    }

    /**
     * Ensures the existence of the workspace into which authorization codes are
     * stored.
     * 
     * @throws RepositoryException
     */
    @PostConstruct
    public void init() throws RepositoryException {
        createOauthWorkspace(sessions);
    }

}
