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

package org.fcrepo.kernel.api.lock;

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Responsible for managing write locks on Fedora resources
 *
 * @author pwinckles
 */
public interface ResourceLockManager {

    /**
     * Acquires a lock on the resource, associating it to the txId. If the lock is held by a different transaction,
     * an exception is thrown. If the lock is already held by the same transaction, then it returns successfully.
     *
     * @param txId the transaction id to associate the lock to
     * @param resourceId the resource to lock
     * @throws ConcurrentUpdateException when lock cannot be acquired
     */
    void acquire(final String txId, final FedoraId resourceId);

    /**
     * Releases all of the locks held by the transaction
     *
     * @param txId the transaction id
     */
    void releaseAll(final String txId);

}
