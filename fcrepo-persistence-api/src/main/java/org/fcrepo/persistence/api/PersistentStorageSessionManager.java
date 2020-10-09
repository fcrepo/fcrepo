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
package org.fcrepo.persistence.api;

/**
 * Interface to access PersistentStorageSessions.
 *
 * @author whikloj
 * @author dbernstein
 * @since 2019-09-19
 */
public interface PersistentStorageSessionManager {

    /**
     * Retrieve a PersistentStorageSession.
     *
     * @param sessionId the externally generated session ID.
     * @return the PersistentStorageSession instance.
     */
    PersistentStorageSession getSession(final String sessionId);

    /**
     * Retrieve a read-only PersistentStorageSession. Clients should expect
     * invocation on storage modifying methods to throw exception.
     *
     * @return the PersistentStorageSession instance.
     */
    PersistentStorageSession getReadOnlySession();

    /**
     * Removes the indicated session. If the session does not exist, null is returned.
     *
     * @param sessionId the id of the session to remove
     * @return the session, if it exists
     */
    PersistentStorageSession removeSession(final String sessionId);

}
