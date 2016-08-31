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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;

/**
 * A class that serves as a pool of locks to guarantee synchronized
 * access based on the String representation of paths.  Because there
 * may be an extremely high number of paths in the repository, the main
 * function of this class is to ensure that for a given path only one
 * semaphore exists, and that that semaphore exists only as long as a
 * caller wishes to access that path.
 *
 * The correct usage pattern is to invoke getSemaphoreForPath (a blocking
 * method), perform some mutations to the resource at that path, then
 * invoke release on that semaphore.
 *
 * @author md5wz
 */
public class PathLockManager {

    private static final Logger LOGGER = getLogger(PathLockManager.class);

    private Map<String, TransientSemaphore> pool = new HashMap<String, TransientSemaphore>();

    /**
     * A specialized construct for the purpose of allowing mutex controls (blocking)
     * on arbitrary keys that only exist in memory as long as there is activity against
     * that particular key.  The creation of these objects should be handled entirely
     * by the method getSemaphoreForPath().
     *
     * @author md5wz
     */
    public class TransientSemaphore {

        private String key;

        private Semaphore wrappedSemaphore;

        private int count;

        /**
         * Default constructor.
         * @param key the locking key
         */
        public TransientSemaphore(final String key) {
            this.wrappedSemaphore = new Semaphore(1);
            this.key = key;
            LOGGER.trace("Created a new lock for {} (Thread {})", key, Thread.currentThread().getId());
        }

        /**
         * Releases this semaphore.  Also, if there is no one waiting on
         * it, also removes it from the pool so that it may be garbage collected
         * (when the caller no longer retains a reference to it).
         */
        public void release() {
            synchronized (pool) {
                count --;
                if (count == 0) {
                    LOGGER.trace("Removed {} from the lock pool. (Thread {})", key, Thread.currentThread().getId());
                    pool.remove(key);
                }
                wrappedSemaphore.release();
                LOGGER.trace("Released lock {} (Thread {})", key, Thread.currentThread().getId());
            }
        }

    }

    /**
     * Gets a semaphore and acquires the lock for the given path.
     * This method blocks until it can acquire the lock for that
     * path, or until interrupted.
     * @param path the path we wish to mutate
     * @return an acquired semaphore
     * @throws InterruptedException if the thread is interrupted while
     *         waiting to acquire the semaphore
     */
    public TransientSemaphore getSemaphoreForPath(final String path) throws InterruptedException {
        TransientSemaphore lock = null;
        synchronized (pool) {
            if (pool.containsKey(path)) {
                lock = pool.get(path);
                lock.count ++;
            } else {
                lock = new TransientSemaphore(path);
                lock.count ++;
                pool.put(path, lock);
            }
        }
        try {
            LOGGER.trace("Waiting to acquire lock {} (Thread {})", path, Thread.currentThread().getId());
            lock.wrappedSemaphore.acquire();
            LOGGER.trace("Acquired lock {} (Thread {})", path, Thread.currentThread().getId());
            return lock;
        } catch (InterruptedException e) {
            LOGGER.trace("Interrupted while acquiring lock {} (Thread {})", path, Thread.currentThread().getId());
            synchronized (pool) {
                lock.count --;
                if (lock.count == 0) {
                    pool.remove(path);
                }
            }
            throw e;
        }
    }
}
