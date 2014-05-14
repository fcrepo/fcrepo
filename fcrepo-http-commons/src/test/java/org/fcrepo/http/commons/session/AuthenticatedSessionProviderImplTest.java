/**
 * Copyright 2014 DuraSpace, Inc.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.fcrepo.http.commons.session.AuthenticatedSessionProviderImpl;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.ServletCredentials;

/**
 * <p>AuthenticatedSessionProviderImplTest class.</p>
 *
 * @author awoods
 */
public class AuthenticatedSessionProviderImplTest {

    private Repository mockRepo;

    @Before
    public void setUp() {
        mockRepo = mock(Repository.class);
    }

    @Test
    public void testCredentialsProvided() throws RepositoryException {
        final ServletCredentials mockCreds = mock(ServletCredentials.class);
        final AuthenticatedSessionProviderImpl test =
            new AuthenticatedSessionProviderImpl(mockRepo, mockCreds);
        test.getAuthenticatedSession();
        verify(mockRepo).login(mockCreds);
    }

    @Test
    public void testNoCredentialsProvided() throws RepositoryException {
        final AuthenticatedSessionProviderImpl test =
            new AuthenticatedSessionProviderImpl(mockRepo, null);
        test.getAuthenticatedSession();
        verify(mockRepo).login();
    }
}
