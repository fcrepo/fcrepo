/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import javax.jcr.Session;

import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.kernel.api.services.NodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for DefaultPathLockManager.
 * @author Mike Durbin
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPathLockManagerTest {

    /**
     * Miliseconds to allow (as a maximum) for running threads to complete.  The
     * current value of 1000 should be orders of magnitude more than is required.
     * Tests are written such that we'll only wait this long if there's something
     * broken in the code and the test would fail.
     */
    public static final int WAIT = 1000;

    @Mock
    private Session session;

    @Mock
    private NodeService nodeService;

    @Before
    public void defaultSetup() {
        when(nodeService.exists(any(), any())).thenReturn(true);

    }

    @Test
    public void testActivePathCleanup() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        assertEquals("There should no active paths in memory.", 0, m.activePaths.size());

        final AcquiredLock l1 = m.lockForRead("p1", session, nodeService);
        assertEquals("There should be exactly 1 path in memory.", 1, m.activePaths.size());

        final AcquiredLock l2 = m.lockForWrite("p2", session, nodeService);
        assertEquals("There should be exactly 2 paths in memory.", 2, m.activePaths.size());

        l1.release();
        assertEquals("There should be exactly 1 path in memory.", 1, m.activePaths.size());
        l2.release();

        assertEquals("There should no active paths in memory.", 0, m.activePaths.size());
    }

    @Test
    public void readsShouldNotBlock() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String path = "path1";
        m.lockForRead(path, session, nodeService);
        assertTrue("Concurrent read operations should be allowed!",
                new Actor(() -> m.lockForRead(path, session, nodeService)).canComplete());
    }

    @Test
    public void readShouldBlockWhileWriting() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String path = "path1";
        final AcquiredLock l = m.lockForWrite(path, session, nodeService);
        final Actor r = new Actor(() -> m.lockForRead(path, session, nodeService));
        assertTrue("Read should block while writing to same path!", r.isBlocked());
        l.release();
        assertTrue("Read should complete after write!", r.canComplete());
    }

    @Test
    public void writesShouldBlock() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String path = "path1";
        final AcquiredLock l = m.lockForWrite(path, session, nodeService);
        final Actor r = new Actor(() -> m.lockForWrite(path, session, nodeService));
        assertTrue("Concurrent writes to the same path should block!", r.isBlocked());
        l.release();
        assertTrue("Write should be able to complete sequentially.", r.canComplete());
    }

    @Test
    public void siblingWritesShouldNotBlock() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String p1 = "0/0";
        final String p2 = "0/1";
        m.lockForWrite(p1, session, nodeService);
        final Actor writer = new Actor(() -> m.lockForWrite(p2, session, nodeService));
        assertTrue("Sibling writes should not block!!", writer.canComplete());
    }

    @Test
    public void siblingCreatesShouldNotBlock() throws InterruptedException {
        when(nodeService.exists(any(), eq("0/0"))).thenReturn(false);
        when(nodeService.exists(any(), eq("0/1"))).thenReturn(false);
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String p1 = "0/0";
        final String p2 = "0/1";
        m.lockForWrite(p1, session, nodeService);
        final Actor writer = new Actor(() -> m.lockForWrite(p2, session, nodeService));
        assertTrue("Sibling creates should not block!!", writer.canComplete());
    }

    @Test
    public void deletePathShouldBeDisappearWhenLockIsReleased() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String p1 = "delete";
        final AcquiredLock l = m.lockForDelete(p1, session, nodeService);
        assertEquals("One delete lock should exist!", 1, m.activeDeletePaths.size());
        assertEquals(p1, m.activeDeletePaths.get(0));
        l.release();
        assertEquals("Delete lock should have been cleaned up!", 0, m.activeDeletePaths.size());
    }

    @Test
    public void deleteShouldBlockAccessToDescendents() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String p1 = "delete";
        m.lockForDelete(p1, session, nodeService);
        assertTrue("Reading a path that is being deleted should block until delete is complete!",
                new Actor(() -> m.lockForRead("delete/some/ancestor", session, nodeService)).isBlocked());

        when(nodeService.exists(any(), eq("delete/some/nonexistant/path"))).thenReturn(false);
        when(nodeService.exists(any(), eq("delete/some/nonexistant"))).thenReturn(false);
        assertTrue("Creating a node under a node being deleted should block until delete is complete!",
                new Actor(() -> m.lockForRead("delete/some/nonexistant/path", session, nodeService)).isBlocked());
    }

    @Test
    public void deleteShouldNotAffectParentOrPeers() throws InterruptedException {
        final DefaultPathLockManager m = new DefaultPathLockManager();
        final String p1 = "root/delete";
        m.lockForDelete(p1, session, nodeService);
        assertTrue("Writing to parent of node-being-deleted should not block.",
                new Actor(() -> m.lockForWrite("root", session, nodeService)).canComplete());
        assertTrue("Writing to peer of node-being-deleted should not block.",
                new Actor(() -> m.lockForWrite("root/other", session, nodeService)).canComplete());
    }

    /**
     * An interface whose single method acquires an AcquiredLock.
     */
    private interface Locker {
        public AcquiredLock acquireLock() throws InterruptedException;
    }

    /**
     * A thread that locks as if performing some action.
     */
    private class Actor extends Thread {

        private boolean interrupted;

        private Locker l;

        public Actor(final Locker l) {
            this.start();
            this.l = l;
        }

        @Override
        public void run() {
            AcquiredLock lock = null;
            try {
                lock = l.acquireLock();
            } catch (InterruptedException e) {
                interrupted = true;
            }
            if (lock != null) {
                lock.release();
            }
        }

        /**
         * Determines if the thread would/was/is blocking.  This
         * is accomplished by interrupting the thread and joining,
         * so once it's called, the thread is no longer of use
         */
        public boolean isBlocked() {
            this.interrupt();
            try {
                this.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return interrupted;
        }

        /**
         * Determines if the thread has/can complete (ie, is not blocked).
         * The current implementation joins this thread (with a timeout)
         * and verifies that it is no longer alive.
         * @return
         * @throws InterruptedException
         */
        public boolean canComplete() throws InterruptedException {
            this.join(WAIT);
            return !this.isAlive();
        }

    }

}
