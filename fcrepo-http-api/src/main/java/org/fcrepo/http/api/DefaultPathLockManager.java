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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InterruptedRuntimeException;
import org.fcrepo.kernel.api.services.NodeService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;

/**
 * A class that serves as a pool lockable paths to guarantee synchronized
 * accesses to the resources at those paths.  Because there may be an
 * extremely high number of paths in the repository, this implementation
 * is complicated by a need to ensure that for a given path, only one set
 * of locks exists and only until all the locks have been acquired and released.
 *
 * Because this is very complex code, extensive logging is produced at
 * the TRACE level.
 *
 * @author Mike Durbin
 */

@Component
public class DefaultPathLockManager implements PathLockManager {

    private static final Logger LOGGER = getLogger(DefaultPathLockManager.class);

    /**
     * A map of all the paths for which requests to lock are underway.  This
     * is an exhaustive set of all paths that may be accessed at this time.
     * Changes to the contents of this map must be synchronized on the instance
     * of this class, though blocking acquisition of locks against those paths
     * must NOT be, lest we degrade to an essentially single-threaded
     * application.
     */
    @VisibleForTesting
    Map<String, ActivePath> activePaths = new HashMap<>();

    /**
     * A list of paths for which delete operations are underway.  Attempts to
     * acquire locks on resources that would be deleted with those paths will
     * block until the delete lock is released.  This is a handy shortcut that
     * allows this class to meet the locking requirements for delete operations
     * without actually discovering (and locking) all the descendant nodes.
     */
    @VisibleForTesting
    List<String> activeDeletePaths = new ArrayList<>();

    /**
     * A class that represents a path that can be locked for reading or writing
     * and is the subject of a currently active lock request (though locks may
     * not necessarily have been granted yet).
     */
    private class ActivePath {

        private String path;

        private ReadWriteLock rwLock;

        /**
         * A list with references to every thread that has requested
         * (though not necessary acquired) a read or write lock through
         * the mechanism of the outer class' contract and not yet
         * relinquished it by invoking LockManager.release().  This
         * list is actually managed by methods outside of ActivePath.
         */
        private List<Thread> threads;

        private ActivePath(final String path) {
            this.path = path;
            rwLock = new ReentrantReadWriteLock();
            threads = new ArrayList<>();
        }

        public PathScopedLock getReadLock() {
            threads.add(Thread.currentThread());
            LOGGER.trace("Thread {} requesting read lock on {}.", Thread.currentThread().getId(), path);
            return new PathScopedLock(rwLock.readLock());
        }

        public PathScopedLock getWriteLock() {
            threads.add(Thread.currentThread());
            LOGGER.trace("Thread {} requesting write lock on {}.", Thread.currentThread().getId(), path);
            return new PathScopedLock(rwLock.writeLock());
        }

        /**
         * Wraps a lock (read or write) to expose a subset of its
         * functionality and to add special handling for our "delete"
         * locks.
         */
        private class PathScopedLock {

            final private Lock lock;

            public PathScopedLock(final Lock l) {
                lock = l;
            }

            public ActivePath getPath() {
                return ActivePath.this;
            }

            public boolean tryLock() {
                for (final String deletePath : activeDeletePaths) {
                    if (isOrIsDescendantOf(path, deletePath)) {
                        LOGGER.trace("Thread {} could not be granted lock on {} because that path is being deleted.",
                                Thread.currentThread().getId(), path);
                        return false;
                    }
                }
                return lock.tryLock();
            }

            public void unlock() {
                lock.unlock();
            }
        }
    }

    /**
     * The AcquiredLock implementation that's returned by the surrounding class.
     * Two main constructors exist, one that accepts a bunch of locks, all of which
     * must be acquired and a second representing a "delete" lock, for which at
     * acquisition time the locks necessary are determined from the current pool of
     * active locks.
     *
     * Never, outside of a block of code synchronized with the surrounding class
     * instance, does this class hold an incomplete subset of the locks required:
     * in other words, it gets all of the locks or none of them, never blocking
     * while holding locks.
     */
    private class AcquiredMultiPathLock implements AcquiredLock {

        private String deletePath;

        private List<ActivePath.PathScopedLock> locks;

        /**
         * Instantiates and initializes an AcquiredMultiPathLock.  This
         * constructor blocks until all of the necessary locks have been
         * acquired, but to avoid possible deadlocks releases all acquired
         * locks when it fails to acquire even one of them.
         * @param locks each PathLock that must be acquired
         * @throws InterruptedException
         */
        private AcquiredMultiPathLock(final List<ActivePath.PathScopedLock> locks) throws InterruptedException {
            this.locks = locks;

            boolean success = false;
            while (!success) {
                synchronized (DefaultPathLockManager.this) {
                    success = tryAcquireAll();
                    if (!success) {
                        LOGGER.debug("Failed to acquire all necessary path locks: waiting.  (Thread {})",
                                Thread.currentThread().getId());
                        DefaultPathLockManager.this.wait();
                    }
                }

            }
            LOGGER.debug("Acquired all necessary path locks  (Thread {})", Thread.currentThread().getId());

        }

