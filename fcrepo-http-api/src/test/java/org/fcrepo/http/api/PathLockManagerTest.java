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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.fcrepo.http.api.PathLockManager.TransientSemaphore;
import org.junit.Test;

/**
 * Unit tests for PathLockManager.
 * @author md5wz
 */
public class PathLockManagerTest {

    @Test
    public void testExclusive() throws InterruptedException {
        final PathLockManager m = new PathLockManager();
        final String path = "path1";
        final TransientSemaphore l = m.getSemaphoreForPath(path);
        final Actor a = new Actor(path, m);
        final Thread t = new Thread(a);
        t.start();
        Thread.sleep(100);
        assertTrue("Thread should still be blocked!", t.isAlive());
        l.release();
        t.join(2000);
        assertFalse("Thread should be done!", t.isAlive());
        assertFalse("Thread should not have been interrupted!", a.wasInterrupted());
    }

    @Test
    public void testInterrupted() throws InterruptedException {
        final PathLockManager m = new PathLockManager();
        final String path = "path1";
        m.getSemaphoreForPath(path);
        final Actor a = new Actor(path, m);
        final Thread t = new Thread(a);
        t.start();
        Thread.sleep(100);
        assertTrue("Thread should still be blocked!", t.isAlive());
        t.interrupt();
        t.join(2000);
        assertFalse("Thread should be done!", t.isAlive());
        assertTrue("Thread should have been interrupted!", a.wasInterrupted());
    }

    @Test
    public void testNonConflicting() throws InterruptedException {
        final PathLockManager m = new PathLockManager();
        final String path = "path1";
        final String otherPath = "path2";

        m.getSemaphoreForPath(path);
        final Actor a = new Actor(otherPath, m);
        final Thread t = new Thread(a);
        t.start();
        t.join(2000);
        assertFalse("Thread should be done!", t.isAlive());
        assertFalse("Thread should not have been interrupted!", a.wasInterrupted());
    }

    private static final class Actor implements Runnable {

        private String path;

        private PathLockManager m;

        private boolean interrupted = false;

        public Actor(final String path, final PathLockManager m) {
            this.path = path;
            this.m = m;
        }

        @Override
        public void run() {
            try {
                m.getSemaphoreForPath(path).release();
            } catch (InterruptedException e) {
                interrupted = true;
                return;
            }
        }

        public boolean wasInterrupted() {
            return interrupted;
        }

    }


}
