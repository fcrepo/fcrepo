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

package org.fcrepo.session;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.modeshape.jcr.api.ServletCredentials;

/**
 * Retrieve a JCR session by just passing along the HTTP
 * credentials.
 */
public class AuthenticatedSessionProviderImpl implements
        AuthenticatedSessionProvider {

    private final Repository repository;

    private final ServletCredentials credentials;

    /**
     * Get a new session provider for the JCR repository
     * 
     * @param repo
     * @param creds
     */
    public AuthenticatedSessionProviderImpl(final Repository repo,
            final ServletCredentials creds) {
        repository = repo;
        credentials = creds;
    }

    @Override
    public Session getAuthenticatedSession() {
        try {
            return (credentials != null) ? repository.login(credentials)
                    : repository.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

}
