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

import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.fcrepo.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UtilTest {
    
    private static final String [] EXISTING_IN_WORKSPACES =
            new String[]{"foo", "oauth"};

    private static final String [] NOT_IN_WORKSPACES =
            new String[]{"foo"};

    @Mock
    SessionFactory mockSessions;

    @Mock
    Session mockSession;

    @Mock
    Workspace mockWorkspace;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockSessions.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
    }

    @Test
    public void createsWorkspaceWhenNecessary() throws RepositoryException {
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(NOT_IN_WORKSPACES);
        Util.createOauthWorkspace(mockSessions);
        verify(mockWorkspace).createWorkspace("oauth");
    }

    @Test
    public void usesExistingWorkspace() throws RepositoryException{
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(EXISTING_IN_WORKSPACES);
        Util.createOauthWorkspace(mockSessions);
        verify(mockWorkspace, times(0)).createWorkspace("oauth");
    }
}
