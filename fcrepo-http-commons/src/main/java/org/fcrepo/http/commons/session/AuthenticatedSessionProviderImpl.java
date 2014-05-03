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

package org.fcrepo.http.commons.session;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Retrieve a JCR session by just passing along the HTTP
 * credentials.
 *
 * @author awoods
 * @author gregjan
 */
public class AuthenticatedSessionProviderImpl implements
        AuthenticatedSessionProvider {

    private final Repository repository;

    private final Credentials credentials;

    /**
     * Get a new session provider for the JCR repository
     *
     * @param repo
     * @param creds
     */
    public AuthenticatedSessionProviderImpl(final Repository repo,
            final Credentials creds) {
        repository = repo;
        credentials = creds;
    }

    @Override
    public Session getAuthenticatedSession() {
        try {
            return (credentials != null) ? repository.login(credentials)
                    : repository.login();
        } catch (final RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

}
