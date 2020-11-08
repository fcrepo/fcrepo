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
package org.fcrepo.kernel.api.models;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Utility class interface for helper methods.
 * @author whikloj
 * @since 6.0.0
 */
public interface ResourceHelper {

    /**
     * Check if a resource exists.
     * @param transaction The current transaction
     * @param fedoraId The internal identifier
     * @param includeDeleted Whether to check for deleted resources too.
     * @return True if the identifier resolves to a resource.
     */
    public boolean doesResourceExist(final Transaction transaction, final FedoraId fedoraId,
                                     final boolean includeDeleted);

    /**
     * Is the resource a "ghost node". Ghost nodes are defined as a resource that does not exist, but whose URI is part
     * of the URI of another resource? For example:
     *
     * http://localhost/rest/a/b - does exist
     * http://localhost/rest/a - does not exist and is therefore a ghost node.
     *
     * @param transaction The transaction
     * @param resourceId Identifier of the resource
     * @return Whether the resource does not exist, but has
     */
    public boolean isGhostNode(final Transaction transaction, final FedoraId resourceId);
}
