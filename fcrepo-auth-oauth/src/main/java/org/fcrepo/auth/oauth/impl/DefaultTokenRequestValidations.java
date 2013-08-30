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
package org.fcrepo.auth.oauth.impl;

import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static org.fcrepo.auth.oauth.Constants.CLIENT_PROPERTY;
import static org.fcrepo.auth.oauth.Constants.OAUTH_WORKSPACE;
import static org.fcrepo.auth.oauth.Constants.SCOPES_PROPERTY;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.map;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.value2string;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.AccessControlException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.fcrepo.auth.oauth.TokenRequestValidations;
import org.fcrepo.http.commons.session.SessionFactory;
import org.slf4j.Logger;


public class DefaultTokenRequestValidations implements TokenRequestValidations {

    private static final Logger LOGGER =
        getLogger(DefaultTokenRequestValidations.class);

    private SessionFactory sessions;

    /**
     * Instantiate the default audits and validations for an OAuth
     * token request, using information stored in a jcr workspace
     * accessible via the given session factory.
     * @param sessions
     */
    public DefaultTokenRequestValidations(SessionFactory sessions) {
        this.sessions = sessions;
    }

    /**
     * @param oauthRequest
     * @return An answer to whether or not this request matches up with an
     *         authorization code issued at the {@link AuthzEndpoint}
     * @throws RepositoryException
     */
    @Override
    public boolean isValidAuthCode(OAuthTokenRequest oauthRequest)
        throws RepositoryException {
        final String client = oauthRequest.getClientId();
        LOGGER.debug("Request has authorization client: {}", client);
        final String code = oauthRequest.getCode();
        if (code == null) {
            return false;
        }
        final Set<String> scopes = oauthRequest.getScopes();
        final Session session = sessions.getInternalSession(OAUTH_WORKSPACE);
        try {
            final Node authCodeNode =
                    session.getNode("/authorization-codes/" + code);
            LOGGER.debug("Found authorization code node stored: {}",
                    authCodeNode.getPath());
            // if the client is right
            if (authCodeNode.getProperty(CLIENT_PROPERTY).getString().equals(
                    client)) {
                if (authCodeNode.getProperty(SCOPES_PROPERTY) != null) {

                    final Set<String> storedScopes =
                        newHashSet(
                            map(authCodeNode.getProperty(SCOPES_PROPERTY)
                                .getValues(),
                                value2string));
                    // and if there is at least one scope in common
                    return (storedScopes.size() == 0 || intersection(storedScopes, scopes).size() > 0);
                } else {
                    return true;
                }
            }
        } catch (final PathNotFoundException e) {
            // this wasn't a code we stored
            return false;
        } finally {
            session.logout();
        }
        throw new AccessControlException(
                "Could not establish validity or invalidity of" +
                 " authorization code! Code:" + code);
    }

    @Override
    public boolean isValidClient(final OAuthTokenRequest oauthRequest) {
        // TODO actually do some checking of client ID and secret and so forth
        return oauthRequest != null;
    }

    @Override
    public boolean isValidSecret(final OAuthTokenRequest oauthRequest) {
        // TODO actually do some checking of client ID and secret and so forth
        return oauthRequest != null;
    }

    @Override
    public boolean isValidCredentials(final OAuthTokenRequest oauthRequest) {
        // TODO actually do some credentials?
        return oauthRequest != null;
    }

}
