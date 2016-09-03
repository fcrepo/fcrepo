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

import javax.jcr.Session;

import org.fcrepo.kernel.api.services.NodeService;

/**
 * An interface representing a utility whose locking methods can enforce
 * the kinds of concurrency restrictions appropriate for an otherwise
 * concurrency-naive application that operates on hierarchical resources
 * represented by paths (as in URIs or filesystems).
 *
 * @author Mike Durbin
 */
public interface PathLockManager {

    /**
     * An interface representing a lock (comparable to that defined in
     * java.util.concurrent.locks) that has been acquired.  Memory management
     * for instances of this lock is handled by LockManager instances and
     * anyone else receiving one of these such locks from a LockManager
     * should not retain a reference to the used lock after releasing it.
     */
    public interface AcquiredLock {

        /**
         * Releases the lock, after which it's useless.
         */
        public void release();
    }

    /**
     * Locks the necessary resources affected in order to safely view a resource
     * at the given path.  A successful return from this method should guarantee
     * that until release() is invoked on the returned Lock that no changes are made
     * to the resource that would affect its display.
     *
     * @param path the path to a resource to be viewed
     * @param session the current session
     * @param nodeService the repository NodeService implementation
     * @return an acquired Lock on the relevant resources
     * @throws InterruptedException if interrupted while waiting to acquire the lock
     */
    public AcquiredLock lockForRead(String path, Session session, NodeService nodeService) throws InterruptedException;

    /**
     * Locks the necessary resources affected in order to safely write to a resource
     * at the given path.  A successful return from this method should guarantee
     * that until release() is invoked on the returned Lock that no other callers
     * may be granted locks necessary to add, modify or delete the resource at the
     * provided path.
     *
     * @param path the path to a resource to be created (may involve implicitly created
     *        resources at parent paths)
     * @param session the current session
     * @param nodeService the repository NodeService implementation
     * @return an acquired Lock on the relevant resources
     * @throws InterruptedException if interrupted while waiting to acquire the lock
     */
    public AcquiredLock lockForWrite(String path, Session session, NodeService nodeService) throws InterruptedException;

    /**
     * Locks the necessary resources affected in order to safely delete a resource
     * at the given path.  A successful return from this method should guarantee
     * that until release() is invoked on the returned Lock that no other callers
     * may be granted locks necessary to add, modify or delete the resource at the
     * provided path or any child path (because deletes cascade).
     *
     * @param path the path to a resource to be deleted (may imply the deletion of
     *        all descendant resources)
     * @param session the current session
     * @param nodeService the repository NodeService implementation
     * @return an acquired Lock on the relevant resources
     * @throws InterruptedException if interrupted while waiting to acquire the lock
     */
    public AcquiredLock lockForDelete(String path, Session session, NodeService nodeService)
            throws InterruptedException;

}
