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

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.auth.oauth.Constants.CLIENT_PROPERTY;
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
import org.fcrepo.session.SessionFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultOAuthResourceProvider implements OAuthRSProvider {

    @Autowired
    SessionFactory sessionFactory;

    private static final Logger LOGGER =
            getLogger(DefaultOAuthResourceProvider.class);

    @Override
    public OAuthDecision validateRequest(final String rsId, final String token,
            final HttpServletRequest req) throws OAuthProblemException {
        // first check validity of token
        try {
            final Session session = sessionFactory.getSession();
            try {
                if (!session.itemExists("/tokens/" + token)) {
                    throw new OAuthRuntimeException("Invalid token!");
                } else {
                    final Node tokenNode = session.getNode("/tokens/" + token);
                    LOGGER.debug("Retrieved token from: {}", tokenNode
                            .getPath());
                    final String client =
                            tokenNode.getProperty(CLIENT_PROPERTY).getString();
                    LOGGER.debug("Retrieved client: {}", client);
                    final String principal =
                            tokenNode.getProperty(PRINCIPAL_PROPERTY)
                                    .getString();
                    LOGGER.debug("Retrieved principal: {}", principal);
                    return new Decision(client, principal);
                }
            } finally {
                session.logout();
            }
        } catch (final RepositoryException e) {
            propagate(e);
        }

        return null;
    }
}
