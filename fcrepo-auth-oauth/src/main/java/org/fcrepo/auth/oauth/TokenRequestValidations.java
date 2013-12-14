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

import javax.jcr.RepositoryException;

import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;


public interface TokenRequestValidations {
    /**
     * Checks the validity of the auth code attached to the given request
     * @param oauthRequest
     * @return
     * @throws RepositoryException
     */
    boolean isValidAuthCode(final OAuthTokenRequest oauthRequest)
        throws RepositoryException;

    /**
     * Checks the validity of the client associated with the given request
     * @param oauthRequest
     * @return
     * @throws RepositoryException
     */
    boolean isValidClient(final OAuthTokenRequest oauthRequest);

    /**
     * Checks the validity of the secret given with the given request
     * @param oauthRequest
     * @return
     * @throws RepositoryException
     */
    boolean isValidSecret(final OAuthTokenRequest oauthRequest);

    /**
     * Checks the validity of the authN credentials
     * associated with the given request
     * @param oauthRequest
     * @return
     * @throws RepositoryException
     */
    boolean isValidCredentials(final OAuthTokenRequest oauthRequest);

}
