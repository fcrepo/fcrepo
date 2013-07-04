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

import static com.google.common.collect.ImmutableSet.copyOf;
import static org.fcrepo.auth.oauth.Constants.OAUTH_WORKSPACE;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.session.SessionFactory;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
public class Util {

    /**
     * Ensures that the workspace in which we store OAuth info exists.
     * 
     * @param sessionFactory
     * @throws RepositoryException
     */
    public static void
            createOauthWorkspace(final SessionFactory sessionFactory)
                throws RepositoryException {
        final Session session = sessionFactory.getSession();
        try {
            if (!copyOf(session.getWorkspace().getAccessibleWorkspaceNames())
                    .contains(OAUTH_WORKSPACE)) {
                session.getWorkspace().createWorkspace(OAUTH_WORKSPACE);
            }
        } finally {
            session.logout();
        }
    }

}
