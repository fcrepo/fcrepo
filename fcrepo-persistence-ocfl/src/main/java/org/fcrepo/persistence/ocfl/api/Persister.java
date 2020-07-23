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
package org.fcrepo.persistence.ocfl.api;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageSession;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public interface Persister {

    /**
     * The method returns true if the operation can be persisted by this persister.
     * @param operation the operation to persist
     * @return true or false
     */
    boolean handle(ResourceOperation operation);

    /**
     * The persistence handling for the given operation.
     *
     * @param session The persistent storage session
     * @param operation The operation and associated data need to perform the operation.
     * @throws PersistentStorageException on failure
     */
    void persist(final OcflPersistentStorageSession session,
            final ResourceOperation operation)
            throws PersistentStorageException;
}
