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
package org.fcrepo.kernel.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Mike Durbin
 */
public class LockServiceImplTest {

    private static final String LOCKABLE_PATH = "test1";

    private static final String ALREADY_LOCKED_PATH = "test2";

    private static final long TIMEOUT = -1;

    private static final String USER = "user";

    private static final String LOCK_TOKEN = "token";


    private LockServiceImpl testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private LockManager mockLockManager;

    @Mock
    private Lock mockLock;

    @Mock
    private Lock otherMockLock;

    @Before
    public void setUp() throws RepositoryException {
        testObj = new LockServiceImpl();
        initMocks(this);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockSession.getUserID()).thenReturn(USER);
        when(mockWorkspace.getLockManager()).thenReturn(mockLockManager);
        when(mockLockManager.getLock(LOCKABLE_PATH)).thenReturn(mockLock);
        when(mockLockManager.getLock(ALREADY_LOCKED_PATH)).thenReturn(otherMockLock);
        when(mockLockManager.lock(LOCKABLE_PATH, false, false, TIMEOUT, USER)).thenReturn(mockLock);
        when(mockLockManager.lock(ALREADY_LOCKED_PATH, false, false, TIMEOUT, USER)).thenThrow(LockException.class);
        when(mockLockManager.isLocked(ALREADY_LOCKED_PATH)).thenReturn(true);
        when(mockLock.getLockToken()).thenReturn(LOCK_TOKEN);
        when(mockLock.isDeep()).thenReturn(false);
        when(otherMockLock.isDeep()).thenReturn(false);
    }

    @Test
    public void testAcquireLock() throws RepositoryException {
        final org.fcrepo.kernel.Lock lock = testObj.acquireLock(mockSession, LOCKABLE_PATH, false);
        Assert.assertEquals(LOCK_TOKEN, lock.getLockToken());
        Assert.assertFalse(lock.isDeep());
    }

    @Test (expected = LockException.class)
    public void testAcquireLockFailure() throws RepositoryException {
        final org.fcrepo.kernel.Lock lock = testObj.acquireLock(mockSession, ALREADY_LOCKED_PATH, false);
    }

    @Test
    public void testGetOwnedLock() throws RepositoryException {
        when(mockLockManager.isLocked(LOCKABLE_PATH)).thenReturn(true);
        final org.fcrepo.kernel.Lock lock = testObj.getLock(mockSession, LOCKABLE_PATH);
        Assert.assertEquals(LOCK_TOKEN, lock.getLockToken());
        Assert.assertFalse(lock.isDeep());
    }

    @Test
    public void testReleaseOwnedLock() throws RepositoryException {
        when(mockLockManager.isLocked(LOCKABLE_PATH)).thenReturn(true);
        testObj.releaseLock(mockSession, LOCKABLE_PATH);
        verify(mockLockManager).unlock(LOCKABLE_PATH);
    }

    @Test
    public void testReleaseOtherLock() throws RepositoryException {
        testObj.releaseLock(mockSession, ALREADY_LOCKED_PATH);
        verify(mockLockManager).unlock(ALREADY_LOCKED_PATH);
    }

}
