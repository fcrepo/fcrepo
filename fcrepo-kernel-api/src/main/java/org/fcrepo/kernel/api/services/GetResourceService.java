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
package org.fcrepo.kernel.api.services;

import java.time.Instant;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * A service to check for or get a FedoraResource from storage.
 * @author whikloj
 * @since 6.0.0
 */
public interface GetResourceService {

    /**
     * Retrieve a resource from persistence if it exists.
     * @param transaction The current transaction or null if read-only.
     * @param fedoraId The internal identifier.
     * @param version The version datetime or null for head.
     * @return The resource or null if it doesn't exist.
     */
    public FedoraResource getResource(final Transaction transaction, final String fedoraId, final Instant version);

    /**
     * Check if a resource exists.
     * @param transaction The current transaction or null if read-only.
     * @param fedoraId The internal identifier
     * @param version The version datetime or null for head.
     * @return True if the identifier resolves to a resource.
     */
    public boolean doesResourceExist(final Transaction transaction, final String fedoraId, final Instant version);
}
