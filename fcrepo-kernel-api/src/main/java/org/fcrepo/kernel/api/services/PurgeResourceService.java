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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Service to permanently remove a resource from the repository.
 *
 * @author whikloj
 */
public interface PurgeResourceService {
    /**
     * Purges the specified resource
     *
     * @param tx the transaction associated with the operation.
     * @param fedoraResource The Fedora resource to purge.
     * @param userPrincipal the principal of the user performing the operation.
     */
    void perform(final Transaction tx, final FedoraResource fedoraResource, final String userPrincipal);
}
