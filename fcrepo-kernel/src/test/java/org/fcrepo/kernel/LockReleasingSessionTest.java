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

package org.fcrepo.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Mike Durbin
 */
public class LockReleasingSessionTest {

    private static final String LOCK_TOKEN = "t1";

    private static final String TX_ID = "TX1";

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private LockManager mockLockManager;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockSession.isLive()).thenReturn(true);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getLockManager()).thenReturn(mockLockManager);
        when(mockLockManager.getLockTokens()).thenReturn(new String[]{LOCK_TOKEN});
    }

    @Test
    public void testDelegationOfOtherMethods() throws RepositoryException {
        final Session proxySession = LockReleasingSession.newInstance(mockSession);
        proxySession.save();
        verify(mockSession, never()).isLive();
        verify(mockSession).save();
    }

    @Test
    public void testDelegationOfLogoutMethod() throws RepositoryException {
        final Session proxySession = LockReleasingSession.newInstance(mockSession);
        // logout should be intercepted and lock tokens should be removed
        proxySession.logout();
        verify(mockSession).isLive();
        Assert.assertTrue(mockSession.isLive());
        verify(mockLockManager).getLockTokens();
        verify(mockLockManager).removeLockToken(LOCK_TOKEN);
        verify(mockSession).logout();


    }

    @Test
    public void testTxSession() {
        final Session txSession = TxAwareSession.newInstance(mockSession, TX_ID);
        final Session proxySession = LockReleasingSession.newInstance(txSession);

        proxySession.logout();
        // verify that logout was NOT called on the mock session
        verify(mockSession, never()).logout();
    }
}