        /**
         * Instantiates and initializes an AcquiredMultiPathLock that requires
         * locks on the given path and all active paths that are descendants of
         * the given path.  This constructor blocks until all of the necessary
         * locks have been acquired, but to avoid possible deadlocks releases
         * all acquired locks when it fails to acquire even one of them.
         * @param deletePath the path for which all descendant paths must also be
         *        write locked.
         * @throws InterruptedException
         */
        private AcquiredMultiPathLock(final String deletePath) throws InterruptedException {
            this.deletePath = deletePath;

            boolean success = false;
            while (!success) {
                synchronized (DefaultPathLockManager.this) {
                    this.locks = new ArrayList<>();

                    // find all paths to lock
                    activePaths.forEach((path, lock) -> {
                        if (isOrIsDescendantOf(path, deletePath)) {
                            locks.add(lock.getWriteLock());
                        }
                        });

                    success = tryAcquireAll();
                    if (!success) {
                        LOGGER.debug("Failed to acquire all necessary path locks: waiting.  (Thread {})",
                                Thread.currentThread().getId());
                        DefaultPathLockManager.this.wait();
                    } else {
                        // So, we have acquired locks on every currently active path that is
                        // the target of the DELETE operation or its ancestor... but what if
                        // one is added once we fall out of this synchronized block?
                        //
                        // ...well, in that case we set a special note that this path is being
                        // deleted so that whenever a new lock is attempted on a to-be-deleted
                        // path, those locks fail to acquire.
                        LOGGER.trace("Thread {} acquired delete lock on path {}.",
                                Thread.currentThread().getId(), deletePath);
                        activeDeletePaths.add(deletePath);
                    }
                }

            }
            LOGGER.debug("Acquired all necessary path locks  (Thread {})", Thread.currentThread().getId());

        }

        private boolean tryAcquireAll() {
            final List<ActivePath.PathScopedLock> acquired = new ArrayList<>();
            for (final ActivePath.PathScopedLock lock : locks) {
                if (lock.tryLock()) {
                    acquired.add(lock);
                } else {
                    // roll back
                    acquired.forEach(ActivePath.PathScopedLock::unlock);
                    return false;
                }
            }
            return true;
        }

        /*
         * This is the only method that removes paths from the pool
         * of currently active paths that can be locked.
         */
        @Override
        public void release() {
            synchronized (DefaultPathLockManager.this) {
                for (final ActivePath.PathScopedLock lock : locks) {
                    lock.unlock();
                    lock.getPath().threads.remove(Thread.currentThread());
                    if (lock.getPath().threads.isEmpty()) {
                        activePaths.remove(lock.getPath().path);
                    }
                }
                if (deletePath != null) {
                    LOGGER.trace("Thread {} releasing delete lock on path {}.",
                            Thread.currentThread().getId(), deletePath);
                    activeDeletePaths.remove(deletePath);
                }
                LOGGER.trace("Thread {} released locks.", Thread.currentThread().getId());
                DefaultPathLockManager.this.notify();
            }
        }

    }

    /*
     * This is the only method that adds paths to the pool of
     * currently active paths that can be locked.
     */
    private synchronized ActivePath getActivePath(final String path) {
        ActivePath activePath = activePaths.get(path);
        if (activePath == null) {
            activePath = new ActivePath(path);
            activePaths.put(path, activePath);
        }
        return activePath;
    }

    private boolean isOrIsDescendantOf(final String possibleDescendant, final String path) {
        return path.equals(possibleDescendant) || possibleDescendant.startsWith(path + "/");
    }

    private String getParentPath(final String path) {
        if (path.indexOf('/') == -1) {
            return null;
        }
        return path.substring(0, path.lastIndexOf('/'));
    }

    @VisibleForTesting
    static String normalizePath(final String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    @Override
    public AcquiredLock lockForRead(final String path) {
        final List<ActivePath.PathScopedLock> locks = new ArrayList<>();

        synchronized (this) {
            locks.add(getActivePath(normalizePath(path)).getReadLock());
        }

        try {
            return new AcquiredMultiPathLock(locks);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
    }

    @Override
    public AcquiredLock lockForWrite(final String path, final FedoraSession session, final NodeService nodeService) {
        final List<ActivePath.PathScopedLock> locks = new ArrayList<>();

        synchronized (this) {
            // lock the specified path while iterating through the path's
            // ancestry to also lock each path that would be created implicitly
            // by this write (ie, non-existent ancestral paths)
            final String startingPath = normalizePath(path);
            for (String currentPath = startingPath ;
                    currentPath == null || currentPath.length() > 0;
                    currentPath = getParentPath(currentPath)) {
                if (currentPath == null || (currentPath != startingPath && nodeService.exists(session, currentPath))) {
                    // either we've followed the path back to the root, or we've found an ancestor that exists...
                    // so there are no more locks to create.
                    break;
                }
                locks.add(getActivePath(currentPath).getWriteLock());
            }
        }

        try {
            return new AcquiredMultiPathLock(locks);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
    }

    @Override
    public AcquiredLock lockForDelete(final String path) {
        try {
            return new AcquiredMultiPathLock(normalizePath(path));
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
    }
}
