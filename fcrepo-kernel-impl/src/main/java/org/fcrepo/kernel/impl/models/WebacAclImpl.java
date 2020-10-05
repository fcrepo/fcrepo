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
package org.fcrepo.kernel.impl.models;

import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

/**
 * Webac Acl class
 *
 * @author whikloj
 */
public class WebacAclImpl extends ContainerImpl implements WebacAcl {

    /**
     * Constructor
     * @param fedoraID the internal identifier
     * @param txId the current transactionId
     * @param pSessionManager a session manager
     * @param resourceFactory a resource factory instance.
     */
    public WebacAclImpl(final FedoraId fedoraID, final String txId,
                        final PersistentStorageSessionManager pSessionManager, final ResourceFactory resourceFactory) {
        super(fedoraID, txId, pSessionManager, resourceFactory);
    }

    @Override
    public FedoraResource getContainer() {
        final var originalId = FedoraId.create(getFedoraId().getBaseId());
        try {

            return resourceFactory.getResource(txId, originalId);
        } catch (final PathNotFoundException exc) {
            throw new PathNotFoundRuntimeException(exc.getMessage(), exc);
        }
    }

    @Override
    public boolean isOriginalResource() {
        return false;
    }

    @Override
    public boolean isAcl() {
        return true;
    }
}
