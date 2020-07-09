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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

/**
 * Tombstone class
 */
public class TombstoneImpl extends FedoraResourceImpl implements Tombstone {

    private FedoraResource originalResource;

    protected TombstoneImpl(final FedoraId fedoraID, final Transaction tx,
                            final PersistentStorageSessionManager pSessionManager,
                            final ResourceFactory resourceFactory, final FedoraResource original) {
        super(fedoraID, tx, pSessionManager, resourceFactory);
        this.originalResource = original;
    }

    @Override
    public FedoraResource getDeletedObject() {
        return originalResource;
    }

    @Override
    public FedoraId getFedoraId() {
        return this.originalResource.getFedoraId();
    }
}
