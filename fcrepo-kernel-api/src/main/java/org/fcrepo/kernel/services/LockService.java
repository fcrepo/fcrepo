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

package org.fcrepo.kernel.services;

import org.fcrepo.kernel.Lock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author Mike Durbin
 */
public interface LockService extends Service {

    /**
     * Acquires a lock on the node at the given path.
     * @param session the session wishing to lock the node/subgraph
     * @param path the path to be affected
     * @param deep if true, indicates that the entire subgraph starting
     *             at the given path is to be locked
     * @return a Lock object that contains a lock token that will allow
     *         other sessions to access resources locked by this call.
     */
    Lock acquireLock(Session session, String path, long timeout, boolean deep) throws RepositoryException;

    /**
     * Gets the lock at the given path.  The returned lock will only
     * contain identifying information for sessions holding the lock.
     */
    Lock getLock(Session session, String path) throws RepositoryException;

    /**
     * Releases the lock on the node at the given path.  This may only be
     * successful if the session holding the lock.
     */
    void releaseLock(Session session, String path) throws RepositoryException;
}
