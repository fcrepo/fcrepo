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
package org.fcrepo.auth.oauth;

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.auth.oauth.Constants.CLIENT_PROPERTY;
import static org.fcrepo.auth.oauth.Constants.OAUTH_WORKSPACE;
import static org.fcrepo.auth.oauth.Constants.PRINCIPAL_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthRuntimeException;
import org.apache.oltu.oauth2.rsfilter.OAuthDecision;
import org.apache.oltu.oauth2.rsfilter.OAuthRSProvider;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
@Component
public class DefaultOAuthResourceProvider implements OAuthRSProvider {

    @Autowired
    private SessionFactory sessionFactory;

    private static final Logger LOGGER =
            getLogger(DefaultOAuthResourceProvider.class);

    /*
     * (non-Javadoc)
     * @see
     * org.apache.oltu.oauth2.rsfilter.OAuthRSProvider#validateRequest(java.
     * lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @Override
    public OAuthDecision validateRequest(final String rsId, final String token,
            final HttpServletRequest req) throws OAuthProblemException {
        // first check validity of token
        final Session session;

        try {
            session = sessionFactory.getInternalSession(OAUTH_WORKSPACE);
        } catch (final RepositoryException e) {
            throw propagate(e);
        }

        try {
            if (!session.itemExists(getTokenPath(token))) {
                throw new OAuthRuntimeException("Invalid token!");
            }
            final Node tokenNode = session.getNode(getTokenPath(token));
            LOGGER.debug("Retrieved token from: {}", tokenNode.getPath());

            final String client =
                tokenNode.getProperty(CLIENT_PROPERTY).getString();
            LOGGER.debug("Retrieved client: {}", client);

            final String principal;
            if (tokenNode.hasProperty(PRINCIPAL_PROPERTY)) {
                principal = tokenNode.getProperty(PRINCIPAL_PROPERTY).getString();
            } else {
                principal = null;
            }
            LOGGER.debug("Retrieved principal: {}", principal);
            return new Decision(client, principal);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException("Exception validating OAuth request", e);
        } finally {
            session.logout();
        }


    }

    private static String getTokenPath(final String token) {
        return "/tokens/" + token;
    }

}
